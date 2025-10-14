package com.slax.reader.domain.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.slax.reader.data.network.ApiService
import com.slax.reader.data.network.dto.AuthParams
import com.slax.reader.data.preferences.AppPreferences
import com.slax.reader.utils.platform
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
) : ViewModel() {

    private val _authState = MutableStateFlow<AuthState>(AuthState.Loading)
    val authState: StateFlow<AuthState> = _authState.asStateFlow()

    init {
        checkAuthStatus()
    }

    private fun checkAuthStatus() {
        viewModelScope.launch {
            val token = appPreferences.getAuthToken().firstOrNull()
            val userId = appPreferences.getUserId().firstOrNull()
            _authState.value = if (!token.isNullOrEmpty()) {
                AuthState.Authenticated(token = token, userId = userId!!)
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
            if (result.code != 200) {
                Result.failure(Exception("Login failed: ${result.message}"))
            } else {
                appPreferences.setAuthToken(result.data!!.token)
                appPreferences.setUserId(result.data.user_id)
                _authState.value = AuthState.Authenticated(token, result.data.user_id)
                Result.success(Unit)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun signOut() {
        viewModelScope.launch {
            appPreferences.clearAuthToken()
            _authState.value = AuthState.Unauthenticated
        }
    }
}
