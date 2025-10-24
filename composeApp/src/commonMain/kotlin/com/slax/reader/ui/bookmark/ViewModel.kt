package com.slax.reader.ui.bookmark

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.slax.reader.data.database.dao.BookmarkDao
import com.slax.reader.data.database.model.UserBookmark
import com.slax.reader.data.database.model.UserTag
import com.slax.reader.data.network.ApiService
import com.slax.reader.data.network.dto.OverviewResponse
import com.slax.reader.data.network.dto.OverviewSocketData
import com.slax.reader.domain.sync.BackgroundDomain
import com.slax.reader.utils.wrapHtmlWithCSS
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.IO
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json

class BookmarkDetailViewModel(
    private val bookmarkDao: BookmarkDao,
    private val backgroundDomain: BackgroundDomain,
    private val apiService: ApiService,
) : ViewModel() {

    private var _bookmarkId = MutableStateFlow<String?>(null)

    fun setBookmarkId(id: String) {
        _bookmarkId.value = id
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
            bookmarkDao.updateBookmarkStar(id, if (isStar) 1 else 0)
        }
    }

    suspend fun toggleArchive(isArchive: Boolean) = withContext(Dispatchers.IO) {
        _bookmarkId.value?.let { id ->
            bookmarkDao.updateBookmarkArchive(id, if (isArchive) 1 else 0)
        }
    }

    suspend fun updateBookmarkTags(bookmarkId: String, newTagIds: List<String>) = withContext(Dispatchers.IO) {
        bookmarkDao.updateMetadataField(bookmarkId, "tags", Json.encodeToString(newTagIds))
    }

    suspend fun getBookmarkContent(bookmarkId: String): String = withContext(Dispatchers.IO) {
        val body = backgroundDomain.getBookmarkContent(bookmarkId)
        return@withContext wrapHtmlWithCSS(body)
    }

    suspend fun getBookmarkOverview(bookmarkId: String, refresh: Boolean = false): Flow<OverviewResponse> =
        withContext(Dispatchers.IO) {
            flow {
                var errorCacheText = ""
                var isDone = false

                apiService.streamBookmarkOverview(bookmarkId).collect { text ->
                    if (isDone || text.isEmpty()) {
                        return@collect
                    }

                    try {
                        val parsedJsons = try {
                            parseConcatenatedJson(errorCacheText + text).also {
                                errorCacheText = ""
                            }
                        } catch (_: Exception) {
                            println("Need concat: $text")
                            errorCacheText = text
                            return@collect
                        }

                        if (parsedJsons.isEmpty()) {
                            return@collect
                        }

                        for (res in parsedJsons) {
                            when {
                                res.type == "error" -> {
                                    emit(OverviewResponse.Error(res.message ?: "Unknown error"))
                                    isDone = true
                                    return@collect
                                }

                                res.type == "done" || res.data?.done == true -> {
                                    emit(OverviewResponse.Done)
                                    isDone = true
                                    return@collect
                                }

                                res.data != null -> {
                                    when {
                                        res.data.overview != null -> {
                                            emit(OverviewResponse.Overview(res.data.overview))
                                        }

                                        res.data.tags != null -> {
                                            emit(OverviewResponse.Tags(res.data.tags))
                                        }

                                        res.data.tag != null -> {
                                            emit(OverviewResponse.Tag(res.data.tag))
                                        }

                                        res.data.key_takeaways != null -> {
                                            emit(OverviewResponse.KeyTakeaways(res.data.key_takeaways))
                                        }
                                    }
                                }
                            }
                        }
                    } catch (error: Exception) {
                        println("Error processing overview: ${error.message}, text: $text")
                        emit(OverviewResponse.Error(error.message ?: "Unknown error"))
                        isDone = true
                    }
                }
            }
        }


    /**
     * Parse concatenated JSON strings separated by "}\n{"
     */
    private fun parseConcatenatedJson(inputString: String): List<OverviewSocketData> {
        val trimmedString = inputString.trim()
        if (trimmedString.isEmpty()) {
            return emptyList()
        }

        val parts = trimmedString.split("}\n{")
        val fixedParts = parts.mapIndexed { index, part ->
            when {
                index == 0 && parts.size > 1 -> "$part}"
                index == parts.size - 1 && parts.size > 1 -> "{$part"
                parts.size > 1 -> "{$part}"
                else -> part
            }
        }

        return fixedParts.mapNotNull { str ->
            try {
                Json.decodeFromString<OverviewSocketData>(str)
            } catch (e: Exception) {
                println("Failed to parse JSON: $str, error: ${e.message}")
                null
            }
        }
    }

}