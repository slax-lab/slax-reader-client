package com.slax.reader.ui.bookmark

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.slax.reader.data.database.dao.BookmarkDao
import com.slax.reader.data.database.model.UserBookmark
import com.slax.reader.data.database.model.UserTag
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.serialization.json.Json

class BookmarkDetailViewModel(
    private val bookmarkDao: BookmarkDao,
) : ViewModel() {
    private var _bookmarkId = MutableStateFlow<String?>(null)

    fun setBookmarkId(id: String) {
        _bookmarkId.value = id
    }

    suspend fun getTagNames(uuids: List<String>): List<UserTag> {
        return bookmarkDao.getTagsByIds(uuids)
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

    suspend fun toggleStar(bookmarkId: String, isStar: Boolean) {
        bookmarkDao.updateBookmarkStar(bookmarkId, if (isStar) 1 else 0)
    }

    suspend fun toggleArchive(bookmarkId: String, isArchive: Boolean) {
        bookmarkDao.updateBookmarkArchive(bookmarkId, if (isArchive) 1 else 0)
    }

    suspend fun updateBookmarkTags(bookmarkId: String, newTagIds: List<String>) {
        bookmarkDao.updateMetadataField(bookmarkId, "tags", Json.encodeToString(newTagIds))
    }
}