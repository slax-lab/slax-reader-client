package com.slax.reader.utils

import android.annotation.SuppressLint
import android.graphics.Color
import android.webkit.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView


private class JsBridge(private val onMessage: (String) -> Unit) {
    @JavascriptInterface
    fun postMessage(message: String) {
        onMessage(message)
    }
}

@SuppressLint("SetJavaScriptEnabled", "JavascriptInterface")
@Composable
actual fun AppWebView(
    url: String?,
    htmlContent: String?,
    updateKey: String,
    modifier: Modifier,
    onHeightChange: ((Double) -> Unit)?,
) {
    var lastSignature by remember { mutableStateOf<String?>(null) }
    val currentSignature = updateKey

    val jsBridge = remember(onHeightChange) {
        JsBridge { msg ->
            try {
                val heightValue = Regex("\"height\":\\s*([0-9.]+)")
                    .find(msg)?.groupValues?.getOrNull(1)?.toDouble()
                if (heightValue != null) onHeightChange?.invoke(heightValue)
            } catch (_: Throwable) {
            }
        }
    }

    AndroidView(
        modifier = modifier,
        factory = { context ->
            WebView(context).apply {
                setBackgroundColor(Color.TRANSPARENT)
                isVerticalScrollBarEnabled = false
                isHorizontalScrollBarEnabled = false

                settings.apply {
                    javaScriptEnabled = true
                    domStorageEnabled = true
                    builtInZoomControls = false
                    displayZoomControls = false
                    allowFileAccess = false
                    allowContentAccess = false
                    cacheMode = WebSettings.LOAD_DEFAULT
                }

                addJavascriptInterface(jsBridge, JS_BRIDGE_NAME)

                webChromeClient = WebChromeClient()
                webViewClient = object : WebViewClient() {
                    override fun onPageFinished(view: WebView, url: String?) {
                        super.onPageFinished(view, url)
                        view.evaluateJavascript(HEIGHT_MONITOR_SCRIPT, null)
                    }
                }

                if (url != null) {
                    loadUrl(url)
                } else if (htmlContent != null) {
                    loadDataWithBaseURL(null, htmlContent, "text/html", "utf-8", null)
                }
                lastSignature = currentSignature
            }
        },
        update = { webView ->
            // Reload only when signature changes
            if (lastSignature != currentSignature) {
                if (url != null) {
                    if (webView.url != url) webView.loadUrl(url)
                } else if (htmlContent != null) {
                    webView.loadDataWithBaseURL(null, htmlContent, "text/html", "utf-8", null)
                }
                lastSignature = currentSignature
            }
        }
    )
}
