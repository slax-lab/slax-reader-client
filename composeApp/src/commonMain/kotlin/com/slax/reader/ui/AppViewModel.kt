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

    val syncStatusData = powerSyncDao.watchPowerSyncStatus()

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

    val syncType: String?
        get() = when {
            syncStatusData.value?.downloading == true -> "正在下载数据"
            syncStatusData.value?.uploading == true -> "正在上传数据"
            syncStatusData.value?.connecting == true -> "正在连接"
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

    override fun onCreate(owner: LifecycleOwner) {
        println("==== create")
    }

    override fun onStart(owner: LifecycleOwner) {
        println("==== onStart")
    }

    override fun onResume(owner: LifecycleOwner) {
        println("==== onResume")
    }

    override fun onPause(owner: LifecycleOwner) {
        println("==== onPause")
    }

    override fun onStop(owner: LifecycleOwner) {
        println("==== onStop")
    }

    override fun onDestroy(owner: LifecycleOwner) {
        println("==== onDestroy")
    }
}
