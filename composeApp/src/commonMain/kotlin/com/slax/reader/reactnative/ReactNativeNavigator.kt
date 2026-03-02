package com.slax.reader.reactnative

import androidx.navigation.NavHostController

expect fun NavHostController.navigateToReactNative(
    screen: String,
    params: Map<String, String> = emptyMap(),
)
