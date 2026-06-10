package com.slax.reader.utils

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.useContents
import platform.CoreMotion.CMMotionManager
import platform.Foundation.NSOperationQueue
import kotlin.math.sqrt

private const val SHAKE_THRESHOLD_G = 2.7
private const val SHAKE_COOLDOWN_S = 1.0

@OptIn(ExperimentalForeignApi::class)
@Composable
actual fun ShakeDetector(onShake: () -> Unit) {
    val currentOnShake by rememberUpdatedState(onShake)

    DisposableEffect(Unit) {
        val motionManager = CMMotionManager()
        if (!motionManager.accelerometerAvailable) {
            return@DisposableEffect onDispose { }
        }

        motionManager.accelerometerUpdateInterval = 0.1
        var lastShakeTime = 0.0

        motionManager.startAccelerometerUpdatesToQueue(NSOperationQueue.mainQueue) { data, _ ->
            if (data == null) return@startAccelerometerUpdatesToQueue
            val gForce = data.acceleration.useContents { sqrt(x * x + y * y + z * z) }
            if (gForce > SHAKE_THRESHOLD_G) {
                val now = data.timestamp
                if (now - lastShakeTime > SHAKE_COOLDOWN_S) {
                    lastShakeTime = now
                    currentOnShake()
                }
            }
        }

        onDispose {
            motionManager.stopAccelerometerUpdates()
        }
    }
}
