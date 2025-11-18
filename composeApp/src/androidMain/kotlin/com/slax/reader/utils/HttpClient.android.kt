package com.slax.reader.utils

import io.ktor.client.*

actual fun HttpClientConfig<*>.configureSslPinning(pins: List<String>) {
    engine {
//        if (this is OkHttpConfig) {
//            val certificatePinner = CertificatePinner.Builder().apply {
//                pins.forEach { pin ->
//                    add("*.slax.dev", pin)
//                    add("*.slax.com", pin)
//                    add("*.slax.app", pin)
//                }
//            }.build()
//
//            config {
//                certificatePinner(certificatePinner)
//            }
//        }
    }
}
