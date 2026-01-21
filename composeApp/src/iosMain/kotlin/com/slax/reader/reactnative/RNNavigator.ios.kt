package com.slax.reader.reactnative

import app.slax.reader.reactnative.bridge.ReactNativeBridge
import com.slax.reader.utils.NavigationHelper
import platform.UIKit.UIViewController

actual fun openReactNativePage(moduleName: String) {
    val reactVC = ReactNativeBridge.shared().createReactViewController(moduleName) ?: return
    NavigationHelper.pushViewController(reactVC as UIViewController)
}
