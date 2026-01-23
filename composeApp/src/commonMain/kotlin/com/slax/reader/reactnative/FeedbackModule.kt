package com.slax.reader.reactnative

import com.slax.reader.data.network.ApiService
import com.slax.reader.data.network.dto.FeedbackParams
import de.voize.reaktnativetoolkit.annotation.ReactNativeMethod
import de.voize.reaktnativetoolkit.annotation.ReactNativeModule
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import kotlin.getValue

@ReactNativeModule("FeedbackModule")
class FeedbackModule {
    private val koinHelper = object : KoinComponent {
        val apiService: ApiService by inject()
    }

    @ReactNativeMethod
    suspend fun sendFeedback(param: FeedbackParams) {
        try {
            koinHelper.apiService.sendFeedback(param)
        } catch (e: Exception) {
            throw e
        }
    }
}