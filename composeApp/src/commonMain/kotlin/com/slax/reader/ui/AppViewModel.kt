package com.slax.reader.ui

import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ViewModel
import com.slax.reader.data.database.dao.PowerSyncDao
import com.slax.reader.data.database.dao.UserDao
import org.koin.core.component.KoinComponent

class AppViewModel(
    private val userDao: UserDao,
    private val powerSyncDao: PowerSyncDao,
) : ViewModel(), KoinComponent, DefaultLifecycleObserver {

    val userInfo = userDao.watchUserInfo()
    var incrErr = 0

    val syncStatusData = powerSyncDao.watchPowerSyncStatus()

    val isConnecting: Boolean
        get() = syncStatusData.value?.connecting == true

    val isUploading: Boolean
        get() = syncStatusData.value?.uploading == true

    val hasError: Boolean
        get() {
            val hasErr = syncStatusData.value?.anyError != null
            if (hasErr) incrErr += 1
            return incrErr != 1 && hasErr
        }

    val isDownloading: Boolean
        get() = syncStatusData.value?.downloading == true

    val connected: Boolean
        get() {
            val isConnected = syncStatusData.value?.connected == true
            if (isConnected) incrErr = 0
            return isConnected
        }

    val syncType: String?
        get() = when {
            syncStatusData.value?.downloading == true -> "Downloading"
            syncStatusData.value?.uploading == true -> "Uploading"
            syncStatusData.value?.connecting == true -> "Connecting"
            else -> null
        }

    val downloadProgress: Float
        get() = syncStatusData.value?.downloadProgress?.let { progress ->
            if (progress.totalOperations > 0) {
                (progress.downloadedOperations.toFloat() / progress.totalOperations.toFloat()).coerceIn(0f, 1f)
            } else {
                0f
            }
        } ?: 0f

    override fun onResume(owner: LifecycleOwner) {
        incrErr = 1
    }
}
