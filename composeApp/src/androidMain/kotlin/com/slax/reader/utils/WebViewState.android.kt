package com.slax.reader.utils

import android.webkit.WebView
import androidx.compose.runtime.Stable
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch

@Stable
actual class AppWebViewState actual constructor(private val scope: CoroutineScope) {
    internal var webView: WebView? = null

    private val _commands = MutableSharedFlow<WebViewCommand>(extraBufferCapacity = 8)
    internal val commands = _commands.asSharedFlow()

    private val _events = MutableSharedFlow<WebViewEvent>(extraBufferCapacity = 8)
    actual val events = _events.asSharedFlow()

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