package com.slax.reader.ui.bookmark

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.slax.reader.data.database.dao.BookmarkDao
import com.slax.reader.data.database.model.UserBookmark
import com.slax.reader.data.database.model.UserTag
import com.slax.reader.data.network.ApiService
import com.slax.reader.data.network.dto.OverviewResponse
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
    private val backgroundDomain: BackgroundDomain,
    private val apiService: ApiService,
) : ViewModel() {

    var _bookmarkId = MutableStateFlow<String?>(null)
    private var overviewCache: String? = null

    private val _overviewContent = MutableStateFlow("")
    val overviewContent: StateFlow<String> = _overviewContent.asStateFlow()

    private val _overviewState = MutableStateFlow(OverviewState())
    val overviewState: StateFlow<OverviewState> = _overviewState.asStateFlow()

    fun loadOverview() {
        val bookmarkId = _bookmarkId.value ?: return

        viewModelScope.launch {
            // 如果有缓存，直接使用
            overviewCache?.let {
                _overviewContent.value = it
                _overviewState.update { state ->
                    state.copy(overview = it, isLoading = false)
                }
                return@launch
            }

            // 流式加载
            _overviewContent.value = ""
            _overviewState.value = OverviewState(isLoading = true)

            try {
                apiService.getBookmarkOverview(bookmarkId).collect { response ->
                    when (response) {
                        is OverviewResponse.Overview -> {
                            overviewCache = (overviewCache ?: "") + response.content
                            _overviewContent.value = overviewCache!!
                            _overviewState.update { state ->
                                state.copy(overview = overviewCache!!, isLoading = true)
                            }
                        }

                        is OverviewResponse.KeyTakeaways -> {
                            _overviewState.update { state ->
                                state.copy(keyTakeaways = response.content)
                            }
                        }

                        is OverviewResponse.Done -> {
                            _overviewState.update { state ->
                                state.copy(isLoading = false)
                            }
                        }

                        is OverviewResponse.Error -> {
                            _overviewState.update { state ->
                                state.copy(error = response.message, isLoading = false)
                            }
                        }

                        else -> {
                            // 忽略其他类型
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
        overviewCache = null
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
}
