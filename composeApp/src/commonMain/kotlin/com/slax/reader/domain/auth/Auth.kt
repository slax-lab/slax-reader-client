package com.slax.reader.domain.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.slax.reader.data.file.FileManager
import com.slax.reader.data.network.ApiService
import com.slax.reader.data.network.dto.AuthParams
import com.slax.reader.data.preferences.AppPreferences
import com.slax.reader.utils.platformType
import com.slax.reader.utils.timeUnix
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

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
            appPreferences.getAuthInfo()
                .distinctUntilChanged { old, new -> old?.token == new?.token }
                .collect { authInfo ->
                    _authState.value = if (authInfo != null) {
                        AuthState.Authenticated(token = authInfo.token, userId = authInfo.userId)
                    } else {
                        AuthState.Unauthenticated
                    }
                    withContext(Dispatchers.IO) {
                        if (_authState.value is AuthState.Authenticated && authInfo == null) fileManager.deleteDataDirectory(
                            "bookmark"
                        )
                    }
                }
        }
    }

    suspend fun signIn(code: String, type: String, redirectUrl: String = ""): Result<Unit> {
        return try {
            val result = apiService.login(
                AuthParams(
                    code = code,
                    redirect_uri = redirectUrl,
                    platform = platformType,
                    type = type
                )
            )
            appPreferences.setAuthInfo(result.data!!.token, result.data.user_id)
            _authState.value = AuthState.Authenticated(result.data.token, result.data.user_id)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun signOut() {
        viewModelScope.launch {
            appPreferences.clearAuthToken()
            withContext(Dispatchers.IO) {
                fileManager.deleteDataDirectory("bookmark")
            }
        }
        return
    }

    suspend fun refreshToken() = withContext(Dispatchers.IO) {
        try {
            val lastRefreshTime = appPreferences.getLastRefreshTime()
            val currentTime = timeUnix()
            if (lastRefreshTime != null && (currentTime - lastRefreshTime) > 24 * 60 * 60) {
                val res = apiService.refresh()
                appPreferences.setAuthInfo(res.data!!.token, null)
            }
            return@withContext
        } catch (e: Exception) {
            println("Error refreshing token: ${e.message}")
        }

    }
}
