package com.slax.reader.reactnative.ui

import android.content.Intent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.platform.LocalContext

@Composable
actual fun RNDemoScreen(onBackClick: () -> Unit) {
    val context = LocalContext.current

    DisposableEffect(Unit) {
        val intent = Intent(context, RNDemoActivity::class.java)
        context.startActivity(intent)
        onBackClick()
        onDispose { }
    }
}
