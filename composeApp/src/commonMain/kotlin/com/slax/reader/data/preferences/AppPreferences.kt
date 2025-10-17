package com.slax.reader.data.preferences

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.slax.reader.utils.timeUnix
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

data class AuthInfo(
    val token: String,
    val userId: String
)

class AppPreferences(private val dataStore: DataStore<Preferences>) {
    companion object {
        private val AUTH_TOKEN_KEY = stringPreferencesKey("auth_token")
        private val USER_ID_KEY = stringPreferencesKey("user_id")
        private val LAST_REFRESH_TIME = stringPreferencesKey("last_refresh_time")
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
}