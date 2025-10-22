package com.slax.reader.domain.auth

/**
 * Apple Sign In result containing only the authorization code
 */
data class AppleSignInResult(
    val code: String
)

/**
 * Platform-specific Apple Sign In provider
 */
expect class AppleSignInProvider() {
    suspend fun signIn(): Result<AppleSignInResult>
    fun isAvailable(): Boolean
}