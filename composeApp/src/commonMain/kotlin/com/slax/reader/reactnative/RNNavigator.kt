package com.slax.reader.reactnative

import androidx.navigation.NavController
import com.slax.reader.const.ReactNativeRoute

expect fun openReactNativePage(moduleName: String, params: Map<String, String>? = null)

fun NavController.navigateToRN(route: ReactNativeRoute, params: Map<String, String>? = null) {
    openReactNativePage(route.moduleName, params)
}
