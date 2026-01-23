package com.slax.reader.reactnative

import android.app.Activity
import android.os.Bundle
import java.lang.ref.WeakReference
import com.slax.reader.RNActivity

private var currentActivityRef: WeakReference<Activity>? = null

fun setCurrentActivity(activity: Activity?) {
    currentActivityRef = activity?.let { WeakReference(it) }
}

actual fun openReactNativePage(moduleName: String, params: Map<String, String>?) {
    val activity = currentActivityRef?.get() ?: return

    val bundle = params?.let {
        Bundle().apply {
            it.forEach { (key, value) ->
                putString(key, value)
            }
        }
    }

    activity.startActivity(RNActivity.createIntent(activity, moduleName, bundle))
}
