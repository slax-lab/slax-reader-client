package com.slax.reader.domain.auth

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.coroutines.suspendCancellableCoroutine
import platform.AuthenticationServices.*
import platform.Foundation.*
import platform.UIKit.UIApplication
import platform.darwin.NSObject
import kotlin.coroutines.resume

actual class AppleSignInProvider {
    @OptIn(ExperimentalForeignApi::class)
    actual suspend fun signIn(): Result<AppleSignInResult> =
        suspendCancellableCoroutine { continuation ->
            val provider = ASAuthorizationAppleIDProvider()
            val request = provider.createRequest()

            val controller = ASAuthorizationController(
                authorizationRequests = listOf(request)
            )

            val delegate = object : NSObject(),
                ASAuthorizationControllerDelegateProtocol {

                override fun authorizationController(
                    controller: ASAuthorizationController,
                    didCompleteWithAuthorization: ASAuthorization
                ) {
                    val credential = didCompleteWithAuthorization.credential
                            as? ASAuthorizationAppleIDCredential

                    if (credential != null) {
                        val authCode = credential.authorizationCode
                            ?.toKotlinString() ?: ""

                        if (authCode.isNotEmpty()) {
                            val result = AppleSignInResult(code = authCode)
                            continuation.resume(Result.success(result))
                        } else {
                            continuation.resume(
                                Result.failure(Exception("Failed to get authorization code"))
                            )
                        }
                    } else {
                        continuation.resume(
                            Result.failure(Exception("Invalid credential"))
                        )
                    }
                }

                override fun authorizationController(
                    controller: ASAuthorizationController,
                    didCompleteWithError: NSError
                ) {
                    continuation.resume(
                        Result.failure(Exception(didCompleteWithError.localizedDescription))
                    )
                }
            }

            controller.delegate = delegate

            val window = UIApplication.sharedApplication.keyWindow
            val presentationDelegate = object : NSObject(),
                ASAuthorizationControllerPresentationContextProvidingProtocol {
                override fun presentationAnchorForAuthorizationController(
                    controller: ASAuthorizationController
                ) = window!!
            }

            controller.presentationContextProvider = presentationDelegate
            controller.performRequests()

            continuation.invokeOnCancellation {}
        }

    actual fun isAvailable(): Boolean = true
}

@OptIn(ExperimentalForeignApi::class)
private fun NSData.toKotlinString(): String? {
    val nsString = NSString.create(
        data = this,
        encoding = NSUTF8StringEncoding
    )
    return nsString?.toString()
}