package com.slax.reader.ui.inbox

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import com.slax.reader.data.database.dao.BookmarkDao
import com.slax.reader.data.database.model.InboxListBookmarkItem
import com.slax.reader.domain.sync.BackgroundDomain
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

class InboxListViewModel(
    private val bookmarkDao: BookmarkDao,
    private val backgroundDomain: BackgroundDomain,
) : ViewModel() {

    val bookmarks = bookmarkDao.watchUserBookmarkList()

    val bookmarkStatusFlow = backgroundDomain.bookmarkStatusFlow

    // 标题编辑状态管理
    var editingBookmark by mutableStateOf<InboxListBookmarkItem?>(null)
        private set

    var editTitleText by mutableStateOf("")
        private set

    var justUpdatedBookmarkId by mutableStateOf<String?>(null)
        private set

    fun startEditTitle(bookmark: InboxListBookmarkItem) {
        editingBookmark = bookmark
        editTitleText = bookmark.displayTitle()
    }

    fun updateEditTitleText(text: String) {
        editTitleText = text
    }

    suspend fun confirmEditTitle() {
        val currentBookmark = editingBookmark ?: return
        val trimmedTitle = editTitleText.trim()

        if (trimmedTitle.isNotEmpty() && trimmedTitle != currentBookmark.displayTitle()) {
            editTitle(currentBookmark.id, trimmedTitle)
            justUpdatedBookmarkId = currentBookmark.id
            cancelEditTitle()
            // 闪烁动画持续后清除标记 (180ms * 4 = 720ms)
            delay(800)
            justUpdatedBookmarkId = null
        } else {
            cancelEditTitle()
        }
    }

    fun cancelEditTitle() {
        editingBookmark = null
        editTitleText = ""
    }

    suspend fun toggleStar(bookmarkId: String, isStar: Boolean) = withContext(Dispatchers.IO) {
        bookmarkDao.updateBookmarkStar(bookmarkId, if (isStar) 1 else 0)
    }

    suspend fun toggleArchive(bookmarkId: String, isArchive: Boolean) = withContext(Dispatchers.IO) {
        bookmarkDao.updateBookmarkArchive(bookmarkId, if (isArchive) 1 else 0)
    }

    private suspend fun editTitle(bookmarkId: String, title: String) = withContext(Dispatchers.IO) {
        bookmarkDao.updateBookmarkAliasTitle(bookmarkId, title)
    }

    suspend fun deleteBookmark(bookmarkId: String) = withContext(Dispatchers.IO) {
        bookmarkDao.deleteBookmark(bookmarkId)
    }
}