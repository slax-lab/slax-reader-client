package com.slax.reader.reactnative

import de.voize.reaktnativetoolkit.annotation.ReactNativeMethod
import de.voize.reaktnativetoolkit.annotation.ReactNativeModule

@ReactNativeModule("NavigationModule")
class NavigationModule {

    @ReactNativeMethod
    suspend fun goBack() {
        navigationGoBack()
    }
}