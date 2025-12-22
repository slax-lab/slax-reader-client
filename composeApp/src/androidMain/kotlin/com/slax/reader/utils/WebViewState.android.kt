package com.slax.reader.utils

import android.webkit.CookieManager
import android.webkit.WebView
import androidx.compose.runtime.Stable
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@Stable
actual class AppWebViewState actual constructor(
    private val scope: CoroutineScope,
    actual val initialCookies: List<WebViewCookie>?
) {
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

    actual fun reload() {
        webView?.reload()
    }

    actual fun setCookies(cookies: List<WebViewCookie>, url: String) {
        val cookieManager = CookieManager.getInstance()
        cookieManager.setAcceptCookie(true)

        cookies.forEach { cookie ->
            val cookieString = buildString {
                append("${cookie.name}=${cookie.value}")
                append("; domain=${cookie.domain}")
                append("; path=${cookie.path}")
                if (cookie.secure) append("; secure")
                if (cookie.httpOnly) append("; httponly")
                cookie.expiresDate?.let {
                    val dateFormat = SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss zzz", Locale.US)
                    dateFormat.timeZone = TimeZone.getTimeZone("GMT")
                    append("; expires=${dateFormat.format(Date(it))}")
                }
            }
            cookieManager.setCookie(url, cookieString)
        }

        cookieManager.flush()
    }
}