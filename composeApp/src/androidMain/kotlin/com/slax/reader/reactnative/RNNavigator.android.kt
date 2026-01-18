package com.slax.reader.reactnative

import android.app.Activity
import java.lang.ref.WeakReference
import com.slax.reader.RNActivity

private var currentActivityRef: WeakReference<Activity>? = null

fun setCurrentActivity(activity: Activity?) {
    currentActivityRef = activity?.let { WeakReference(it) }
}

actual fun openReactNativePage(moduleName: String) {
    val activity = currentActivityRef?.get() ?: return
    activity.startActivity(RNActivity.createIntent(activity, moduleName))
}
