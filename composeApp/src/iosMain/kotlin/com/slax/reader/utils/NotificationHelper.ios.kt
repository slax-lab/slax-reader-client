package com.slax.reader.utils

import com.slax.reader.firebase.FirebaseBridge
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

actual suspend fun requestToken(): String {
    val bridge = FirebaseBridge.shared

    val granted = suspendCancellableCoroutine<Boolean> { cont ->
        bridge.requestNotificationPermission { granted ->
            if (cont.isActive) {
                cont.resume(granted)
            }
        }
    }

    if (!granted) {
        throw Exception("Notification permission denied")
    }

    val token = suspendCancellableCoroutine<String?> { cont ->
        bridge.getFCMToken { token ->
            if (cont.isActive) {
                cont.resume(token)
            }
        }
    }

    return token ?: throw Exception("Failed to get FCM token")
}