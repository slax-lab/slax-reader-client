package com.slax.reader.utils

import com.google.firebase.ktx.Firebase
import com.google.firebase.messaging.ktx.messaging
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

actual object NotificationHelper {
    actual suspend fun requestTokenAndLog() {
        try {
            val token = suspendCancellableCoroutine<String?> { cont ->
                Firebase.messaging.token.addOnCompleteListener { task ->
                    if (cont.isActive) {
                        cont.resume(if (task.isSuccessful) task.result else null)
                    }
                }
            }
            if (token != null) {
                println("[NotificationHelper] FCM Token: $token")
            } else {
                println("[NotificationHelper] Failed to get FCM token")
            }
        } catch (e: Exception) {
            println("[NotificationHelper] Failed to get FCM token: ${e.message}")
        }
    }
}
