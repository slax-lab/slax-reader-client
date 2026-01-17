package com.slax.reader.reactnative

import android.app.Activity
import com.slax.reader.RNActivity

private var currentActivity: Activity? = null

fun setCurrentActivity(activity: Activity) {
    currentActivity = activity
}

actual fun openReactNativePage(moduleName: String) {
    val activity = currentActivity ?: return
    activity.startActivity(RNActivity.createIntent(activity, moduleName))
}
