package com.slax.reader

import android.util.Log
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.slax.reader.domain.silent.BackgroundTaskRunner
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

class SlaxFirebaseMessagingService : FirebaseMessagingService() {

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    companion object {
        private const val TAG = "SilentPush"
    }

    override fun onMessageReceived(message: RemoteMessage) {
        super.onMessageReceived(message)
        Log.d(TAG, "Received remote message: ${message.data}")

        serviceScope.launch {
            try {
                BackgroundTaskRunner.onSilentPush(message.data)
                Log.d(TAG, "Background task completed")
            } catch (e: Exception) {
                Log.e(TAG, "Background task failed", e)
            }
        }
    }

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Log.d(TAG, "FCM Token refreshed: $token")
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()
    }
}
