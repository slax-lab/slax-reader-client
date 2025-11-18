package com.slax.reader.domain.auth

data class AppleSignInResult(
    val code: String
)

expect class AppleSignInProvider() {
    suspend fun signIn(): Result<AppleSignInResult>
    fun isAvailable(): Boolean
}