package com.slax.reader.ui.bookmark

import android.webkit.WebView

/**
 * Android WebView JavaScript 执行器
 */
actual class WebViewExecutor(private val webView: WebView) {

    /**
     * 执行 JavaScript 代码
     */
    actual fun executeJavaScript(script: String, callback: ((String?) -> Unit)?) {
        webView.post {
            webView.evaluateJavascript(script) { result ->
                callback?.invoke(result)
            }
        }
    }
}
