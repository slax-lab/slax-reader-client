package com.slax.reader.ui.inbox

import androidx.lifecycle.ViewModel
import com.slax.reader.data.database.dao.BookmarkDao
import com.slax.reader.data.database.dao.PowerSyncDao
import com.slax.reader.data.database.dao.UserDao
import com.slax.reader.domain.sync.BackgroundDomain

class InboxListViewModel(
    private val bookmarkDao: BookmarkDao,
    private val userDao: UserDao,
    private val powerSyncDao: PowerSyncDao,
    private val backgroundDomain: BackgroundDomain,
) : ViewModel() {

    val bookmarks = bookmarkDao.watchUserBookmarkList()

    val userInfo = userDao.watchUserInfo()

    val syncStatusData = powerSyncDao.watchPowerSyncStatus()

    val bookmarkStatusFlow = backgroundDomain.bookmarkStatusFlow

    val isConnecting: Boolean
        get() = syncStatusData.value?.connecting == true

    val isUploading: Boolean
        get() = syncStatusData.value?.uploading == true

    val hasError: Boolean
        get() = syncStatusData.value?.anyError != null

    val isDownloading: Boolean
        get() = syncStatusData.value?.downloading == true

    val connected: Boolean
        get() = syncStatusData.value?.connected == true

    val downloadProgress: Float
        get() = syncStatusData.value?.downloadProgress?.let { progress ->
            if (progress.totalOperations > 0) {
                (progress.downloadedOperations.toFloat() / progress.totalOperations.toFloat()).coerceIn(0f, 1f)
            } else {
                0f
            }
        } ?: 0f
}