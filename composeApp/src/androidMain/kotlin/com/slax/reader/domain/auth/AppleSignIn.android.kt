package com.slax.reader.domain.auth

actual class AppleSignInProvider {
    actual suspend fun signIn(): Result<AppleSignInResult> {
        return Result.failure(Exception("Apple Sign In is not available on Android"))
    }

    actual fun isAvailable(): Boolean = false
}