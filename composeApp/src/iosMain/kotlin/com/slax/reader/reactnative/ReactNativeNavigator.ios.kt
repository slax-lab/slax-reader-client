package com.slax.reader.reactnative

import androidx.navigation.NavHostController
import app.slax.reader.rnbridge.RNBridge
import com.slax.reader.utils.NavigationHelper
import platform.UIKit.UIViewController

actual fun NavHostController.navigateToReactNative(
    route: String,
    params: Map<String, Any>?
) {
    val reactVC = RNBridge.createReactViewController(route, params?.toMap()) ?: return
    NavigationHelper.pushViewController(reactVC as UIViewController)
}
