package com.slax.reader.utils

import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import platform.WebKit.WKWebView

@Stable
actual class AppWebViewState actual constructor(private val scope: CoroutineScope) {
    internal var webView: WKWebView? = null

    private val _commands = MutableSharedFlow<WebViewCommand>(extraBufferCapacity = 8)
    internal val commands = _commands.asSharedFlow()

    private val _events = MutableSharedFlow<WebViewEvent>(extraBufferCapacity = 8)
    actual val events = _events.asSharedFlow()

    var topContentInsetPx by mutableFloatStateOf(0f)

    var onScrollChange: ((scrollY: Float, contentHeight: Float, visibleHeight: Float) -> Unit)? by mutableStateOf(null)

    actual fun evaluateJs(script: String) {
        scope.launch { _commands.emit(WebViewCommand.EvaluateJs(script)) }
    }

    actual fun scrollToAnchor(anchor: String) {
        evaluateJs("window.SlaxWebViewBridge.scrollToAnchor(`${escapeJsTemplateString(anchor)}`)")
    }

    internal fun dispatchEvent(event: WebViewEvent) {
        scope.launch { _events.emit(event) }
    }
}