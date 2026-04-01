package com.slax.reader.utils

import com.google.firebase.ktx.Firebase
import com.google.firebase.messaging.ktx.messaging
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

actual suspend fun requestToken(): String {
    return suspendCancellableCoroutine { cont ->
        Firebase.messaging.token.addOnCompleteListener { task ->
            if (!cont.isActive) return@addOnCompleteListener
            if (task.isSuccessful && task.result != null) {
                cont.resume(task.result!!)
            } else {
                cont.resumeWithException(Exception("Failed to get FCM token: ${task.exception?.message}"))
            }
        }
    }
}