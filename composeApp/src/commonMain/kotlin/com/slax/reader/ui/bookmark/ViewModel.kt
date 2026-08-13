package com.slax.reader.ui.bookmark

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.slax.reader.SlaxConfig
import com.powersync.PowerSyncDatabase
import com.slax.reader.data.database.dao.BookmarkCommentDao
import com.slax.reader.data.database.dao.BookmarkDao
import com.slax.reader.data.database.dao.CollectionDao
import com.slax.reader.data.database.dao.LocalBookmarkDao
import com.slax.reader.data.database.dao.SubscriptionDao
import com.slax.reader.data.database.dao.UserDao
import com.slax.reader.data.database.model.CollectionMarkPolicy
import com.slax.reader.data.database.model.canCreateMark
import com.slax.reader.data.database.model.checkIsSubscribed
import com.slax.reader.data.database.model.collectionMarkPolicy
import com.slax.reader.data.network.ApiService
import com.slax.reader.data.preferences.AppPreferences
import com.slax.reader.data.preferences.ContinueReadingBookmark
import com.slax.reader.domain.image.ShareImageSelector
import com.slax.reader.domain.sync.BackgroundDomain
import com.slax.reader.domain.sync.CollectionBackgroundDomain
import com.slax.reader.ui.bookmark.states.BookmarkDelegate
import com.slax.reader.ui.bookmark.states.BookmarkDetailBinding
import com.slax.reader.ui.bookmark.states.BookmarkOverlay
import com.slax.reader.ui.bookmark.states.CommentDelegate
import com.slax.reader.ui.bookmark.states.OutlineDelegate
import com.slax.reader.ui.bookmark.states.OverlayDelegate
import com.slax.reader.ui.bookmark.states.OverviewDelegate
import com.slax.reader.ui.bookmark.states.toStableId
import com.slax.reader.utils.bookmarkEvent
import com.slax.reader.data.network.dto.MarkType
import com.slax.reader.data.network.dto.StrokeCreateData
import com.slax.reader.utils.AppWebViewState
import com.slax.reader.utils.BridgeMarkItemInfo
import com.slax.reader.utils.BridgeMarkStrokeInfo
import com.slax.reader.utils.isIOS
import com.slax.reader.utils.shareContent
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.*
import kotlinx.serialization.json.Json
import slax_reader_client.composeapp.generated.resources.Res

data class FeedbackPageParams(
    val title: String? = null,
    val href: String? = null,
    val email: String? = null,
    val bookmarkId: String? = null,
    val entryPoint: String? = null,
    val targetUrl: String? = null,
    val version: String? = null
)

sealed interface BookmarkDetailEffect {
    data object NavigateBack : BookmarkDetailEffect
    data object NavigateToSubscription : BookmarkDetailEffect
    data class NavigateToFeedback(val params: FeedbackPageParams) : BookmarkDetailEffect

    data class ScrollToAnchor(val anchor: String) : BookmarkDetailEffect

    /** 通知 UI 层调用 JS drawMarks，传入序列化后的 MarkDetail JSON */
    data class DrawMarks(val markDetailJson: String) : BookmarkDetailEffect
}

data class BookmarkContentState(
    val htmlContent: String? = null,
    val isLoading: Boolean = false,
    val cacheKey: String = "",
)

