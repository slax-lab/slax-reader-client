package com.slax.reader.reactnative

import com.slax.reader.data.network.ApiService
import com.slax.reader.data.network.dto.FeedbackParams
import de.voize.reaktnativetoolkit.annotation.ReactNativeMethod
import de.voize.reaktnativetoolkit.annotation.ReactNativeModule
import org.koin.core.component.KoinComponent

@ReactNativeModule("FeedbackModule")
class FeedbackModule {
//    private val koinHelper = object : KoinComponent {
//        val apiService: ApiService by inject()
//    }
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