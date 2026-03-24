package com.slax.reader

import android.util.Log
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import dev.brewkits.kmpworkmanager.KmpWorkManager
import dev.brewkits.kmpworkmanager.background.domain.Constraints
import dev.brewkits.kmpworkmanager.background.domain.ExistingPolicy
import dev.brewkits.kmpworkmanager.background.domain.TaskTrigger
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

        val scheduler = KmpWorkManager.getInstance().backgroundTaskScheduler
        serviceScope.launch {
            try {
                val result = scheduler.enqueue(
                    id = "silent_push_print_task",
                    trigger = TaskTrigger.OneTime(initialDelayMs = 0),
                    workerClassName = "PrintWorker",
                    constraints = Constraints(),
                    inputJson = null,
                    policy = ExistingPolicy.REPLACE
                )
                Log.d(TAG, "Successfully enqueued PrintWorker: $result")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to enqueue PrintWorker", e)
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
