package com.slax.reader.reactnative

import app.slax.reader.reactnative.bridge.ReactNativeBridge
import com.slax.reader.utils.NavigationHelper
import kotlinx.cinterop.ExperimentalForeignApi
import platform.Foundation.NSMutableDictionary
import platform.UIKit.UIViewController

@OptIn(ExperimentalForeignApi::class)
actual fun openReactNativePage(moduleName: String, params: Map<String, String>?) {
    val initialProps = params?.let { map ->
        NSMutableDictionary().apply {
            map.forEach { (key, value) ->
                setObject(value, forKey = key)
            }
        }
    }

    val reactVC = ReactNativeBridge.shared().createReactViewController(moduleName, initialProps) ?: return
    NavigationHelper.pushViewController(reactVC as UIViewController)
}
