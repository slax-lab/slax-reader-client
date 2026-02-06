package com.slax.reader.utils

import android.os.Bundle
import com.google.firebase.analytics.ktx.analytics
import com.google.firebase.crashlytics.ktx.crashlytics
import com.google.firebase.ktx.Firebase

actual object FirebaseHelper {
    private val analytics = Firebase.analytics
    private val crashlytics = Firebase.crashlytics

    actual fun logEvent(name: String, params: Map<String, Any>?) {
        analytics.logEvent(name, params?.toBundle())
    }

    actual fun setUserId(userId: String?) {
        analytics.setUserId(userId)
    }

    actual fun setUserProperty(name: String, value: String?) {
        analytics.setUserProperty(name, value)
    }

    actual fun setCrashlyticsUserId(userId: String?) {
        crashlytics.setUserId(userId ?: "")
    }
}

private fun Map<String, Any>.toBundle(): Bundle = Bundle().apply {
    forEach { (key, value) ->
        when (value) {
            is String -> putString(key, value)
            is Int -> putInt(key, value)
            is Long -> putLong(key, value)
            is Double -> putDouble(key, value)
            is Boolean -> putBoolean(key, value)
            else -> putString(key, value.toString())
        }
    }
}