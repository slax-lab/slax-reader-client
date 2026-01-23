package com.slax.reader.reactnative

import app.slax.reader.reactnative.bridge.ReactNativeBridge
import com.slax.reader.utils.NavigationHelper
import kotlinx.cinterop.ExperimentalForeignApi
import platform.UIKit.UIViewController

@OptIn(ExperimentalForeignApi::class)
actual fun openReactNativePage(moduleName: String, params: Map<String, String>?) {
    val reactVC = ReactNativeBridge.shared().createReactViewController(moduleName, params?.toMap()) ?: return
    NavigationHelper.pushViewController(reactVC as UIViewController)
}
