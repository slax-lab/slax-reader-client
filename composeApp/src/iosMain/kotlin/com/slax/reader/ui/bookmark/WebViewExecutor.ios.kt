package com.slax.reader.ui.bookmark

import platform.WebKit.WKWebView

/**
 * iOS WebView JavaScript 执行器
 */
actual class WebViewExecutor(private val webView: WKWebView) {

    /**
     * 执行 JavaScript 代码
     */
    actual fun executeJavaScript(script: String, callback: ((String?) -> Unit)?) {
        webView.evaluateJavaScript(script) { result, error ->
            if (error != null) {
                println("[WebViewExecutor] JavaScript execution error: ${error.localizedDescription}")
                callback?.invoke(null)
            } else {
                callback?.invoke(result?.toString())
            }
        }
    }
}
