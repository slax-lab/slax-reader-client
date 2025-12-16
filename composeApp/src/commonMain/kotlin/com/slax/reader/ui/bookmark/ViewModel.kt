package com.slax.reader.ui.bookmark

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.slax.reader.data.database.dao.BookmarkDao
import com.slax.reader.data.database.dao.LocalBookmarkDao
import com.slax.reader.data.database.model.UserBookmark
import com.slax.reader.data.database.model.UserTag
import com.slax.reader.data.network.ApiService
import com.slax.reader.data.network.dto.OutlineResponse
import com.slax.reader.data.network.dto.OverviewResponse
import com.slax.reader.data.preferences.AppPreferences
import com.slax.reader.data.preferences.ContinueReadingBookmark
import com.slax.reader.domain.sync.BackgroundDomain
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.*
import kotlinx.serialization.json.Json

data class OverviewState(
    val overview: String = "",
    val keyTakeaways: List<String> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null
)

data class OutlineState(
    val outline: String = "",
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


    private val _outlineContent = MutableStateFlow("")
    val outlineContent: StateFlow<String> = _outlineContent.asStateFlow()

    private val _outlineState = MutableStateFlow(OutlineState())
    val outlineState: StateFlow<OutlineState> = _outlineState.asStateFlow()

    // 锚点滚动事件流（使用 SharedFlow 确保事件不丢失）
    private val _scrollToAnchorEvent = MutableSharedFlow<String>(
        replay = 0,
        extraBufferCapacity = 1,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )
    val scrollToAnchorEvent: SharedFlow<String> = _scrollToAnchorEvent.asSharedFlow()

    // 触发锚点滚动
    fun scrollToAnchor(anchorText: String) {
        viewModelScope.launch {
            _scrollToAnchorEvent.emit(anchorText)
        }
    }

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

    fun loadOutline() {
        if (_outlineState.value.isLoading) {
            return
        }

        val bookmarkId = _bookmarkId.value ?: return
        viewModelScope.launch {
            val cacheOutline = withContext(Dispatchers.IO) {
                localBookmarkDao.getLocalBookmarkOutline(bookmarkId)
            }

            if (!cacheOutline.isNullOrEmpty()) {
                _outlineContent.value = cacheOutline
                _outlineState.update { state ->
                    state.copy(
                        outline = cacheOutline,
                        isLoading = false
                    )
                }
                return@launch
            }

            _outlineContent.value = ""
            _outlineState.value = OutlineState(isLoading = true)

            var fullOutline = ""

            try {
                apiService.getBookmarkOutline(bookmarkId).collect { response ->
                    when (response) {
                        is OutlineResponse.Outline -> {
                            fullOutline = response.content
                            _outlineContent.value = fullOutline
                            _outlineState.update { state ->
                                state.copy(outline = fullOutline, isLoading = true)
                            }
                        }

                        is OutlineResponse.Done -> {
                            _outlineState.update { state ->
                                state.copy(isLoading = false)
                            }

                            if (fullOutline.isNotEmpty()) {
                                withContext(Dispatchers.IO) {
                                    localBookmarkDao.updateLocalBookmarkOutline(
                                        bookmarkId = bookmarkId,
                                        outline = fullOutline
                                    )
                                }
                            }
                        }

                        is OutlineResponse.Error -> {
                            _outlineState.update { state ->
                                state.copy(error = response.message, isLoading = false)
                            }
                        }

                        else -> {
                        }
                    }
                }
            } catch (e: Exception) {
                _outlineState.update { state ->
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

    suspend fun updateBookmarkTitle(newTitle: String) = withContext(Dispatchers.IO) {
        _bookmarkId.value?.let { id ->
            return@withContext bookmarkDao.updateBookmarkAliasTitle(id, newTitle)
        }
    }
}
