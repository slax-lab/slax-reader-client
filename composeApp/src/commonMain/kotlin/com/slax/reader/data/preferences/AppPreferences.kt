package com.slax.reader.data.preferences

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import com.slax.reader.utils.timeUnix
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

data class AuthInfo(
    val token: String,
    val userId: String
)

data class PowerSyncAuthInfo(
    val token: String,
    val refreshTime: Long,
    val connectUrl: String
)

@Serializable
data class ContinueReadingBookmark (
    val bookmarkId: String,
    val title: String,
    val scrollY: Int
)

class AppPreferences(private val dataStore: DataStore<Preferences>) {
    companion object {
        private val AUTH_TOKEN_KEY = stringPreferencesKey("auth_token")
        private val USER_ID_KEY = stringPreferencesKey("user_id")
        private val LAST_REFRESH_TIME = stringPreferencesKey("last_refresh_time")

        private val POWER_SYNC_REFRESH_TIME = longPreferencesKey("powersync_refresh_time")
        private val POWER_SYNC_TOKEN_KEY = stringPreferencesKey("powersync_token")
        private val POWER_SYNC_CONNECT_URL = stringPreferencesKey("powersync_connect_url")

        private val CONTINUE_READING_BOOKMARK_KEY = stringPreferencesKey("continue_reading_bookmark")
    }

    suspend fun getLastRefreshTime(): Long? {
        return dataStore.data.map { preferences ->
            preferences[LAST_REFRESH_TIME]?.toLong() ?: 0L
        }.firstOrNull()
    }

    fun getAuthInfo(): Flow<AuthInfo?> {
        return dataStore.data.map { preferences ->
            val token = preferences[AUTH_TOKEN_KEY]
            val userId = preferences[USER_ID_KEY]
            if (token != null && userId != null) AuthInfo(token, userId) else null
        }
    }

    suspend fun getAuthInfoSuspend(): String? {
        return dataStore.data.first()[AUTH_TOKEN_KEY]
    }

    suspend fun setAuthInfo(token: String, userId: String?) {
        withContext(Dispatchers.IO) {
            dataStore.edit { preferences ->
                preferences[AUTH_TOKEN_KEY] = token
                preferences[LAST_REFRESH_TIME] = timeUnix().toString()
                if (userId != null) preferences[USER_ID_KEY] = userId
            }
        }
    }

    suspend fun clearAuthToken() {
        dataStore.edit { preferences ->
            preferences.remove(AUTH_TOKEN_KEY)
            preferences.remove(USER_ID_KEY)
        }
    }

    suspend fun getPowerSyncToken(): PowerSyncAuthInfo? = withContext(Dispatchers.IO) {
        val prefs = dataStore.data.first()
        val token = prefs[POWER_SYNC_TOKEN_KEY]
        val refreshTime = prefs[POWER_SYNC_REFRESH_TIME]
        val connectUrl = prefs[POWER_SYNC_CONNECT_URL]
        return@withContext if (token != null && refreshTime != null && connectUrl != null) {
            PowerSyncAuthInfo(token, refreshTime, connectUrl)
        } else {
            null
        }
    }

    suspend fun setPowerSyncToken(token: PowerSyncAuthInfo) = withContext(Dispatchers.IO) {
        return@withContext dataStore.edit { preferences ->
            preferences[POWER_SYNC_TOKEN_KEY] = token.token
            preferences[POWER_SYNC_REFRESH_TIME] = token.refreshTime
        }
    }

    suspend fun getContinueReadingBookmark() = withContext(Dispatchers.IO) {
        val prefs = dataStore.data.first()
        val jsonString = prefs[CONTINUE_READING_BOOKMARK_KEY]
        return@withContext if (jsonString != null) {
            try {
                Json.decodeFromString<ContinueReadingBookmark>(jsonString)
            } catch (_: Exception) {
                null
            }
        } else {
            null
        }
    }

    suspend fun setContinueReadingBookmark(info: ContinueReadingBookmark) = withContext(Dispatchers.IO) {
        dataStore.edit { preferences ->
            val jsonString = Json.encodeToString(info)
            preferences[CONTINUE_READING_BOOKMARK_KEY] = jsonString
        }
    }

    suspend fun clearContinueReadingBookmark() = withContext(Dispatchers.IO) {
        dataStore.edit { preferences ->
            preferences.remove(CONTINUE_READING_BOOKMARK_KEY)
        }
    }
}