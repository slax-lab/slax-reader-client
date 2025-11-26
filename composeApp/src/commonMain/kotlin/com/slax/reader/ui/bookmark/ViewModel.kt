package com.slax.reader.ui.bookmark

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.slax.reader.data.database.dao.BookmarkDao
import com.slax.reader.data.database.dao.LocalBookmarkDao
import com.slax.reader.data.database.model.UserBookmark
import com.slax.reader.data.database.model.UserTag
import com.slax.reader.data.model.MarkDetail
import com.slax.reader.data.model.MarkPathItem
import com.slax.reader.data.model.SelectionData
import com.slax.reader.data.network.ApiService
import com.slax.reader.data.network.dto.OverviewResponse
import com.slax.reader.data.preferences.AppPreferences
import com.slax.reader.data.preferences.ContinueReadingBookmark
import com.slax.reader.domain.sync.BackgroundDomain
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.serialization.json.Json

data class OverviewState(
    val overview: String = "",
    val keyTakeaways: List<String> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null
)

class BookmarkDetailViewModel(
    private val bookmarkDao: BookmarkDao,
    private val localBookmarkDao: LocalBookmarkDao,
    private val backgroundDomain: BackgroundDomain,
    private val apiService: ApiService,
    private val appPreferences: AppPreferences
) : ViewModel() {

    var _bookmarkId = MutableStateFlow<String?>(null)

    private val _overviewContent = MutableStateFlow("")
    val overviewContent: StateFlow<String> = _overviewContent.asStateFlow()

    private val _overviewState = MutableStateFlow(OverviewState())
    val overviewState: StateFlow<OverviewState> = _overviewState.asStateFlow()

    fun loadOverview() {
        val bookmarkId = _bookmarkId.value ?: return

        viewModelScope.launch {
            val (cachedOverview, cachedKeyTakeaways) = withContext(Dispatchers.IO) {
                localBookmarkDao.getLocalBookmarkOverview(bookmarkId)
            }

            if (!cachedOverview.isNullOrEmpty() && !cachedKeyTakeaways.isNullOrEmpty()) {
                _overviewContent.value = cachedOverview
                _overviewState.update { state ->
                    state.copy(
                        keyTakeaways = cachedKeyTakeaways,
                        overview = cachedOverview,
                        isLoading = false
                    )
                }
                return@launch
            }

            _overviewContent.value = ""
            _overviewState.value = OverviewState(isLoading = true)

            var fullOverview = ""
            var fullKeyTakeaways = emptyList<String>()

            try {
                apiService.getBookmarkOverview(bookmarkId).collect { response ->
                    when (response) {
                        is OverviewResponse.Overview -> {
                            fullOverview += response.content
                            _overviewContent.value = fullOverview
                            _overviewState.update { state ->
                                state.copy(overview = fullOverview, isLoading = true)
                            }
                        }

                        is OverviewResponse.KeyTakeaways -> {
                            fullKeyTakeaways = response.content
                            _overviewState.update { state ->
                                state.copy(keyTakeaways = response.content)
                            }
                        }

                        is OverviewResponse.Done -> {
                            _overviewState.update { state ->
                                state.copy(isLoading = false)
                            }

                            if (fullOverview.isNotEmpty()) {
                                withContext(Dispatchers.IO) {
                                    // 序列化 keyTakeaways 为 JSON 字符串
                                    val keyTakeawaysJson = if (fullKeyTakeaways.isNotEmpty()) {
                                        Json.encodeToString(fullKeyTakeaways)
                                    } else {
                                        null
                                    }
                                    localBookmarkDao.updateLocalBookmarkOverview(
                                        bookmarkId = bookmarkId,
                                        overview = fullOverview,
                                        keyTakeaways = keyTakeawaysJson
                                    )
                                }
                            }
                        }

                        is OverviewResponse.Error -> {
                            _overviewState.update { state ->
                                state.copy(error = response.message, isLoading = false)
                            }
                        }

                        else -> {
                        }
                    }
                }
            } catch (e: Exception) {
                _overviewState.update { state ->
                    state.copy(error = e.message ?: "Unknown error", isLoading = false)
                }
            }
        }
    }

    fun setBookmarkId(id: String) {
        _bookmarkId.value = id
        _overviewContent.value = ""
        _overviewState.value = OverviewState()
    }

    suspend fun getTagNames(uuids: List<String>): List<UserTag> = withContext(Dispatchers.IO) {
        return@withContext bookmarkDao.getTagsByIds(uuids)
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    val userTagList: Flow<List<UserTag>> = bookmarkDao.watchUserTag()
        .distinctUntilChanged()
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    @OptIn(ExperimentalCoroutinesApi::class)
    val bookmarkDetail: StateFlow<List<UserBookmark>> = _bookmarkId
        .filterNotNull()
        .distinctUntilChanged()
        .flatMapLatest { id ->
            bookmarkDao.watchBookmarkDetail(id)
        }.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    @OptIn(ExperimentalCoroutinesApi::class)
    val selectedTagList: StateFlow<Set<UserTag>> = bookmarkDetail
        .mapLatest { bookmarks ->
            val tagIds = bookmarks.firstOrNull()?.metadataObj?.tags ?: emptyList()
            if (tagIds.isEmpty()) {
                emptySet()
            } else {
                getTagNames(tagIds).toHashSet()
            }
        }
        .distinctUntilChanged()
        .stateIn(viewModelScope, SharingStarted.Lazily, emptySet())

    suspend fun toggleStar(isStar: Boolean) = withContext(Dispatchers.IO) {
        _bookmarkId.value?.let { id ->
            return@withContext bookmarkDao.updateBookmarkStar(id, if (isStar) 1 else 0)
        }
    }

    suspend fun toggleArchive(isArchive: Boolean) = withContext(Dispatchers.IO) {
        _bookmarkId.value?.let { id ->
            return@withContext bookmarkDao.updateBookmarkArchive(id, if (isArchive) 1 else 0)
        }
    }

    suspend fun updateBookmarkTags(bookmarkId: String, newTagIds: List<String>) = withContext(Dispatchers.IO) {
        return@withContext bookmarkDao.updateMetadataField(bookmarkId, "tags", Json.encodeToString(newTagIds))
    }

    suspend fun getBookmarkContent(bookmarkId: String): String = withContext(Dispatchers.IO) {
        return@withContext backgroundDomain.getBookmarkContent(bookmarkId)
    }

    suspend fun createTag(tagName: String): UserTag = withContext(Dispatchers.IO) {
        return@withContext bookmarkDao.createTag(tagName)
    }

    suspend fun recordContinueBookmark(scrollY: Int) = withContext(Dispatchers.IO) {
        _bookmarkId.value?.let { id ->
            val detail = bookmarkDetail.value.firstOrNull { it.id == id } ?: return@withContext
            val continueBookmark = ContinueReadingBookmark(
                bookmarkId = id,
                title = detail.displayTitle,
                scrollY = scrollY
            )
            appPreferences.setContinueReadingBookmark(continueBookmark)
        }
    }

    suspend fun clearContinueBookmark() = withContext(Dispatchers.IO) {
        return@withContext appPreferences.clearContinueReadingBookmark()
    }

    // ==================== 标记管理功能 ====================

    private var webViewExecutor: WebViewExecutor? = null

    /**
     * 设置 WebView 执行器
     * 需要在 DetailScreen 中调用此方法设置 WebView 引用
     */
    fun setWebViewExecutor(executor: WebViewExecutor) {
        webViewExecutor = executor
    }

    /**
     * 初始化选择桥接
     */
    fun initializeSelectionBridge(userId: Int, containerId: String = "article") {
        val script = WebViewJsHelper.getInitBridgeScript(userId, containerId)
        webViewExecutor?.executeJavaScript(script)
    }

    /**
     * 启动选择监听
     */
    fun startMonitoring() {
        val script = WebViewJsHelper.getStartMonitoringScript()
        webViewExecutor?.executeJavaScript(script)
    }

    /**
     * 停止选择监听
     */
    fun stopMonitoring() {
        val script = WebViewJsHelper.getStopMonitoringScript()
        webViewExecutor?.executeJavaScript(script)
    }

    /**
     * 加载并绘制所有标记
     * TODO: 从后端 API 获取标记数据
     */
    suspend fun loadAndDrawMarks(bookmarkId: String) = withContext(Dispatchers.IO) {
        try {
            // TODO: 调用 API 获取标记数据
            // val markDetail = apiService.getBookmarkMarks(bookmarkId)

            // 临时使用空数据测试
            val markDetail = MarkDetail(
                markList = emptyList(),
                userList = emptyMap()
            )

            withContext(Dispatchers.Main) {
                drawMarks(markDetail)
            }
        } catch (e: Exception) {
            println("[ViewModel] Failed to load marks: ${e.message}")
        }
    }

    /**
     * 绘制所有标记
     */
    private fun drawMarks(markDetail: MarkDetail) {
        val script = WebViewJsHelper.getDrawMarksScript(markDetail)
        webViewExecutor?.executeJavaScript(script) { result ->
            println("[ViewModel] DrawMarks result: $result")
        }
    }

    /**
     * 创建划线标记
     */
    fun createHighlightMark(selectionData: SelectionData, userId: Int) {
        viewModelScope.launch {
            try {
                // 标记类型：1 = LINE (划线)
                val markType = 1

                // TODO: 调用后端 API 创建标记
                // val response = apiService.createMark(
                //     bookmarkId = _bookmarkId.value ?: return@launch,
                //     paths = selectionData.paths,
                //     approx = selectionData.approx,
                //     markType = markType,
                //     comment = ""
                // )

                // 临时生成一个 markId
                val tempMarkId = "temp_${System.currentTimeMillis()}"

                // 在 WebView 中绘制标记
                val script = WebViewJsHelper.getDrawMarkScript(
                    markId = tempMarkId,
                    paths = selectionData.paths,
                    markType = markType,
                    userId = userId,
                    comment = ""
                )
                webViewExecutor?.executeJavaScript(script) { result ->
                    println("[ViewModel] Highlight mark created: $result")
                }
            } catch (e: Exception) {
                println("[ViewModel] Failed to create highlight mark: ${e.message}")
            }
        }
    }

    /**
     * 创建评论标记
     */
    fun createCommentMark(selectionData: SelectionData, comment: String, userId: Int) {
        viewModelScope.launch {
            try {
                // 标记类型：2 = COMMENT (评论)
                val markType = 2

                // TODO: 调用后端 API 创建标记
                // val response = apiService.createMark(
                //     bookmarkId = _bookmarkId.value ?: return@launch,
                //     paths = selectionData.paths,
                //     approx = selectionData.approx,
                //     markType = markType,
                //     comment = comment
                // )

                // 临时生成一个 markId
                val tempMarkId = "temp_${System.currentTimeMillis()}"

                // 在 WebView 中绘制标记
                val script = WebViewJsHelper.getDrawMarkScript(
                    markId = tempMarkId,
                    paths = selectionData.paths,
                    markType = markType,
                    userId = userId,
                    comment = comment
                )
                webViewExecutor?.executeJavaScript(script) { result ->
                    println("[ViewModel] Comment mark created: $result")
                }
            } catch (e: Exception) {
                println("[ViewModel] Failed to create comment mark: ${e.message}")
            }
        }
    }

    /**
     * 删除标记
     */
    fun deleteMark(markId: String) {
        viewModelScope.launch {
            try {
                // TODO: 调用后端 API 删除标记
                // apiService.deleteMark(markId)

                // 从 WebView 中移除标记
                val script = WebViewJsHelper.getRemoveMarkScript(markId)
                webViewExecutor?.executeJavaScript(script)

                println("[ViewModel] Mark deleted: $markId")
            } catch (e: Exception) {
                println("[ViewModel] Failed to delete mark: ${e.message}")
            }
        }
    }

    /**
     * 清除所有标记
     */
    fun clearAllMarks() {
        val script = WebViewJsHelper.getClearAllMarksScript()
        webViewExecutor?.executeJavaScript(script)
    }
}
