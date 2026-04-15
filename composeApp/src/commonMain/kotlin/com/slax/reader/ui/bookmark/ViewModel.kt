package com.slax.reader.ui.bookmark

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.slax.reader.SlaxConfig
import com.powersync.PowerSyncDatabase
import com.slax.reader.data.database.dao.BookmarkCommentDao
import com.slax.reader.data.database.dao.BookmarkDao
import com.slax.reader.data.database.dao.LocalBookmarkDao
import com.slax.reader.data.database.dao.SubscriptionDao
import com.slax.reader.data.database.dao.UserDao
import com.slax.reader.data.database.model.checkIsSubscribed
import com.slax.reader.data.network.ApiService
import com.slax.reader.data.preferences.AppPreferences
import com.slax.reader.data.preferences.ContinueReadingBookmark
import com.slax.reader.domain.sync.BackgroundDomain
import com.slax.reader.ui.bookmark.states.BookmarkDelegate
import com.slax.reader.ui.bookmark.states.BookmarkOverlay
import com.slax.reader.ui.bookmark.states.CommentDelegate
import com.slax.reader.ui.bookmark.states.OutlineDelegate
import com.slax.reader.ui.bookmark.states.OverlayDelegate
import com.slax.reader.ui.bookmark.states.OverviewDelegate
import com.slax.reader.utils.bookmarkEvent
import com.slax.reader.data.network.dto.AddMarkParams
import com.slax.reader.data.network.dto.MarkPathItem
import com.slax.reader.data.network.dto.MarkType
import com.slax.reader.data.network.dto.StrokeCreateData
import com.slax.reader.utils.AppWebViewState
import com.slax.reader.utils.BridgeMarkCommentInfo
import com.slax.reader.utils.BridgeMarkItemInfo
import com.slax.reader.utils.BridgeMarkStrokeInfo
import com.slax.reader.utils.escapeJsTemplateString
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.serialization.json.Json

data class FeedbackPageParams(
    val title: String? = null,
    val href: String? = null,
    val email: String? = null,
    val bookmarkId: String? = null,
    val entryPoint: String? = null,
    val targetUrl: String? = null,
    val version: String? = null
)

fun FeedbackPageParams.toMap() : Map<String, String> {
    return buildMap {
        title?.let { put("title", it) }
        href?.let { put("href", it) }
        email?.let { put("email", it) }
        bookmarkId?.let { put("bookmarkId", it) }
        entryPoint?.let { put("entryPoint", it) }
        targetUrl?.let { put("targetUrl", it) }
        version?.let { put("version", it)}
        bookmarkId?.let { put("bookmarkId", it) }
    }
}

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
)

