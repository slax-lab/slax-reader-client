package com.slax.reader.utils

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.remember
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

data class WebViewCookie(
    val name: String,
    val value: String,
    val domain: String,
    val path: String = "/",
    val secure: Boolean = false,
    val httpOnly: Boolean = false,
    val expiresDate: Long? = null
)

@Stable
expect class AppWebViewState(
    scope: CoroutineScope,
    initialCookies: List<WebViewCookie>? = null
) {
    val events: SharedFlow<WebViewEvent>
    val initialCookies: List<WebViewCookie>?

    fun evaluateJs(script: String)
    fun scrollToAnchor(anchor: String)
    fun reload()
    fun setCookies(cookies: List<WebViewCookie>, url: String)
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
    data object PageLoaded : WebViewEvent
    data class Scroll(val scrollX: Double, val scrollY: Double, val contentHeight: Double, val visibleHeight: Double) : WebViewEvent
    data class PurchaseWithOffer(val productId: String, val orderId: String, val offer: IAPProductOffer) : WebViewEvent
    data class Purchase(val productId: String, val orderId: String) : WebViewEvent

    data object BookmarkRetry: WebViewEvent

    data object BookmarkFeedback: WebViewEvent
}

@Composable
fun rememberAppWebViewState(
    scope: CoroutineScope,
    initialCookies: List<WebViewCookie>? = null
): AppWebViewState {
    return remember(initialCookies) { AppWebViewState(scope, initialCookies) }
}