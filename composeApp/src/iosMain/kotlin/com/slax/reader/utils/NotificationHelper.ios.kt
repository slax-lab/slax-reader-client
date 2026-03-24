package com.slax.reader.utils

import com.slax.reader.firebase.FirebaseBridge
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

actual object NotificationHelper {
    actual suspend fun requestTokenAndLog() {
        val bridge = FirebaseBridge.shared

        val granted = suspendCancellableCoroutine<Boolean> { cont ->
            bridge.requestNotificationPermission { granted ->
                if (cont.isActive) {
                    cont.resume(granted)
                }
            }
        }

        if (!granted) {
            println("[NotificationHelper] Notification permission denied")
            return
        }

        val token = suspendCancellableCoroutine<String?> { cont ->
            bridge.getFCMToken { token ->
                if (cont.isActive) {
                    cont.resume(token)
                }
            }
        }

        if (token != null) {
            println("[NotificationHelper] FCM Token: $token")
        } else {
            println("[NotificationHelper] Failed to get FCM token")
        }
    }
}
