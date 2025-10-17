package com.slax.reader.utils

import com.multiplatform.webview.setting.PlatformWebSettings
import com.multiplatform.webview.util.KLogSeverity
import com.multiplatform.webview.web.WebViewState

fun webViewStateSetting(webViewState: WebViewState) {
    webViewState.webSettings.apply {
        isJavaScriptEnabled = true
        supportZoom = false
        allowFileAccessFromFileURLs = false
        allowUniversalAccessFromFileURLs = false
        logSeverity = KLogSeverity.Error

        androidWebSettings.apply {
            domStorageEnabled = true
            safeBrowsingEnabled = true
            allowFileAccess = false
            layerType = PlatformWebSettings.AndroidWebSettings.LayerType.HARDWARE
        }
    }
}