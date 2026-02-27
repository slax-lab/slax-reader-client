package com.slax.reader.reactnative

import androidx.navigation.NavHostController
import app.slax.reader.rnbridge.RNBridge
import com.slax.reader.utils.NavigationHelper
import platform.UIKit.UIViewController

actual fun NavHostController.navigateToReactNative(
    screen: String,
    params: Map<String, String>,
) {
    val props: Map<Any?, Any?> = mapOf("route" to screen) + params
    val reactVC = RNBridge.createReactViewController("main", props) ?: return
    NavigationHelper.pushViewController(reactVC as UIViewController)
}
