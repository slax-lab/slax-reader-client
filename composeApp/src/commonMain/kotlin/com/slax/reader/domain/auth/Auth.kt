package com.slax.reader.domain.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.slax.reader.data.file.FileManager
import com.slax.reader.data.network.ApiService
import com.slax.reader.data.network.dto.AuthParams
import com.slax.reader.data.preferences.AppPreferences
import com.slax.reader.utils.platform
import com.slax.reader.utils.timeUnix
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch

sealed class AuthState {
    data object Loading : AuthState()

    data object Unauthenticated : AuthState()

    data class Authenticated(val token: String, val userId: String) : AuthState()
}

class AuthDomain(
    private val appPreferences: AppPreferences,
    private val apiService: ApiService,
    private val fileManager: FileManager,
) : ViewModel() {

    private val _authState = MutableStateFlow<AuthState>(AuthState.Loading)
    val authState: StateFlow<AuthState> = _authState.asStateFlow()

    init {
        checkAuthStatus()
    }

    private fun checkAuthStatus() {
        viewModelScope.launch {
            val authInfo = appPreferences.getAuthInfo().firstOrNull()
            _authState.value = if (authInfo != null) {
                AuthState.Authenticated(token = authInfo.token, userId = authInfo.userId)
            } else {
                AuthState.Unauthenticated
            }
        }
    }

    suspend fun signIn(token: String): Result<Unit> {
        return try {
            val result = apiService.login(
                AuthParams(
                    code = token,
                    redirect_uri = "",
                    platform = platform,
                    type = "google"
                )
            )
            appPreferences.setAuthInfo(result.data!!.token, result.data.user_id)
            _authState.value = AuthState.Authenticated(token, result.data.user_id)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun signOut() {
        viewModelScope.launch {
            appPreferences.clearAuthToken()
            fileManager.deleteDataDirectory("bookmark")
            _authState.value = AuthState.Unauthenticated
        }
    }

    suspend fun refreshToken() {
        coroutineScope {
            val lastRefreshTime = appPreferences.getLastRefreshTime()
            val currentTime = timeUnix()
            if (lastRefreshTime != null && (currentTime - lastRefreshTime) > 24 * 60 * 60) {
                val res = apiService.refresh()
                appPreferences.setAuthInfo(res.data!!.token, null)
            }
        }
    }
}
