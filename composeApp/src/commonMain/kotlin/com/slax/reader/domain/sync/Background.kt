package com.slax.reader.domain.sync

import com.slax.reader.data.database.dao.BookmarkDao

class BackgroundDomain(
    private val bookmarkDao: BookmarkDao
) {
//    private val workerJob: Job = Job()

    fun startup() {
//        bookmarkDao.watchUserBookmarkList().collect { list -> list }
    }

    fun cleanup() {

    }

    fun getBookmarkContent(id: String) {

    }
}