package com.slax.reader.ui.bookmark

import androidx.compose.runtime.collectAsState
import androidx.lifecycle.ViewModel
import com.slax.reader.data.database.dao.BookmarkDao
import com.slax.reader.data.database.model.UserBookmark
import kotlinx.coroutines.flow.Flow

class BookmarkViewModel(
    private val bookmarkDao: BookmarkDao
) : ViewModel() {
    fun getTagNames(uuids: List<String>) {

    }

    fun getBookmarkDetail(bookmarkId: String): Flow<List<UserBookmark>> {
        return bookmarkDao.getBookmarkDetail(bookmarkId)
    }
}