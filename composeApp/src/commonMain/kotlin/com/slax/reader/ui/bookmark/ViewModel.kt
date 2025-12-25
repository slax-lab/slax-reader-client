package com.slax.reader.ui.bookmark

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.slax.reader.data.database.dao.BookmarkDao
import com.slax.reader.data.database.dao.LocalBookmarkDao
import com.slax.reader.data.database.dao.SubscriptionDao
import com.slax.reader.data.database.model.UserBookmark
import com.slax.reader.data.database.model.UserTag
import com.slax.reader.data.network.ApiService
import com.slax.reader.data.network.dto.OutlineResponse
import com.slax.reader.data.network.dto.OverviewResponse
import com.slax.reader.data.preferences.AppPreferences
import com.slax.reader.data.preferences.ContinueReadingBookmark
import com.slax.reader.domain.sync.BackgroundDomain
import com.slax.reader.utils.MarkdownHelper
import com.slax.reader.utils.parseInstant
import kotlinx.coroutines.*
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
    val isPending: Boolean = true,
    val isLoading: Boolean = false,
    val error: String? = null
)

class BookmarkDetailViewModel(
    private val bookmarkDao: BookmarkDao,
    private val subscriptionDao: SubscriptionDao,
    private val localBookmarkDao: LocalBookmarkDao,
    private val backgroundDomain: BackgroundDomain,
    private val apiService: ApiService,
    private val appPreferences: AppPreferences
) : ViewModel() {

    var _bookmarkId = MutableStateFlow<String?>(null)

    private val _overviewState = MutableStateFlow(OverviewState())
    val overviewState = _overviewState.asStateFlow()

    private val _outlineState = MutableStateFlow(OutlineState())
    val outlineState = _outlineState.asStateFlow()

    fun loadOverview() {
        val bookmarkId = _bookmarkId.value ?: return

        viewModelScope.launch {
            val (cachedOverview, cachedKeyTakeaways) = withContext(Dispatchers.IO) {
                localBookmarkDao.getLocalBookmarkOverview(bookmarkId)
            }

            if (!cachedOverview.isNullOrEmpty() && !cachedKeyTakeaways.isNullOrEmpty()) {
                _overviewState.update { state ->
                    state.copy(
                        overview = cachedOverview,
                        keyTakeaways = cachedKeyTakeaways,
                        isLoading = false,
                    )
                }
                return@launch
            }

            _overviewState.value = OverviewState(isLoading = true)

            var fullOverview = ""
            var fullKeyTakeaways = emptyList<String>()

            try {
                apiService.getBookmarkOverview(bookmarkId).collect { response ->
                    when (response) {
                        is OverviewResponse.Overview -> {
                            fullOverview += response.content
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
                val fixedOutline = MarkdownHelper.fixMarkdownLinks(cacheOutline)
                _outlineState.update { state ->
                    state.copy(
                        outline = fixedOutline,
                        isLoading = false
                    )
                }
                return@launch
            }

            _outlineState.value = OutlineState(isLoading = true, isPending = true)

            val streamProcessor = MarkdownHelper.createStreamProcessor()

            try {
                apiService.getBookmarkOutline(bookmarkId).collect { response ->
                    when (response) {
                        is OutlineResponse.Outline -> {
                            _outlineState.update { state ->
                                state.copy(outline = streamProcessor.process(response.content), isLoading = true, isPending = false)
                            }
                        }

                        is OutlineResponse.Done -> {
                            val finalOutline = streamProcessor.flush()

                            _outlineState.update { state ->
                                state.copy(outline = finalOutline, isLoading = false)
                            }

                            if (finalOutline.isNotEmpty()) {
                                withContext(Dispatchers.IO) {
                                    localBookmarkDao.updateLocalBookmarkOutline(
                                        bookmarkId = bookmarkId,
                                        outline = finalOutline
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

    @OptIn(kotlin.time.ExperimentalTime::class)
    suspend fun checkUserIsSubscribed(): Boolean = withContext(Dispatchers.IO)  {
        val info = subscriptionDao.getSubscriptionInfo() ?: return@withContext false

        try {
            val endTime = parseInstant(info.subscription_end_time)
            val now = kotlin.time.Clock.System.now()
            endTime > now
        } catch (e: Exception) {
            println("Error checking subscription: ${e.message}")
            false
        }
    }
}
