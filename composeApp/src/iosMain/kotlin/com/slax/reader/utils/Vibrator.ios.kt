package com.slax.reader.utils

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import platform.AudioToolbox.AudioServicesPlaySystemSound

private const val K_SYSTEM_SOUND_ID_VIBRATE: UInt = 4095u

@Composable
actual fun rememberVibrate(): () -> Unit {
    return remember {
        {
            AudioServicesPlaySystemSound(K_SYSTEM_SOUND_ID_VIBRATE)
        }
    }
}
