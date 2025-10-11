package com.slax.reader.ui.inbox

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.powersync.PowerSyncDatabase
import com.powersync.sync.SyncStatusData
import com.slax.reader.data.database.dao.BookmarkDao
import com.slax.reader.data.database.dao.UserDao
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class InboxListViewModel(
    bookmarkDao: BookmarkDao,
    userDao: UserDao,
    database: PowerSyncDatabase,
) : ViewModel() {
    var syncStatusData by mutableStateOf<SyncStatusData?>(null)
        private set

    var syncCompleted by mutableStateOf(false)
        private set

    init {
        viewModelScope.launch {
            database.currentStatus.asFlow().collect { status ->
                syncStatusData = status
                syncCompleted = status.connected && !status.downloading
            }
        }
    }

    val bookmarks = bookmarkDao.getUserBookmarkList()
        .distinctUntilChanged()
        .flowOn(Dispatchers.Default)
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val userInfo = userDao.getUserInfo()
        .distinctUntilChanged()
        .flowOn(Dispatchers.Default)
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = null
        )

    val isConnecting: Boolean
        get() = syncStatusData?.connecting == true

    val isUploading: Boolean
        get() = syncStatusData?.uploading == true

    val hasError: Boolean
        get() = syncStatusData?.anyError != null

    val isDownloading: Boolean
        get() = syncStatusData?.downloading == true

    val downloadProgress: Float
        get() = syncStatusData?.downloadProgress?.let { progress ->
            if (progress.totalOperations > 0) {
                (progress.downloadedOperations.toFloat() / progress.totalOperations.toFloat()).coerceIn(0f, 1f)
            } else {
                0f
            }
        } ?: 0f
}