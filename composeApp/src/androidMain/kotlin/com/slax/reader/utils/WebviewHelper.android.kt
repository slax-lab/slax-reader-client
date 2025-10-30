package com.slax.reader.utils

import android.annotation.SuppressLint
import android.graphics.Color
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
    println("[watch][UI] recomposition AppWebView")

    val onTapCallback = remember(onTap) { onTap }

    AndroidView(
        modifier = modifier,
        factory = { context ->
            object : WebView(context) {
                override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
                    val newHeightMeasureSpec = MeasureSpec.makeMeasureSpec(
                        0,
                        MeasureSpec.UNSPECIFIED
                    )
                    super.onMeasure(widthMeasureSpec, newHeightMeasureSpec)
                }

                override fun onScrollChanged(l: Int, t: Int, oldl: Int, oldt: Int) {
                    scrollTo(0, 0)
                }
            }.apply {
                setBackgroundColor(Color.TRANSPARENT)

                // 启用硬件加速以提升滚动性能
                setLayerType(android.view.View.LAYER_TYPE_HARDWARE, null)

                // 禁用嵌套滚动支持
                isNestedScrollingEnabled = false

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
