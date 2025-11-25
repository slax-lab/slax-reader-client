package com.slax.reader.ui.inbox

import androidx.lifecycle.ViewModel
import com.slax.reader.data.database.dao.BookmarkDao
import com.slax.reader.data.database.dao.LocalBookmarkDao
import com.slax.reader.data.database.dao.UserDao
import com.slax.reader.domain.coordinator.CoordinatorDomain
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.withContext

class InboxListViewModel(
    private val userDao: UserDao,
    private val bookmarkDao: BookmarkDao,
    private val localBookmarkDao: LocalBookmarkDao,
    private val coordinatorDomain: CoordinatorDomain
) : ViewModel() {
    val userInfo = userDao.watchUserInfo()
    val syncState = coordinatorDomain.syncState

    val bookmarks = bookmarkDao.watchUserBookmarkList()

    val localBookmarkMap = localBookmarkDao.watchUserLocalBookmarkMap()

    private val _scrollToTopEvent = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val scrollToTopEvent: SharedFlow<Unit> = _scrollToTopEvent.asSharedFlow()

    fun scrollToTop() {
        _scrollToTopEvent.tryEmit(Unit)
    }

    suspend fun confirmEditTitle(bookmarkId: String, newTitle: String) {
        withContext(Dispatchers.IO) {
            bookmarkDao.updateBookmarkAliasTitle(bookmarkId, newTitle)
        }
    }

    suspend fun toggleStar(bookmarkId: String, isStar: Boolean) = withContext(Dispatchers.IO) {
        bookmarkDao.updateBookmarkStar(bookmarkId, if (isStar) 1 else 0)
    }

    suspend fun toggleArchive(bookmarkId: String, isArchive: Boolean) = withContext(Dispatchers.IO) {
        bookmarkDao.updateBookmarkArchive(bookmarkId, if (isArchive) 1 else 0)
    }

    suspend fun deleteBookmark(bookmarkId: String) = withContext(Dispatchers.IO) {
        bookmarkDao.deleteBookmark(bookmarkId)
    }

    suspend fun addLinkBookmark(url: String): String = withContext(Dispatchers.IO) {
        return@withContext bookmarkDao.createBookmark(url)
    }
}
