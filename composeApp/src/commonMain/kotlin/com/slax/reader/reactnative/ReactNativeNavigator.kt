package com.slax.reader.reactnative

import androidx.navigation.NavHostController

expect fun NavHostController.navigateToReactNative(
    route: String,
    params: Map<String, Any>? = null
)
