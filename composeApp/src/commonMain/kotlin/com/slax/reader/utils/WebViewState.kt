package com.slax.reader.utils

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.SharedFlow

fun escapeJsTemplateString(input: String): String {
    return input
        .replace("\\", "\\\\")  // 反斜杠必须最先处理，避免二次转义
        .replace("`", "\\`")     // 反引号（模板字符串分隔符）
        .replace("$", "\\$")     // 美元符号（模板字符串插值）
        .replace("\n", "\\n")    // 换行符
        .replace("\r", "\\r")    // 回车符
        .replace("\t", "\\t")    // 制表符
}

@Stable
expect class AppWebViewState(scope: CoroutineScope) {
    val events: SharedFlow<WebViewEvent>

    fun evaluateJs(script: String)
    fun scrollToAnchor(anchor: String)
}

@Stable
sealed interface WebViewCommand {
    data class EvaluateJs(val script: String, val callback: ((String) -> Unit)? = null) : WebViewCommand
}

sealed interface WebViewEvent {
    data class ImageClick(val src: String, val allImages: List<String>) : WebViewEvent
    data class ScrollToPosition(val percentage: Double) : WebViewEvent
    data class ScrollChange(val scrollY: Float, val contentHeight: Float, val visibleHeight: Float) : WebViewEvent
    data object Tap : WebViewEvent
}

@Composable
fun rememberAppWebViewState(): AppWebViewState {
    val scope = rememberCoroutineScope()
    return remember { AppWebViewState(scope) }
}