class BookmarkDetailViewModel(
    private val bookmarkDao: BookmarkDao,
    private val collectionDao: CollectionDao,
    private val subscriptionDao: SubscriptionDao,
    private val localBookmarkDao: LocalBookmarkDao,
    private val commentDao: BookmarkCommentDao,
    private val userDao: UserDao,
    private val backgroundDomain: BackgroundDomain,
    private val collectionBackgroundDomain: CollectionBackgroundDomain,
    private val apiService: ApiService,
    private val appPreferences: AppPreferences,
    private val database: PowerSyncDatabase,
    private val shareImageSelector: ShareImageSelector,
    private val applicationScope: CoroutineScope,
) : ViewModel() {

    companion object {
        private const val SAVE_DEBOUNCE_MS = 2000L

        /** 用于向 JS Bridge 序列化划线数据，必须保留所有默认值字段 */
        private val markDetailJson = Json { encodeDefaults = true }
    }

    private val _bookmarkId = MutableStateFlow<String?>(null)
    val bookmarkId = _bookmarkId.asStateFlow()
    private val _bookmarkBinding = MutableStateFlow<BookmarkDetailBinding?>(null)
    val isCollectionBookmark: StateFlow<Boolean> = _bookmarkBinding
        .map { it?.isCollection == true }
        .stateIn(viewModelScope, SharingStarted.Eagerly, false)

    private fun currentCacheKey(): String =
        _bookmarkBinding.value?.resolveCacheKey().orEmpty()

    val cacheKey: StateFlow<String> = _bookmarkBinding
        .map { binding -> binding?.resolveCacheKey().orEmpty() }
        .stateIn(viewModelScope, SharingStarted.Eagerly, "")

    private val _effects = MutableSharedFlow<BookmarkDetailEffect>(extraBufferCapacity = 8)
    val effects: SharedFlow<BookmarkDetailEffect> = _effects.asSharedFlow()

    private val _deleteConfirmVisible = MutableStateFlow(false)
    val deleteConfirmVisible: StateFlow<Boolean> = _deleteConfirmVisible.asStateFlow()

    private val _contentState = MutableStateFlow(BookmarkContentState(isLoading = false))
    val contentState = _contentState.asStateFlow()

    // 文章图片地址（由 processContent 解析得到，仅供分享挑图用，不参与 UI 渲染）
    private val articleImageUrls = MutableStateFlow<List<String>>(emptyList())

    // 阅读位置：一次性消费，PageLoaded 时读取并清空
    private var initialReadPosition: Float? = null

    private var currentPosition: Float = -1f
    private var savePositionJob: Job? = null
    private val readPositionSaves = Channel<ReadPositionSnapshot>(Channel.UNLIMITED)

    private var contentJob: Job? = null
    private var markObserveJob: Job? = null
    private var markPolicyJob: Job? = null

    private val _markPolicy = MutableStateFlow(CollectionMarkPolicy.Personal)
    val allowLineActions = _markPolicy
        .map { it.canCreateMark(MarkType.LINE) }
        .stateIn(viewModelScope, SharingStarted.Eagerly, true)
    val allowCommentActions = _markPolicy
        .map { it.canCreateMark(MarkType.COMMENT) }
        .stateIn(viewModelScope, SharingStarted.Eagerly, true)

    val userInfo = userDao.watchUserInfo()
    val subscriptionInfo = subscriptionDao.watchSubscriptionInfo()

    init {
        applicationScope.launch {
            var lastSavedReadPosition: ReadPositionSnapshot? = null
            for (snapshot in readPositionSaves) {
                if (snapshot == lastSavedReadPosition) continue
                try {
                    localBookmarkDao.updateLocalBookmarkReadPosition(snapshot.bookmarkId, snapshot.position)
                    lastSavedReadPosition = snapshot
                } catch (error: CancellationException) {
                    throw error
                } catch (error: Exception) {
                    println("Failed to save read position for ${snapshot.bookmarkId}: ${error.message}")
                }
            }
        }
    }

    val overlayDelegate = OverlayDelegate()
    val commentDelegate = CommentDelegate(database, commentDao, localBookmarkDao, userDao, apiService, viewModelScope)
    val outlineDelegate = OutlineDelegate(localBookmarkDao, apiService, viewModelScope)
    val overviewDelegate = OverviewDelegate(localBookmarkDao, apiService, viewModelScope)
    val bookmarkDelegate = BookmarkDelegate(
        bookmarkDao,
        collectionDao,
        _bookmarkBinding,
        viewModelScope,
    )

    fun bind(bookmarkId: String, collectionOwnerId: String? = null, collectionId: String? = null) {
        val binding = BookmarkDetailBinding(bookmarkId, collectionOwnerId, collectionId)
        if (_bookmarkBinding.value == binding) return

        flushReadPosition()

        _bookmarkBinding.value = binding
        _bookmarkId.value = bookmarkId
        _contentState.value = BookmarkContentState(isLoading = true)

        // 重置阅读位置状态
        initialReadPosition = null
        currentPosition = -1f
        savePositionJob?.cancel()
        markObserveJob?.cancel()
        markPolicyJob?.cancel()

        overlayDelegate.reset()
        outlineDelegate.reset()
        overviewDelegate.reset()
        commentDelegate.reset()

        val initialPolicy = if (!binding.isCollection) {
            CollectionMarkPolicy.Personal
        } else {
            CollectionMarkPolicy.CollectionPending
        }
        _markPolicy.value = initialPolicy

        markPolicyJob = viewModelScope.launch(Dispatchers.IO) {
            loadSavedPosition(bookmarkId)
            commentDelegate.bind(bookmarkId, initialPolicy, binding.validCollectionOwnerId)
            if (binding.isCollection && binding.validCollectionOwnerId != null) {
                combine(
                    bookmarkDelegate.currentBookmark,
                    collectionDao.watchCollectionMarkSettings(binding.validCollectionOwnerId!!),
                ) { bookmark, collectionSettings ->
                    bookmark to collectionSettings
                }.collect { (bookmark, collectionSettings) ->
                    if (_bookmarkBinding.value != binding) return@collect
                    val policy = runCatching {
                        collectionMarkPolicy(bookmark.metadataObj?.share, collectionSettings)
                    }
                        .getOrDefault(CollectionMarkPolicy.CollectionPending)
                    _markPolicy.value = policy
                    commentDelegate.bind(bookmarkId, policy, binding.validCollectionOwnerId)
                }
            }
        }

        loadOutline()

        // 如果订阅了，Outline 按钮默认展示
        viewModelScope.launch {
            val isSubscribed = subscriptionInfo.value?.checkIsSubscribed() == true
            if (isSubscribed) {
                outlineDelegate.showCollapsed()
            }
        }

        refreshContent()
    }

    fun refreshContent() {
        val id = _bookmarkId.value ?: return
        val binding = _bookmarkBinding.value ?: return
        val collectionOwnerId = binding.validCollectionOwnerId
        if (binding.isCollection && collectionOwnerId == null) {
            contentJob?.cancel()
            _contentState.value = BookmarkContentState(isLoading = false)
            articleImageUrls.value = emptyList()
            return
        }
        // 与 ImageViewer / shareBookmark 共用同一个 key，避免三处各算一遍算出不一样的结果
        val resolvedCacheKey = currentCacheKey().ifEmpty { id }

        contentJob?.cancel()
        _contentState.value = _contentState.value.copy(isLoading = true, cacheKey = resolvedCacheKey)
        articleImageUrls.value = emptyList()

        contentJob = viewModelScope.launch {
            try {
                val content = withContext(Dispatchers.IO) {
                    if (binding.isCollection && collectionOwnerId != null) {
                        collectionBackgroundDomain.getBookmarkContent(id, collectionOwnerId)
                    } else {
                        backgroundDomain.getBookmarkContent(id)
                    }
                }
                if (_bookmarkBinding.value != binding) return@launch
                _contentState.value = BookmarkContentState(
                    htmlContent = content.html,
                    isLoading = false,
                    cacheKey = resolvedCacheKey,
                )
                articleImageUrls.value = content.imageUrls
            } catch (error: CancellationException) {
                throw error
            } catch (error: Exception) {
                println("Failed to load bookmark content $id: ${error.message}")
                if (_bookmarkBinding.value != binding) return@launch
                _contentState.value = BookmarkContentState(
                    htmlContent = null,
                    isLoading = false,
                    cacheKey = resolvedCacheKey,
                )
            }
        }
    }

    fun requestScrollToAnchor(anchor: String) {
        viewModelScope.launch {
            _effects.emit(BookmarkDetailEffect.ScrollToAnchor(anchor))
        }
        outlineDelegate.collapseDialog()
    }

    fun requestNavigateBack() {
        viewModelScope.launch { _effects.emit(BookmarkDetailEffect.NavigateBack) }
    }

    fun requestNavigateToSubscription() {
        viewModelScope.launch { _effects.emit(BookmarkDetailEffect.NavigateToSubscription) }
    }

    fun requestNavigateToFeedback() {
        viewModelScope.launch {
            val bookmarkState = bookmarkDelegate.bookmarkDetailState.value
            val currentBookmarkId = _bookmarkId.value

            val params = FeedbackPageParams(
                title = bookmarkState.displayTitle,
                href = bookmarkState.metadataUrl,
                email = userInfo.value?.email,
                bookmarkId = currentBookmarkId,
                entryPoint = "bookmark_detail",
                version = "${SlaxConfig.APP_VERSION_NAME} (${SlaxConfig.APP_VERSION_CODE})"
            )

            _effects.emit(BookmarkDetailEffect.NavigateToFeedback(params))
        }
    }

    fun requestDeleteBookmark() {
        if (isCollectionBookmark.value) return
        _deleteConfirmVisible.value = true
    }

    fun dismissDeleteConfirmation() {
        _deleteConfirmVisible.value = false
    }

    fun confirmDeleteBookmark() {
        viewModelScope.launch {
            runCatching { bookmarkDelegate.deleteBookmark() }
                .onSuccess {
                    bookmarkEvent.action("delete").send()
                    _deleteConfirmVisible.value = false
                    overlayDelegate.dismissOverlay(BookmarkOverlay.Toolbar)
                    requestNavigateBack()
                }
                .onFailure {
                    bookmarkEvent.action("delete_failed").send()
                    _deleteConfirmVisible.value = false
                }
        }
    }

    fun onToolbarIconClick(pageId: String) {
        if (isCollectionBookmark.value && pageId in setOf("star", "archive", "edit_title")) return
        val current = bookmarkDelegate.bookmarkDetailState.value

        when (pageId) {
            "star" -> bookmarkDelegate.onToggleStar(!current.isStarred)
            "archive" -> bookmarkDelegate.onToggleArchive(!current.isArchived)
            "edit_title" -> overlayDelegate.showOverlay(BookmarkOverlay.EditTitle)
            "summary" -> {
                viewModelScope.launch {
                    val isSubscribed = subscriptionInfo.value?.checkIsSubscribed() == true
                    bookmarkEvent.action("use_outline").isSubscribed(isSubscribed).send()

                    if (!isSubscribed) {
                        overlayDelegate.showOverlay(BookmarkOverlay.SubscriptionRequired)
                        overlayDelegate.dismissOverlay(BookmarkOverlay.Toolbar)
                        return@launch
                    }
                    outlineDelegate.showDialog()
                }
            }
            "feedback" -> overlayDelegate.showOverlay(BookmarkOverlay.FeedbackRequired)
            "share" -> shareBookmark()
        }

        overlayDelegate.dismissOverlay(BookmarkOverlay.Toolbar)
    }

    fun shareBookmark() {
        val id = _bookmarkId.value ?: return
        viewModelScope.launch(Dispatchers.IO) {
            val state = bookmarkDelegate.bookmarkDetailState.value
            val url = "${SlaxConfig.WEB_BASE_URL}/b/$id"
            val text = buildString {
                if (state.displayTitle.isNotBlank()) appendLine(state.displayTitle)
                append(url)
            }

            val articleImage = runCatching {
                // 挑图要落到与预缓存一致的目录，否则会在 bookmark/<裸id>/images 下留一份无人回收的副本
                shareImageSelector.pick(articleImageUrls.value, currentCacheKey().ifEmpty { id })
            }.getOrNull()
            val imageBytes = articleImage
                ?: runCatching { Res.readBytes("files/share_logo.png") }.getOrNull()

            withContext(Dispatchers.Main) {
                shareContent(title = state.displayTitle, text = text, url = url, imageBytes = imageBytes)
            }
            bookmarkEvent.action("share").send()
        }
    }

    fun onStopRecordContinue() {
        viewModelScope.launch { runCatching { recordContinueBookmark() } }
    }

    fun onResumeClearContinue() {
        viewModelScope.launch { runCatching { clearContinueBookmark() } }
    }

    private suspend fun recordContinueBookmark() = withContext(Dispatchers.IO) {
        _bookmarkId.value?.let { id ->
            val state = bookmarkDelegate.bookmarkDetailState.value
            if (state.displayTitle.isEmpty()) return@withContext
            appPreferences.setContinueReadingBookmark(
                ContinueReadingBookmark(
                    bookmarkId = id,
                    title = state.displayTitle,
                    collectionOwnerId = _bookmarkBinding.value?.collectionOwnerId,
                    collectionId = _bookmarkBinding.value?.collectionId,
                )
            )
        }
    }

    private suspend fun clearContinueBookmark() = withContext(Dispatchers.IO) {
        return@withContext appPreferences.clearContinueReadingBookmark()
    }

    fun loadOverview() {
        val id = _bookmarkId.value ?: return
        overviewDelegate.loadOverview(id)
    }

    fun loadOutline() {
        val id = _bookmarkId.value ?: return
        outlineDelegate.loadOutline(id)
    }

    fun startObservingMarks() {
        markObserveJob?.cancel()
        markObserveJob = viewModelScope.launch(Dispatchers.IO) {
            commentDelegate.markDetailFlow.collect { markDetail ->
                val json = markDetailJson.encodeToString(markDetail)
                _effects.emit(BookmarkDetailEffect.DrawMarks(json))
            }
        }
    }

    fun strokeHighlight(webViewState: AppWebViewState, onComplete: (() -> Unit)? = null) {
        if (!_markPolicy.value.canCreateMark(MarkType.LINE)) {
            onComplete?.invoke()
            return
        }
        webViewState.evaluateJsWithCallback(
            "window.SlaxWebViewBridge.captureCurrentSelection()"
        ) { resultJson ->
            if (resultJson.isNullOrBlank() || resultJson == "null") return@evaluateJsWithCallback
            viewModelScope.launch(Dispatchers.IO) {
                runCatching {
                    val data = StrokeCreateData.fromJsResult(resultJson)
                    val localId = commentDelegate.addMark(
                        type = MarkType.LINE,
                        source = data.toMarkPath(),
                        approxSource = data.approx_source,
                        selectContent = data.select_content,
                    )
                    withContext(Dispatchers.Main) { onComplete?.invoke() }
                }.onFailure {
                    println("[划线] 创建失败: ${it.message}")
                    withContext(Dispatchers.Main) { onComplete?.invoke() }
                }
            }
        }
    }

    fun captureSelectionForComment(
        webViewState: AppWebViewState,
        onCaptured: (text: String, markInfo: BridgeMarkItemInfo) -> Unit,
    ) {
        if (!_markPolicy.value.canCreateMark(MarkType.COMMENT)) return
        webViewState.evaluateJsWithCallback(
            "window.SlaxWebViewBridge.captureCurrentSelection()"
        ) { resultJson ->
            if (resultJson.isNullOrBlank() || resultJson == "null") return@evaluateJsWithCallback
            runCatching {
                val data = StrokeCreateData.fromJsResult(resultJson)
                val markInfo = BridgeMarkItemInfo(
                    source = data.toMarkPath(),
                    approx = data.approx_source,
                )
                val text = data.text ?: data.approx_source?.raw_text
                    ?: data.approx_source?.exact
                    ?: ""
                if (text.isNotBlank()) {
                    onCaptured(text, markInfo)
                }
            }.onFailure { println("[评论] 获取选区数据失败: ${it.message}") }
        }
    }

    fun addStrokeToMark(
        markItemInfo: BridgeMarkItemInfo,
        onComplete: () -> Unit,
    ) {
        if (!_markPolicy.value.canCreateMark(MarkType.LINE)) {
            onComplete()
            return
        }
        viewModelScope.launch(Dispatchers.IO) {
            runCatching {
                val localId = commentDelegate.addMark(
                    type = MarkType.LINE,
                    source = markItemInfo.source,
                    approxSource = markItemInfo.approx,
                    selectContent = markItemInfo.toSelectContent(),
                )

                withContext(Dispatchers.Main) { onComplete() }
            }.onFailure {
                println("[划线] 添加到已有 mark 失败: ${it.message}")
                withContext(Dispatchers.Main) { onComplete() }
            }
        }
    }

    fun removeStrokeFromMark(
        markItemInfo: BridgeMarkItemInfo,
        onComplete: () -> Unit,
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            runCatching {
                val uid = commentDelegate.currentUserId ?: ""
                val sourceJson = markDetailJson.encodeToString(markItemInfo.source)

                val recordId = commentDelegate.findCommentId { po ->
                    po.type == MarkType.LINE.value && po.is_deleted == 0 && po.metadataObj?.user_id == uid && po.source == sourceJson
                }

                if (recordId != null) commentDelegate.deleteComment(recordId)
                withContext(Dispatchers.Main) { onComplete() }
            }.onFailure {
                println("[划线] 删除失败: ${it.message}")
                withContext(Dispatchers.Main) { onComplete() }
            }
        }
    }

    fun submitComment(
        markItemInfo: BridgeMarkItemInfo,
        comment: String,
        replyMarkId: Long? = null,
        onComplete: (() -> Unit)? = null,
    ) {
        if (!_markPolicy.value.canCreateMark(MarkType.COMMENT)) {
            onComplete?.invoke()
            return
        }
        viewModelScope.launch(Dispatchers.IO) {
            runCatching {
                if (replyMarkId != null) {
                    val parentPO = commentDelegate.findComment { po ->
                        po.id.toStableId() == replyMarkId
                    } ?: return@launch
                    val rootId = parentPO.metadataObj?.root_id ?: parentPO.id

                    commentDelegate.addMark(
                        type = MarkType.REPLY,
                        source = markItemInfo.source,
                        approxSource = markItemInfo.approx,
                        selectContent = markItemInfo.toSelectContent(),
                        comment = comment,
                        rootId = rootId,
                        parentId = parentPO.id,
                    )
                } else {
                    commentDelegate.addMark(
                        type = MarkType.COMMENT,
                        source = markItemInfo.source,
                        approxSource = markItemInfo.approx,
                        selectContent = markItemInfo.toSelectContent(),
                        comment = comment,
                    )
                }
                withContext(Dispatchers.Main) { onComplete?.invoke() }
            }.onFailure {
                println("[评论] 提交失败: ${it.message}")
                withContext(Dispatchers.Main) { onComplete?.invoke() }
            }
        }
    }

    fun deleteComment(markId: Long) {
        viewModelScope.launch(Dispatchers.IO) {
            runCatching {
                val recordId = commentDelegate.findCommentId { po ->
                    po.id.toStableId() == markId
                } ?: return@launch
                commentDelegate.deleteComment(recordId)
            }.onFailure {
                println("[评论] 删除失败: ${it.message}")
            }
        }
    }

    fun canDeleteComment(markId: Long): Boolean {
        val recordId = commentDelegate.findCommentId { po -> po.id.toStableId() == markId } ?: return false
        return commentDelegate.canDeleteComment(recordId)
    }

    // 加载书签的保存阅读位置
    private suspend fun loadSavedPosition(bookmarkId: String) {
        if (_bookmarkId.value != bookmarkId) return
        initialReadPosition = localBookmarkDao.getLocalBookmarkReadPosition(bookmarkId)
    }

    // 一次性消费初始阅读位置，PageLoaded 时调用
    fun consumeInitialReadPosition(): Float? {
        val pos = initialReadPosition
        initialReadPosition = null
        return pos?.takeIf { it > 0f }
    }

    fun saveReadPosition(scrollY: Float) {
        currentPosition = scrollY
        savePositionJob?.cancel()
        savePositionJob = viewModelScope.launch {
            delay(SAVE_DEBOUNCE_MS)
            val id = _bookmarkId.value ?: return@launch
            enqueueReadPositionSave(ReadPositionSnapshot(id, currentPosition))
        }
    }

    fun flushReadPosition() {
        savePositionJob?.cancel()
        savePositionJob = null
        val id = _bookmarkId.value ?: return
        val position = currentPosition
        if (position < 0f) return
        enqueueReadPositionSave(ReadPositionSnapshot(id, position))
    }

    private fun enqueueReadPositionSave(snapshot: ReadPositionSnapshot) {
        if (!readPositionSaves.trySend(snapshot).isSuccess) {
            println("Failed to enqueue read position for ${snapshot.bookmarkId}")
        }
    }

    fun flushOutlineScrollPosition() {
        outlineDelegate.flushScrollPosition()
    }

    override fun onCleared() {
        super.onCleared()

        flushReadPosition()
        readPositionSaves.close()

        contentJob?.cancel()
        contentJob = null
        markObserveJob?.cancel()
        markObserveJob = null
        commentDelegate.reset()
        outlineDelegate.reset()
        overviewDelegate.reset()
        overlayDelegate.reset()
    }
}

private data class ReadPositionSnapshot(
    val bookmarkId: String,
    val position: Float,
)
