package com.slax.reader.utils

import androidx.compose.ui.graphics.Color
import com.multiplatform.webview.setting.PlatformWebSettings
import com.multiplatform.webview.util.KLogSeverity
import com.multiplatform.webview.web.WebViewState

expect fun configureIOSWebView(webView: Any)

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

        iOSWebSettings.apply {
            isInspectable = true
            scrollEnabled = false
            bounces = false
            opaque = false
            showHorizontalScrollIndicator = false
            showVerticalScrollIndicator = false
            backgroundColor = Color(0xFFFCFCFC)
            underPageBackgroundColor = Color(0xFFFCFCFC)
        }
    }
}

fun wrapHtmlWithCSS(htmlContent: String, cssContent: String): String {
    return """
        <!DOCTYPE html>
        <html>
        <head>
            <meta charset="UTF-8">
            <meta name="viewport" content="width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=no">
            <meta name="color-scheme" content="light only">
            <meta name="supported-color-schemes" content="light">
            <style>
                $cssContent
            </style>
        </head>
        <body>
            $htmlContent
        </body>
        </html>
    """.trimIndent()
}