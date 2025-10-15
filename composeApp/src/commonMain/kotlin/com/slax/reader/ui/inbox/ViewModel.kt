package com.slax.reader.ui.inbox

import androidx.lifecycle.ViewModel
import com.slax.reader.data.database.dao.BookmarkDao
import com.slax.reader.data.database.dao.PowerSyncDao
import com.slax.reader.data.database.dao.UserDao

class InboxListViewModel(
    bookmarkDao: BookmarkDao,
    userDao: UserDao,
    powerSyncDao: PowerSyncDao,
) : ViewModel() {

    val bookmarks = bookmarkDao.watchUserBookmarkList()

    val userInfo = userDao.watchUserInfo()

    val syncStatusData = powerSyncDao.watchPowerSyncStatus()

    val isConnecting: Boolean
        get() = syncStatusData.value?.connecting == true

    val isUploading: Boolean
        get() = syncStatusData.value?.uploading == true

    val hasError: Boolean
        get() = syncStatusData.value?.anyError != null

    val isDownloading: Boolean
        get() = syncStatusData.value?.downloading == true

    val downloadProgress: Float
        get() = syncStatusData.value?.downloadProgress?.let { progress ->
            if (progress.totalOperations > 0) {
                (progress.downloadedOperations.toFloat() / progress.totalOperations.toFloat()).coerceIn(0f, 1f)
            } else {
                0f
            }
        } ?: 0f
}