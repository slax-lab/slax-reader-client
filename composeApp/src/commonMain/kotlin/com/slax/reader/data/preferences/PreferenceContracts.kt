package com.slax.reader.data.preferences

import kotlinx.coroutines.flow.Flow

interface SettingsPreferences {
    fun getCacheCount(): Flow<Int>
    suspend fun setCacheCount(count: Int)
    fun getDownloadImages(): Flow<Boolean>
    suspend fun setDownloadImages(enabled: Boolean)
}

interface AuthTokenPreferences {
    suspend fun getAuthInfoSuspend(): String?
}
