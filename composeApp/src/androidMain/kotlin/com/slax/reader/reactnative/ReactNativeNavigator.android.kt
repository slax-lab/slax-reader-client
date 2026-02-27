package com.slax.reader.reactnative

import android.content.Intent
import androidx.navigation.NavHostController

actual fun NavHostController.navigateToReactNative(
    screen: String,
    params: Map<String, String>,
) {
    val context = this.context
    val intent = Intent(context, ReactNativeActivity::class.java).apply {
        putExtra("route", screen)
        params.forEach { (key, value) -> putExtra(key, value) }
    }
    context.startActivity(intent)
}
