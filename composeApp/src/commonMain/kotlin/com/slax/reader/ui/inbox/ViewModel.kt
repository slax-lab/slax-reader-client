package com.slax.reader.ui.inbox

import androidx.lifecycle.ViewModel
import com.slax.reader.data.database.dao.BookmarkDao
import com.slax.reader.domain.sync.BackgroundDomain

class InboxListViewModel(
    bookmarkDao: BookmarkDao,
    backgroundDomain: BackgroundDomain,
) : ViewModel() {

    val bookmarks = bookmarkDao.watchUserBookmarkList()

    val bookmarkStatusFlow = backgroundDomain.bookmarkStatusFlow
}