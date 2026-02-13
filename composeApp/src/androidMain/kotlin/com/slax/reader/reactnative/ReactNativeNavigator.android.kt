package com.slax.reader.reactnative

import android.content.Intent
import android.os.Bundle
import androidx.navigation.NavHostController

actual fun NavHostController.navigateToReactNative(
    route: String,
    params: Map<String, Any>?
) {
    val context = this.context
    val intent = Intent(context, ReactNativeActivity::class.java).apply {
        putExtra("route", route)
        params?.let {
            val bundle = Bundle()
            it.forEach { (key, value) ->
                when (value) {
                    is String -> bundle.putString(key, value)
                    is Int -> bundle.putInt(key, value)
                    is Boolean -> bundle.putBoolean(key, value)
                    is Double -> bundle.putDouble(key, value)
                    is Float -> bundle.putFloat(key, value)
                    is Long -> bundle.putLong(key, value)
                }
            }
            putExtra("params", bundle)
        }
    }
    context.startActivity(intent)
}
