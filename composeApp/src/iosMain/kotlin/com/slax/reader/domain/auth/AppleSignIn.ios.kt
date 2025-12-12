package com.slax.reader.domain.auth

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.coroutines.suspendCancellableCoroutine
import platform.AuthenticationServices.*
import platform.Foundation.*
import platform.UIKit.UIApplication
import platform.darwin.NSObject
import kotlin.coroutines.resume

actual class AppleSignInProvider {
    private var currentController: ASAuthorizationController? = null
    private var currentDelegate: NSObject? = null
    private var currentPresentationDelegate: NSObject? = null

    @OptIn(ExperimentalForeignApi::class)
    actual suspend fun signIn(): Result<AppleSignInResult> =
        suspendCancellableCoroutine { continuation ->
            val provider = ASAuthorizationAppleIDProvider()
            val request = provider.createRequest().apply {
                requestedScopes = listOf(
                    ASAuthorizationScopeFullName,
                    ASAuthorizationScopeEmail
                )
            }

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

                        val idToken = credential.identityToken
                            ?.toKotlinString() ?: ""

                        if (authCode.isNotEmpty()) {
                            val result = AppleSignInResult(authCode, idToken)
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
                    cleanup()
                    if (didCompleteWithError.code == 1001L) {
                        continuation.resume(
                            Result.failure(Exception("User canceled"))
                        )
                        return
                    }
                    val errorMessage = when (didCompleteWithError.code) {
                        1002L -> "Authorization request not handled"
                        1003L -> "Authorization request failed"
                        1004L -> "Authorization request not interactive"
                        else -> didCompleteWithError.localizedDescription
                    }
                    continuation.resume(
                        Result.failure(Exception(errorMessage))
                    )
                }
            }

            val window = UIApplication.sharedApplication.keyWindow
            val presentationDelegate = object : NSObject(),
                ASAuthorizationControllerPresentationContextProvidingProtocol {
                override fun presentationAnchorForAuthorizationController(
                    controller: ASAuthorizationController
                ) = window!!
            }

            currentController = controller
            currentDelegate = delegate
            currentPresentationDelegate = presentationDelegate

            controller.delegate = delegate
            controller.presentationContextProvider = presentationDelegate
            controller.performRequests()

            continuation.invokeOnCancellation {
                cleanup()
            }
        }

    private fun cleanup() {
        currentController = null
        currentDelegate = null
        currentPresentationDelegate = null
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