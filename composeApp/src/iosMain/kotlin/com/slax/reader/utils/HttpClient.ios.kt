package com.slax.reader.utils

import io.ktor.client.*

actual fun HttpClientConfig<*>.configureSslPinning(pins: List<String>) {
    engine {
//        if (this is DarwinClientEngineConfig) {
//            handleChallenge { session, task, challenge, completionHandler ->
//                completionHandler(
////                    NSURLSessionAuthChallengePerformDefaultHandling,
//                    null
//                )
//            }
//        }
    }
}