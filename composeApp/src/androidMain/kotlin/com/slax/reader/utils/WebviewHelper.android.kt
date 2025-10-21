package com.slax.reader.utils

import android.annotation.SuppressLint
import android.graphics.Color
import android.webkit.*
import androidx.browser.customtabs.CustomTabsIntent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.net.toUri
import com.slax.reader.const.HEIGHT_MONITOR_SCRIPT
import com.slax.reader.const.JS_BRIDGE_NAME

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
    modifier: Modifier,
    onHeightChange: ((Double) -> Unit)?,
    onTap: (() -> Unit)?,
) {

    val onTapCallback = remember(onTap) { onTap }
    val onHeightChangeCallback = remember(onHeightChange) { onHeightChange }

    val jsBridge = remember(onHeightChange) {
        JsBridge { msg ->
            try {
                val heightValue = Regex("\"height\":\\s*([0-9.]+)")
                    .find(msg)?.groupValues?.getOrNull(1)?.toDouble()
                if (heightValue != null) onHeightChangeCallback?.invoke(heightValue)
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

                setOnTouchListener { _, event ->
                    when (event.action) {
                        android.view.MotionEvent.ACTION_UP -> {
                            onTapCallback?.invoke()
                        }
                    }
                    false
                }

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
            }
        },
        update = { webView ->
            when {
                url != null -> {
                    webView.loadUrl(url)
                }

                htmlContent != null -> {
                    webView.loadDataWithBaseURL(
                        null,
                        htmlContent,
                        "text/html",
                        "UTF-8",
                        null
                    )
                }
            }
        }
    )
}

@Composable
actual fun OpenInBrowserTab(url: String) {
    val ctx = LocalContext.current
    val builder = CustomTabsIntent.Builder()
    val customTabsIntent = builder.build()
    customTabsIntent.launchUrl(ctx, url.toUri())
}