package com.slax.reader.utils

import android.annotation.SuppressLint
import android.graphics.Color
import android.webkit.JavascriptInterface
import android.webkit.WebChromeClient
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.browser.customtabs.CustomTabsIntent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.net.toUri
import app.slax.reader.SlaxConfig
import com.slax.reader.const.HEIGHT_MONITOR_SCRIPT
import com.slax.reader.const.JS_BRIDGE_NAME
import com.slax.reader.model.BridgeMessageParser
import com.slax.reader.model.HeightMessage

private class JsBridge(
    private val onHeightChange: ((Double) -> Unit)?,
) {
    @JavascriptInterface
    fun postMessage(message: String) {
        val bridgeMessage = BridgeMessageParser.parse(message) ?: return

        when (bridgeMessage) {
            is HeightMessage -> onHeightChange?.invoke(bridgeMessage.height)
        }
    }
}

@SuppressLint("SetJavaScriptEnabled", "JavascriptInterface", "ClickableViewAccessibility")
@Composable
actual fun AppWebView(
    url: String?,
    htmlContent: String?,
    modifier: Modifier,
    onHeightChange: ((Double) -> Unit)?,
    onTap: (() -> Unit)?,
    webViewStartY: Double,
    onWebViewPositioned: (((Double) -> Unit) -> Unit)?,
) {

    val onTapCallback = remember(onTap) { onTap }

    val jsBridge = remember(onHeightChange) {
        JsBridge(onHeightChange)
    }

    AndroidView(
        modifier = modifier,
        factory = { context ->
            WebView(context).apply {
                setBackgroundColor(Color.TRANSPARENT)
                isVerticalScrollBarEnabled = false
                isHorizontalScrollBarEnabled = false

                // 启用调试功能（开发环境）
                WebView.setWebContentsDebuggingEnabled(SlaxConfig.BUILD_ENV == "dev")

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

@Composable
actual fun WebView(
    url: String?,
    htmlContent: String?,
    modifier: Modifier,
    onScroll: ((x: Double, y: Double) -> Unit)?
) {
    AndroidView(
        modifier = modifier,
        factory = { context ->
            WebView(context).apply {

                setBackgroundColor(Color.TRANSPARENT)

                // 启用调试功能（开发环境）
                WebView.setWebContentsDebuggingEnabled(SlaxConfig.BUILD_ENV == "dev")

                settings.apply {
                    javaScriptEnabled = true
                    domStorageEnabled = true
                    builtInZoomControls = false
                    displayZoomControls = false
                    allowFileAccess = false
                    allowContentAccess = false
                    cacheMode = WebSettings.LOAD_DEFAULT
                }

                // 添加滚动监听器
                setOnScrollChangeListener { _, scrollX, scrollY, _, _ ->
                    onScroll?.invoke(scrollX.toDouble(), scrollY.toDouble())
                }

                webChromeClient = WebChromeClient()
                webViewClient = WebViewClient()

                when {
                    url != null -> loadUrl(url)
                    htmlContent != null -> loadDataWithBaseURL(
                        null,
                        htmlContent,
                        "text/html",
                        "utf-8",
                        null
                    )
                }
            }
        },
        update = { view ->
            view.apply {
                when {
                    url != null -> loadUrl(url)
                    htmlContent != null -> loadDataWithBaseURL(
                        null,
                        htmlContent,
                        "text/html",
                        "utf-8",
                        null
                    )
                }
            }
        }
    )
}
