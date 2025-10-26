package com.slax.reader.utils

import android.annotation.SuppressLint
import android.graphics.Color
import android.webkit.JavascriptInterface
import android.webkit.WebChromeClient
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.browser.customtabs.CustomTabsIntent
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.net.toUri
import app.slax.reader.SlaxConfig
import com.slax.reader.model.BridgeMessageParser
import com.slax.reader.model.HeightMessage
import kotlin.math.roundToInt

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
    topContentInsetPx: Float,
    onTap: (() -> Unit)?,
    onScrollChange: ((scrollY: Float) -> Unit)?,
) {
    val onTapCallback = remember(onTap) { onTap }
    val onScrollChangeCallback = remember(onScrollChange) { onScrollChange }

    AndroidView(
        modifier = modifier,
        factory = { context ->
            // 自定义 WebView 以监听滚动
            object : WebView(context) {
                override fun onScrollChanged(l: Int, t: Int, oldl: Int, oldt: Int) {
                    super.onScrollChanged(l, t, oldl, oldt)
                    // Android WebView scrollY 已经是设备像素
                    // 直接用于 graphicsLayer.translationY
                    println("[Android WebView Scroll] scrollY(px)=$t")
                    onScrollChangeCallback?.invoke(t.toFloat())
                }
            }.apply {
                setBackgroundColor(Color.TRANSPARENT)

                // 启用硬件加速以提升滚动性能
                setLayerType(android.view.View.LAYER_TYPE_HARDWARE, null)

                // 启用嵌套滚动支持
                isNestedScrollingEnabled = true

                // 隐藏滚动条
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

                    // 性能优化配置
                    @Suppress("DEPRECATION")
                    setRenderPriority(WebSettings.RenderPriority.HIGH)
                }
                val topPadding = topContentInsetPx.coerceAtLeast(0f).toInt()
                setPadding(paddingLeft, topPadding, paddingRight, paddingBottom)

                setOnTouchListener { _, event ->
                    when (event.action) {
                        android.view.MotionEvent.ACTION_UP -> {
                            onTapCallback?.invoke()
                        }
                    }
                    false
                }

                webChromeClient = WebChromeClient()
                webViewClient = WebViewClient()

                if (url != null) {
                    loadUrl(url)
                } else if (htmlContent != null) {
                    loadDataWithBaseURL(null, htmlContent, "text/html", "utf-8", null)
                }
            }
        },
        update = { webView ->
            val topPadding = topContentInsetPx.coerceAtLeast(0f).toInt()
            if (webView.paddingTop != topPadding) {
                webView.setPadding(
                    webView.paddingLeft,
                    topPadding,
                    webView.paddingRight,
                    webView.paddingBottom
                )
            }
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

fun PaddingValues.setOnWebView(webView: WebView) {
    webView.setPadding(
        calculateLeftPadding(LayoutDirection.Ltr).value.roundToInt(),
        calculateTopPadding().value.roundToInt(),
        calculateRightPadding(LayoutDirection.Ltr).value.roundToInt(),
        calculateBottomPadding().value.roundToInt()
    )
}

@Composable
actual fun WebView(
    url: String?,
    htmlContent: String?,
    modifier: Modifier,
    contentInsets: PaddingValues?,
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

                contentInsets?.setOnWebView(this)
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
                contentInsets?.setOnWebView(this)
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
