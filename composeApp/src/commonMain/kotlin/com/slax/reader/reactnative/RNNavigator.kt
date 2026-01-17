package com.slax.reader.reactnative

import androidx.navigation.NavController
import com.slax.reader.const.ReactNativeRoute

expect fun openReactNativePage(moduleName: String)

fun NavController.navigateToRN(route: ReactNativeRoute) {
    openReactNativePage(route.moduleName)
}
