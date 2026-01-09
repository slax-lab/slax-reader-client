package com.slax.reader.reactnative

import de.voize.reaktnativetoolkit.annotation.ReactNativeMethod
import de.voize.reaktnativetoolkit.annotation.ReactNativeModule
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

@ReactNativeModule("TestModule")
class TestModule {

    @OptIn(ExperimentalTime::class)
    @ReactNativeMethod
    suspend fun hello(): String {
        return "Hello from Kotlin!" + Clock.System.now()
    }

    @ReactNativeMethod
    suspend fun add(a: Int, b: Int): Int {
        return a + b
    }
}