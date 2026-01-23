package com.slax.reader.reactnative

import de.voize.reaktnativetoolkit.annotation.ReactNativeFlow
import de.voize.reaktnativetoolkit.annotation.ReactNativeMethod
import de.voize.reaktnativetoolkit.annotation.ReactNativeModule
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

@ReactNativeModule("TestModule")
class TestModule {
    private val counter = MutableStateFlow(0)

    @OptIn(ExperimentalTime::class)
    @ReactNativeMethod
    suspend fun hello(): String {
        return "Hello from Kotlin!" + Clock.System.now()
    }

    @ReactNativeMethod
    suspend fun add(a: Int, b: Int): Int {
        return a + b
    }

    @ReactNativeFlow
    suspend fun count(): Flow<Int> = counter

    @ReactNativeMethod
    suspend fun increment() {
        counter.update { it + 1 }
    }
}