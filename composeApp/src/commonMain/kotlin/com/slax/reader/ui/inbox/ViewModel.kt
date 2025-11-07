package com.slax.reader.ui.inbox

import androidx.lifecycle.ViewModel
import com.slax.reader.data.database.dao.BookmarkDao
import com.slax.reader.data.database.dao.UserDao
import com.slax.reader.domain.coordinator.CoordinatorDomain
import com.slax.reader.domain.sync.BackgroundDomain
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.withContext

class InboxListViewModel(
    private val userDao: UserDao,
    private val bookmarkDao: BookmarkDao,
    private val backgroundDomain: BackgroundDomain,
    private val coordinatorDomain: CoordinatorDomain
) : ViewModel() {
    val userInfo = userDao.watchUserInfo()
    val syncState = coordinatorDomain.syncState

    val bookmarks = bookmarkDao.watchUserBookmarkList()

    val bookmarkStatusFlow = backgroundDomain.bookmarkStatusFlow

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
}