class BookmarkDetailViewModel(
    private val bookmarkDao: BookmarkDao,
    private val subscriptionDao: SubscriptionDao,
    private val localBookmarkDao: LocalBookmarkDao,
    private val commentDao: BookmarkCommentDao,
    private val userDao: UserDao,
    private val backgroundDomain: BackgroundDomain,
    private val apiService: ApiService,
    private val appPreferences: AppPreferences,
    private val database: PowerSyncDatabase,
) : ViewModel() {

    companion object {
        private const val SAVE_DEBOUNCE_MS = 2000L

        /** 用于向 JS Bridge 序列化划线数据，必须保留所有默认值字段 */
        private val markDetailJson = Json { encodeDefaults = true }
    }

    private val _bookmarkId = MutableStateFlow<String?>(null)
    val bookmarkId = _bookmarkId.asStateFlow()

    private val _effects = MutableSharedFlow<BookmarkDetailEffect>(extraBufferCapacity = 8)
    val effects: SharedFlow<BookmarkDetailEffect> = _effects.asSharedFlow()

    private val _deleteConfirmVisible = MutableStateFlow(false)
    val deleteConfirmVisible: StateFlow<Boolean> = _deleteConfirmVisible.asStateFlow()

    private val _contentState = MutableStateFlow(BookmarkContentState(isLoading = false))
    val contentState = _contentState.asStateFlow()

    // 阅读位置：一次性消费，PageLoaded 时读取并清空
    private var initialReadPosition: Float? = null

    private var currentPosition: Float = -1f
    private var savePositionJob: Job? = null

    private var contentJob: Job? = null

    /** 当前登录用户的 userId，用于 JS 侧判断划线归属 */
    suspend fun getCurrentUserId(): String? {
        return appPreferences.getAuthInfo().firstOrNull()?.userId
    }

    val userInfo = userDao.watchUserInfo()
    val subscriptionInfo = subscriptionDao.watchSubscriptionInfo()

    val overlayDelegate = OverlayDelegate()
    val commentDelegate = CommentDelegate(database, commentDao, viewModelScope)
    val outlineDelegate = OutlineDelegate(localBookmarkDao, apiService, viewModelScope)
    val overviewDelegate = OverviewDelegate(localBookmarkDao, apiService, viewModelScope)
    val bookmarkDelegate = BookmarkDelegate(bookmarkDao, _bookmarkId, viewModelScope)

    fun bind(bookmarkId: String) {
        if (_bookmarkId.value == bookmarkId) return

        _bookmarkId.value = bookmarkId
        _contentState.value = BookmarkContentState(isLoading = true)

        // 重置阅读位置状态
        initialReadPosition = null
        currentPosition = -1f
        savePositionJob?.cancel()

        overlayDelegate.reset()
        outlineDelegate.reset()
        overviewDelegate.reset()
        commentDelegate.reset()

        // 异步加载保存的阅读位置，不阻塞主流程
        viewModelScope.launch(Dispatchers.IO) {
            loadSavedPosition(bookmarkId)
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

        contentJob?.cancel()
        _contentState.value = _contentState.value.copy(isLoading = true)

        contentJob = viewModelScope.launch {
            runCatching {
                withContext(Dispatchers.IO) { backgroundDomain.getBookmarkContent(id) }
            }.onSuccess { content ->
                _contentState.value = BookmarkContentState(htmlContent = content, isLoading = false)
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
        }

        overlayDelegate.dismissOverlay(BookmarkOverlay.Toolbar)
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
                    title = state.displayTitle
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

    /**
     * 拉取当前书签的划线列表并通过 [BookmarkDetailEffect.DrawMarks] 派发给 UI 层。
     * 在 WebView 页面加载完成（PageLoaded）后调用，确保 JS Bridge 已就绪。
     * 失败时静默忽略，划线为非核心功能，不影响阅读体验。
     */
    fun loadAndDrawMarks() {
        val id = _bookmarkId.value ?: return
        viewModelScope.launch(Dispatchers.IO) {
            runCatching { apiService.getBookmarkMarkList(id) }
                .onSuccess { httpData ->
                    httpData.data?.let { markDetail ->
                        val json = markDetailJson.encodeToString(markDetail)
                        _effects.emit(BookmarkDetailEffect.DrawMarks(json))
                    }
                }
        }
    }

    /**
     * 执行划线操作的完整链路：
     * 1. 调用 JS Bridge strokeCurrentSelection 获取选区数据并在 WebView 中即时渲染
     * 2. 将选区数据转换后调用后端 /v1/mark/create 创建划线记录
     * 3. 用后端返回的 mark_id 通过 JS Bridge updateMarkIdByUuid 回补到本地
     *
     * @param webViewState 用于执行 JS Bridge 调用
     */
    fun strokeHighlight(webViewState: AppWebViewState) {
        val bookmarkIdStr = _bookmarkId.value ?: return

        // 调用 JS Bridge 获取选区数据（同时在前端渲染划线）
        webViewState.evaluateJsWithCallback(
            "window.SlaxWebViewBridge.strokeCurrentSelection()"
        ) { resultJson ->
            // JS 返回 null 或空字符串表示没有有效选区
            if (resultJson.isNullOrBlank() || resultJson == "null") return@evaluateJsWithCallback

            viewModelScope.launch(Dispatchers.IO) {
                runCatching {
                    // 解析 JS Bridge 返回的 StrokeCreateData
                    // WebView 回调会对字符串值再包一层引号，需先解码外层字符串
                    val actualJson = if (resultJson.startsWith("\"")) {
                        Json.decodeFromString<String>(resultJson)
                    } else {
                        resultJson
                    }
                    val strokeData = Json.decodeFromString<StrokeCreateData>(actualJson)

                    // 将 StrokeCreateSource 转换为 MarkPathItem（xpath → path 字段映射）
                    val markSource = strokeData.source.map { src ->
                        when (src.type) {
                            "image" -> MarkPathItem.Image(path = src.xpath)
                            else -> MarkPathItem.Text(
                                path = src.xpath,
                                start = src.start_offset,
                                end = src.end_offset
                            )
                        }
                    }

                    // 将 StrokeCreateSelectContent 提取为纯文本列表
                    val selectContent = strokeData.select_content.map { item ->
                        when (item.type) {
                            "image" -> item.src
                            else -> item.text
                        }
                    }

                    val params = AddMarkParams(
                        bookmark_uid = bookmarkIdStr,
                        type = MarkType.LINE,
                        source = markSource,
                        select_content = selectContent,
                        approx_source = strokeData.approx_source,
                    )

                    // 调用后端创建划线
                    val result = apiService.addBookmarkMark(params)
                    val markId = result.data?.mark_id ?: return@launch

                    // 用后端返回的 mark_id 回补到 JS Bridge 的本地数据
                    webViewState.evaluateJs(
                        "window.SlaxWebViewBridge.updateMarkIdByUuid('${strokeData.uuid}', $markId)"
                    )
                }.onFailure { error ->
                    println("[划线] 创建失败: ${error.message}")
                }
            }
        }
    }

    /**
     * 为已有 mark（通过 CommentPanelSheet 点击已有标记进入）添加划线。
     *
     * 与 [strokeHighlight] 不同的是，这里无需调用 strokeCurrentSelection 获取选区，
     * 因为 markItemInfo 已包含完整的 source/approx 数据。流程为：
     * 1. 调用 JS Bridge addStrokeByUuid 在前端渲染划线样式
     * 2. 调用后端 /v1/mark/create 创建划线记录
     * 3. 用后端返回的 mark_id 通过 JS Bridge updateMarkIdByUuid 回补到本地
     *
     * @param webViewState 用于执行 JS Bridge 调用
     * @param markItemInfo 当前选中的 mark 信息
     * @param onComplete 操作完成回调，参数为更新后的 markItemInfo（新增了当前用户的 stroke）
     */
    fun addStrokeToMark(
        webViewState: AppWebViewState,
        markItemInfo: BridgeMarkItemInfo,
        onComplete: (BridgeMarkItemInfo) -> Unit,
    ) {
        val bookmarkIdStr = _bookmarkId.value ?: return
        val isTemporary = markItemInfo.id.isEmpty()

        if (isTemporary) {
            addStrokeToTemporaryMark(webViewState, markItemInfo, bookmarkIdStr, onComplete)
        } else {
            addStrokeToExistingMark(webViewState, markItemInfo, bookmarkIdStr, onComplete)
        }
    }

    /**
     * 已有 mark 的划线路径：addStrokeByUuid → API → updateMarkIdByUuid
     */
    private fun addStrokeToExistingMark(
        webViewState: AppWebViewState,
        markItemInfo: BridgeMarkItemInfo,
        bookmarkIdStr: String,
        onComplete: (BridgeMarkItemInfo) -> Unit,
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            runCatching {
                val currentUserId = getCurrentUserId()?.toLongOrNull() ?: 0L

                // 1. 调用 JS Bridge 在前端渲染划线样式
                webViewState.evaluateJs(
                    "window.SlaxWebViewBridge.addStrokeByUuid('${markItemInfo.id}', $currentUserId)"
                )

                // 2. 调用后端创建划线
                val selectContent = markItemInfo.approx?.raw_text?.let { listOf(it) }
                    ?: listOf(markItemInfo.approx?.exact ?: "")

                val params = AddMarkParams(
                    bookmark_uid = bookmarkIdStr,
                    type = MarkType.LINE,
                    source = markItemInfo.source,
                    select_content = selectContent,
                    approx_source = markItemInfo.approx,
                )

                val result = apiService.addBookmarkMark(params)
                val markId = result.data?.mark_id ?: return@launch

                // 3. 回补 mark_id
                webViewState.evaluateJs(
                    "window.SlaxWebViewBridge.updateMarkIdByUuid('${markItemInfo.id}', $markId)"
                )

                // 4. 更新 markItemInfo 并回调
                val updatedInfo = markItemInfo.copy(
                    stroke = markItemInfo.stroke + BridgeMarkStrokeInfo(
                        markId = markId,
                        userId = currentUserId,
                    )
                )
                withContext(Dispatchers.Main) {
                    onComplete(updatedInfo)
                }
            }.onFailure { error ->
                println("[划线] 添加到已有 mark 失败: ${error.message}")
                withContext(Dispatchers.Main) {
                    onComplete(markItemInfo)
                }
            }
        }
    }

    /**
     * 临时选区的划线路径：addStrokeBySource → API → updateMarkIdByUuid
     */
    private fun addStrokeToTemporaryMark(
        webViewState: AppWebViewState,
        markItemInfo: BridgeMarkItemInfo,
        bookmarkIdStr: String,
        onComplete: (BridgeMarkItemInfo) -> Unit,
    ) {
        val sourceJson = escapeJsTemplateString(
            markDetailJson.encodeToString(markItemInfo.source)
        )
        val approxJson = markItemInfo.approx?.let {
            escapeJsTemplateString(markDetailJson.encodeToString(it))
        }

        viewModelScope.launch(Dispatchers.IO) {
            runCatching {
                val currentUserId = getCurrentUserId()?.toLongOrNull() ?: 0L

                // 1. 调用 JS Bridge addStrokeBySource，创建 MarkItemInfo + 渲染 + 返回接口数据
                val approxArg = if (approxJson != null) "`$approxJson`" else "undefined"
                val jsScript = "window.SlaxWebViewBridge.addStrokeBySource(" +
                    "`$sourceJson`, $currentUserId, $approxArg)"

                val resultJson = suspendCancellableCoroutine { cont ->
                    webViewState.evaluateJsWithCallback(jsScript) { result ->
                        cont.resumeWith(Result.success(result))
                    }
                }

                if (resultJson.isBlank() || resultJson == "null") {
                    println("[划线] addStrokeBySource 返回空")
                    return@launch
                }

                // 解析返回的 StrokeCreateData
                val actualJson = if (resultJson.startsWith("\"")) {
                    Json.decodeFromString<String>(resultJson)
                } else {
                    resultJson
                }
                val createData = Json.decodeFromString<StrokeCreateData>(actualJson)

                // 2. 转换 source 并调用后端
                val markSource = createData.source.map { src ->
                    when (src.type) {
                        "image" -> MarkPathItem.Image(path = src.xpath)
                        else -> MarkPathItem.Text(
                            path = src.xpath,
                            start = src.start_offset,
                            end = src.end_offset
                        )
                    }
                }

                val selectContent = createData.select_content.map { item ->
                    when (item.type) {
                        "image" -> item.src
                        else -> item.text
                    }
                }

                val params = AddMarkParams(
                    bookmark_uid = bookmarkIdStr,
                    type = MarkType.LINE,
                    source = markSource,
                    select_content = selectContent,
                    approx_source = createData.approx_source,
                )

                val result = apiService.addBookmarkMark(params)
                val markId = result.data?.mark_id ?: return@launch

                // 3. 回补 mark_id
                webViewState.evaluateJs(
                    "window.SlaxWebViewBridge.updateMarkIdByUuid('${createData.uuid}', $markId)"
                )

                // 4. 构造更新后的 markItemInfo（用新 uuid 替换空 id）
                val updatedInfo = markItemInfo.copy(
                    id = createData.uuid,
                    stroke = markItemInfo.stroke + BridgeMarkStrokeInfo(
                        markId = markId,
                        userId = currentUserId,
                    )
                )
                withContext(Dispatchers.Main) {
                    onComplete(updatedInfo)
                }
            }.onFailure { error ->
                println("[划线] 临时选区创建失败: ${error.message}")
                withContext(Dispatchers.Main) {
                    onComplete(markItemInfo)
                }
            }
        }
    }

    /**
     * 删除已有 mark 的划线。
     *
     * 流程为：
     * 1. 调用后端 /v1/mark/delete 删除划线记录
     * 2. 调用 JS Bridge removeStrokeByUuid 从前端移除划线渲染
     * 3. 更新 markItemInfo 并回调
     *
     * @param webViewState 用于执行 JS Bridge 调用
     * @param markItemInfo 当前选中的 mark 信息
     * @param onComplete 操作完成回调，参数为更新后的 markItemInfo（移除了当前用户的 stroke）
     */
    fun removeStrokeFromMark(
        webViewState: AppWebViewState,
        markItemInfo: BridgeMarkItemInfo,
        onComplete: (BridgeMarkItemInfo) -> Unit,
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            runCatching {
                val currentUserId = getCurrentUserId()?.toLongOrNull() ?: 0L

                // 找到当前用户的 stroke 记录，获取 markId 用于调用删除接口
                val userStroke = markItemInfo.stroke.find { it.userId == currentUserId }
                val markId = userStroke?.markId

                // 1. 调用后端删除划线（markId 存在时）
                if (markId != null) {
                    apiService.removeBookmark(markId)
                }

                // 2. 调用 JS Bridge 移除前端渲染
                webViewState.evaluateJs(
                    "window.SlaxWebViewBridge.removeStrokeByUuid('${markItemInfo.id}', $currentUserId)"
                )

                // 3. 更新 markItemInfo 并回调
                val updatedInfo = markItemInfo.copy(
                    stroke = markItemInfo.stroke.filter { it.userId != currentUserId }
                )
                withContext(Dispatchers.Main) {
                    onComplete(updatedInfo)
                }
            }.onFailure { error ->
                println("[划线] 删除失败: ${error.message}")
                withContext(Dispatchers.Main) {
                    onComplete(markItemInfo)
                }
            }
        }
    }

    /**
     * 为 mark 添加评论的完整链路。
     *
     * 根据 markItemInfo.id 是否为空自动选择路径：
     * - **已有 mark**（id 非空）：调用 addCommentByUuid 在本地追加评论
     * - **临时选区**（id 为空）：调用 addCommentBySource 创建 MarkItemInfo 并追加评论
     *
     * 后续流程统一：调用后端 API 创建 → 回补 mark_id → 更新 UI。
     *
     * @param webViewState 用于执行 JS Bridge 调用
     * @param markItemInfo 当前选中的 mark 信息
     * @param comment 评论文本内容
     * @param parentId 回复目标的 markId（普通评论传 null，回复时传被回复评论的 markId）
     * @param onComplete 操作完成回调，参数为更新后的 markItemInfo
     */
    @OptIn(kotlin.time.ExperimentalTime::class)
    fun addCommentToMark(
        webViewState: AppWebViewState,
        markItemInfo: BridgeMarkItemInfo,
        comment: String,
        parentId: Long? = null,
        onComplete: (BridgeMarkItemInfo) -> Unit,
    ) {
        val bookmarkIdStr = _bookmarkId.value ?: return
        val isTemporary = markItemInfo.id.isEmpty()

        if (isTemporary) {
            addCommentToTemporaryMark(webViewState, markItemInfo, bookmarkIdStr, comment, parentId, onComplete)
        } else {
            addCommentToExistingMark(webViewState, markItemInfo, bookmarkIdStr, comment, parentId, onComplete)
        }
    }

    /**
     * 已有 mark 的评论路径：addCommentByUuid → API → updateCommentMarkIdByUuid
     */
    @OptIn(kotlin.time.ExperimentalTime::class)
    private fun addCommentToExistingMark(
        webViewState: AppWebViewState,
        markItemInfo: BridgeMarkItemInfo,
        bookmarkIdStr: String,
        comment: String,
        parentId: Long?,
        onComplete: (BridgeMarkItemInfo) -> Unit,
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            runCatching {
                val currentUserId = getCurrentUserId()?.toLongOrNull() ?: 0L
                val currentUser = userInfo.value
                val now = kotlin.time.Clock.System.now()

                // 1. JS Bridge 乐观渲染
                val escapedComment = escapeJsTemplateString(comment)
                val escapedUsername = escapeJsTemplateString(currentUser?.name ?: "")
                val escapedAvatar = escapeJsTemplateString(currentUser?.picture ?: "")
                webViewState.evaluateJs(
                    "window.SlaxWebViewBridge.addCommentByUuid('${markItemInfo.id}', " +
                        "{userId: $currentUserId, comment: `$escapedComment`, username: `$escapedUsername`, avatar: `$escapedAvatar`})"
                )

                // 2. 调用后端创建评论
                val markType = if (parentId != null) MarkType.REPLY else MarkType.COMMENT
                val selectContent = markItemInfo.approx?.raw_text?.let { listOf(it) }
                    ?: listOf(markItemInfo.approx?.exact ?: "")

                val params = AddMarkParams(
                    bookmark_uid = bookmarkIdStr,
                    type = markType,
                    source = markItemInfo.source,
                    select_content = selectContent,
                    approx_source = markItemInfo.approx,
                    comment = comment,
                    parent_id = parentId,
                )

                val result = apiService.addBookmarkMark(params)
                val markId = result.data?.mark_id ?: return@launch

                // 3. 回补 mark_id
                webViewState.evaluateJs(
                    "window.SlaxWebViewBridge.updateCommentMarkIdByUuid('${markItemInfo.id}', $markId)"
                )

                // 4. 更新 markItemInfo 并回调
                val newComment = BridgeMarkCommentInfo(
                    markId = markId,
                    comment = comment,
                    userId = currentUserId,
                    username = currentUser?.name ?: "",
                    avatar = currentUser?.picture ?: "",
                    createdAt = now.toString(),
                )
                val updatedInfo = markItemInfo.copy(
                    comments = markItemInfo.comments + newComment
                )
                withContext(Dispatchers.Main) {
                    onComplete(updatedInfo)
                }
            }.onFailure { error ->
                println("[评论] 创建失败: ${error.message}")
                withContext(Dispatchers.Main) {
                    onComplete(markItemInfo)
                }
            }
        }
    }

    /**
     * 临时选区的评论路径：addCommentBySource → API → updateCommentMarkIdByUuid
     *
     * addCommentBySource 会在 JS 端创建新的 MarkItemInfo 并返回 uuid 和接口入参，
     * 后续流程与已有 mark 一致。
     */
    @OptIn(kotlin.time.ExperimentalTime::class)
    private fun addCommentToTemporaryMark(
        webViewState: AppWebViewState,
        markItemInfo: BridgeMarkItemInfo,
        bookmarkIdStr: String,
        comment: String,
        parentId: Long?,
        onComplete: (BridgeMarkItemInfo) -> Unit,
    ) {
        val sourceJson = escapeJsTemplateString(
            markDetailJson.encodeToString(markItemInfo.source)
        )
        val approxJson = markItemInfo.approx?.let {
            escapeJsTemplateString(markDetailJson.encodeToString(it))
        }

        viewModelScope.launch(Dispatchers.IO) {
            runCatching {
                val currentUserId = getCurrentUserId()?.toLongOrNull() ?: 0L
                val currentUser = userInfo.value
                val now = kotlin.time.Clock.System.now()

                val escapedComment = escapeJsTemplateString(comment)
                val escapedUsername = escapeJsTemplateString(currentUser?.name ?: "")
                val escapedAvatar = escapeJsTemplateString(currentUser?.picture ?: "")

                // 1. 调用 JS Bridge addCommentBySource，创建 MarkItemInfo + 渲染 + 返回接口数据
                val approxArg = if (approxJson != null) "`$approxJson`" else "undefined"
                val jsScript = "window.SlaxWebViewBridge.addCommentBySource(" +
                    "`$sourceJson`, " +
                    "{userId: $currentUserId, comment: `$escapedComment`, username: `$escapedUsername`, avatar: `$escapedAvatar`}, " +
                    "$approxArg)"

                // 使用 suspendCancellableCoroutine 等待 JS 回调
                val resultJson = suspendCancellableCoroutine { cont ->
                    webViewState.evaluateJsWithCallback(jsScript) { result ->
                        cont.resumeWith(Result.success(result))
                    }
                }

                if (resultJson.isBlank() || resultJson == "null") {
                    println("[评论] addCommentBySource 返回空")
                    return@launch
                }

                // 解析返回的 StrokeCreateData（包含 uuid 和接口入参）
                val actualJson = if (resultJson.startsWith("\"")) {
                    Json.decodeFromString<String>(resultJson)
                } else {
                    resultJson
                }
                val createData = Json.decodeFromString<StrokeCreateData>(actualJson)

                // 2. 将 StrokeCreateSource 转换为 MarkPathItem
                val markSource = createData.source.map { src ->
                    when (src.type) {
                        "image" -> MarkPathItem.Image(path = src.xpath)
                        else -> MarkPathItem.Text(
                            path = src.xpath,
                            start = src.start_offset,
                            end = src.end_offset
                        )
                    }
                }

                val selectContent = createData.select_content.map { item ->
                    when (item.type) {
                        "image" -> item.src
                        else -> item.text
                    }
                }

                // 3. 调用后端创建评论
                val markType = if (parentId != null) MarkType.REPLY else MarkType.COMMENT
                val params = AddMarkParams(
                    bookmark_uid = bookmarkIdStr,
                    type = markType,
                    source = markSource,
                    select_content = selectContent,
                    approx_source = createData.approx_source,
                    comment = comment,
                    parent_id = parentId,
                )

                val result = apiService.addBookmarkMark(params)
                val markId = result.data?.mark_id ?: return@launch

                // 4. 回补 mark_id
                webViewState.evaluateJs(
                    "window.SlaxWebViewBridge.updateCommentMarkIdByUuid('${createData.uuid}', $markId)"
                )

                // 5. 构造更新后的 markItemInfo（用新 uuid 替换空 id）
                val newComment = BridgeMarkCommentInfo(
                    markId = markId,
                    comment = comment,
                    userId = currentUserId,
                    username = currentUser?.name ?: "",
                    avatar = currentUser?.picture ?: "",
                    createdAt = now.toString(),
                )
                val updatedInfo = markItemInfo.copy(
                    id = createData.uuid,
                    comments = markItemInfo.comments + newComment
                )
                withContext(Dispatchers.Main) {
                    onComplete(updatedInfo)
                }
            }.onFailure { error ->
                println("[评论] 临时选区创建失败: ${error.message}")
                withContext(Dispatchers.Main) {
                    onComplete(markItemInfo)
                }
            }
        }
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
            withContext(Dispatchers.IO) {
                localBookmarkDao.updateLocalBookmarkReadPosition(id, scrollY)
            }
        }
    }

    fun flushReadPosition() {
        savePositionJob?.cancel()
        val id = _bookmarkId.value ?: return
        val position = currentPosition
        if (position < 0f) return
        viewModelScope.launch(Dispatchers.IO) {
            localBookmarkDao.updateLocalBookmarkReadPosition(id, position)
        }
    }

    fun flushOutlineScrollPosition() {
        outlineDelegate.flushScrollPosition()
    }

    override fun onCleared() {
        super.onCleared()

        flushReadPosition()

        contentJob?.cancel()
        contentJob = null
        commentDelegate.reset()
        outlineDelegate.reset()
        overviewDelegate.reset()
        overlayDelegate.reset()
    }
}
