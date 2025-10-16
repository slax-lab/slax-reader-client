package com.slax.reader.domain.sync

import com.slax.reader.data.database.dao.BookmarkDao
import com.slax.reader.data.database.dao.FileManagerDao
import com.slax.reader.data.file.FileManager
import kotlinx.coroutines.*
import kotlin.time.ExperimentalTime

class BackgroundDomain(
    private val bookmarkDao: BookmarkDao,
    private val fileManagerDao: FileManagerDao,
    private val fileManager: FileManager
) {
    private val workerScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    // max size 5G
    private val maximumSize = 5L * 1024 * 1024 * 1024

    // max save cache 30day
    private val longestSaveDuration = 30L * 24 * 60 * 60 * 1000

    // auto download recent 3 days
    private val recentDownloadDuration = 3L * 24 * 60 * 60 * 1000

    // max download concurrent
    private val maxDownloadConcurrent = 3

    @OptIn(ExperimentalTime::class)
    fun startup() {
//        workerScope.launch {
////            bookmarkDao.watchUserBookmarkList().collect { list ->
////                list.filter { bookmark ->
////                    val updateAt = Instant.parse(bookmark.updatedAt).toEpochMilliseconds()
////                    val now = kotlin.time.Clock.System.now()
////                    if (updateAt + DurationUnit(recentDownloadDuration) > now) {
////
////                    }
////                    val updatedAt = parser(strTime = bookmark.updatedAt)
////                    if (isBefore(now(), updatedAt + recentDownloadDuration)) {
////                        val file = fileManagerDao.getFileByBookmarkId(bookmark.id)
////                        file == null || file.status != "downloaded"
////                    } else {
////                        false
////                    }
////                }
////            }
//        }
    }

    fun cleanup() {
        workerScope.cancel()
    }

    fun getBookmarkContent(id: String) {

    }
}