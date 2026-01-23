package com.slax.reader.reactnative

import com.slax.reader.data.network.dto.FeedbackParams
import com.slax.reader.data.network.ApiService
import com.slax.reader.data.database.dao.BookmarkDao
import de.voize.reaktnativetoolkit.annotation.ReactNativeFlow
import de.voize.reaktnativetoolkit.annotation.ReactNativeMethod
import de.voize.reaktnativetoolkit.annotation.ReactNativeModule
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

@ReactNativeModule("TestModule")
class TestModule {
    private val counter = MutableStateFlow(0)

    private val koinHelper = object : KoinComponent {
        val apiService: ApiService by inject()
        val bookmarkDao: BookmarkDao by inject()
    }

    @OptIn(ExperimentalTime::class)
    @ReactNativeMethod
    suspend fun hello(): String {
        val res = koinHelper.apiService.getIAPProductIds()
        return "Hello from Kotlin! ${Clock.System.now()} - API: ${koinHelper.apiService.hashCode()} - ${res.message}"
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

    @ReactNativeMethod
    suspend fun sendFeedback(param: FeedbackParams) {
        println("反馈反馈")
        try {
//            koinHelper.apiService.sendFeedback(param)
            println("反馈发送成功")
        } catch (e: Exception) {
            println("发送反馈时出错: ${e.message}")
            throw e
        }
    }
}