package com.slax.reader.ui.bookmark

/**
 * 在 WebView 中执行 JavaScript 代码
 * 平台特定实现
 */
expect class WebViewExecutor {
    /**
     * 执行 JavaScript 代码
     * @param script JavaScript 代码
     * @param callback 可选的回调，接收执行结果
     */
    fun executeJavaScript(script: String, callback: ((String?) -> Unit)? = null)
}
