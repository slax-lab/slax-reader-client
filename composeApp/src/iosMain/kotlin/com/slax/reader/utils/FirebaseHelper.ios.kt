package com.slax.reader.utils

import com.slax.reader.firebase.FirebaseBridge
import platform.Foundation.NSNumber

actual object FirebaseHelper {
    private val bridge: FirebaseBridge
        get() = FirebaseBridge.shared

    actual fun logEvent(name: String, params: Map<String, Any>?) {
        bridge.logEvent(name, parameters = params?.toNSDict())
    }

    actual fun setUserId(userId: String?) {
        bridge.setUserId(userId)
    }

    actual fun setUserProperty(name: String, value: String?) {
        bridge.setUserProperty(value, forName = name)
    }

    actual fun setCrashlyticsUserId(userId: String?) {
        bridge.setCrashlyticsUserId(userId)
    }
}

private fun Map<String, Any>.toNSDict(): Map<Any?, *>? {
    if (isEmpty()) return null
    return mapValues { (_, value) ->
        when (value) {
            is Int -> NSNumber(int = value)
            is Long -> NSNumber(longLong = value)
            is Double -> NSNumber(double = value)
            is Boolean -> NSNumber(bool = value)
            else -> value
        }
    }
}