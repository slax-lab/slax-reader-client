package com.slax.reader.reactnative.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import kotlinx.cinterop.ExperimentalForeignApi
import app.slax.reader.reactnative.bridge.ReactNativeBridge

@OptIn(ExperimentalForeignApi::class)
@Composable
actual fun RNDemoScreen(onBackClick: () -> Unit) {
    DisposableEffect(Unit) {
        ReactNativeBridge.shared().openReactNativeDemo()

        onBackClick()

        onDispose { }
    }
}
