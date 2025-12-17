package com.slax.reader.utils

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Color
import android.view.MotionEvent
import android.webkit.*
import androidx.browser.customtabs.CustomTabsIntent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.net.toUri
import app.slax.reader.SlaxConfig
import com.slax.reader.const.JS_BRIDGE_NAME
import com.slax.reader.data.preferences.AppPreferences
import com.slax.reader.ui.bookmark.WebViewMessage
import kotlinx.serialization.json.Json
import org.koin.compose.koinInject
import kotlin.math.roundToInt

private fun generateAndroidUserAgent(context: Context, format: String = "Android"): String {
    val androidVersion = android.os.Build.VERSION.RELEASE
    val deviceModel = android.os.Build.MODEL
    val locale = java.util.Locale.getDefault().toString()

    val prefix = if (format == "Linux") "Linux; U; " else ""
    return "com.slax.reader/${SlaxConfig.APP_VERSION_NAME} " +
            "($prefix$format $androidVersion; $locale; $deviceModel; " +
            "Build/${SlaxConfig.APP_VERSION_CODE}; Webkit/0.0.0)"
}

@SuppressLint("SetJavaScriptEnabled", "JavascriptInterface", "ClickableViewAccessibility")
@Composable
actual fun AppWebView(
    htmlContent: String,
    modifier: Modifier,
    webState: AppWebViewState
) {
    println("[watch][UI] recomposition AppWebView")

    var externalUrl by remember { mutableStateOf<String?>(null) }
    val appPreference: AppPreferences = koinInject()
    var doNotAlert by remember { mutableStateOf<Boolean?>(null) }

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
                webState.webView = this
                setBackgroundColor(Color.TRANSPARENT)
                setLayerType(android.view.View.LAYER_TYPE_HARDWARE, null)

                isNestedScrollingEnabled = false

                // 隐藏滚动条
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

                    // 性能优化配置
                    @Suppress("DEPRECATION")
                    setRenderPriority(WebSettings.RenderPriority.HIGH)

                    userAgentString = generateAndroidUserAgent(context, "Android")
                }

                addJavascriptInterface(object {
                    @JavascriptInterface
                    fun postMessage(message: String) {
                        runCatching { Json.decodeFromString<WebViewMessage>(message) }
                            .onSuccess { msg ->
                                when (msg.type) {
                                    "imageClick" -> {
                                        webState.dispatchEvent(
                                            WebViewEvent.ImageClick(msg.src!!, msg.allImages!!)
                                        )
                                    }

                                    "scrollToPosition" -> {
                                        webState.dispatchEvent(
                                            WebViewEvent.ScrollToPosition(msg.percentage ?: 0.0)
                                        )
                                    }
                                }
                            }
                    }
                }, JS_BRIDGE_NAME)

                setOnTouchListener { _, event ->
                    if (event.action == MotionEvent.ACTION_UP) {
                        webState.dispatchEvent(WebViewEvent.Tap)
                    }
                    false
                }

                webChromeClient = WebChromeClient()
                webViewClient = object : WebViewClient() {
                    override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
                        val url = request?.url?.toString() ?: "null"
                        if (url.startsWith("http://") || url.startsWith("https://")) {
                            externalUrl = url
                        }
                        return true
                    }
                }

                loadDataWithBaseURL(null, htmlContent, "text/html", "utf-8", null)
            }
        }
    )

    LaunchedEffect(webState) {
        webState.commands.collect { cmd ->
            when (cmd) {
                is WebViewCommand.EvaluateJs -> {
                    webState.webView?.evaluateJavascript(cmd.script, cmd.callback)
                }
            }
        }
    }

    DisposableEffect(Unit) {
        onDispose { webState.webView = null }
    }

    if (externalUrl != null) {
        if (doNotAlert == null) {
            doNotAlert = getDoNotAlertSetting(appPreference)
        }

        OpenInBrowserTab(
            url = externalUrl!!,
            doNotAlert = doNotAlert!!,
            onDismiss = {
                externalUrl = null
            },
            onDoNotAlert = {
                doNotAlert = true
                setDoNotAlertSetting(appPreference)
            }
        )
    }
}

@Composable
fun OpenInBrowserTab(
    url: String,
    doNotAlert: Boolean,
    onDismiss: () -> Unit,
    onDoNotAlert: () -> Unit,
) {
    val ctx = LocalContext.current
    val isChecked = remember { mutableStateOf(false) }

    fun openInBrowserTab() {
        val builder = CustomTabsIntent.Builder()
        val customTabsIntent = builder.build()
        customTabsIntent.launchUrl(ctx, url.toUri())
    }

    if (doNotAlert) {
        openInBrowserTab()
        onDismiss()
        return
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("你即将跳转到第三方页面") },
        text = {
            Column {
                Text("是否确认在浏览器中打开此链接？\n$url")

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .padding(end = 8.dp)
                ) {
                    Checkbox(
                        checked = isChecked.value,
                        onCheckedChange = { isChecked.value = it }
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "不再提示",
                        fontSize = 14.sp
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (isChecked.value) {
                        onDoNotAlert()
                    }
                    openInBrowserTab()
                    onDismiss()
                }
            ) {
                Text("Confirm")
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss
            ) {
                Text("Dismiss")
            }
        }
    )
}

fun PaddingValues.setOnWebView(webView: WebView) {
    webView.setPadding(
        calculateLeftPadding(LayoutDirection.Ltr).value.roundToInt(),
        calculateTopPadding().value.roundToInt(),
        calculateRightPadding(LayoutDirection.Ltr).value.roundToInt(),
        calculateBottomPadding().value.roundToInt()
    )
}

@SuppressLint("SetJavaScriptEnabled")
@Composable
actual fun WebView(
    url: String,
    modifier: Modifier,
    contentInsets: PaddingValues?,
    onScroll: ((scrollX: Double, scrollY: Double, contentHeight: Double, visibleHeight: Double) -> Unit)?,
    onPageLoaded: (() -> Unit)?,
    injectUser: Boolean,
) {
    val appPreference: AppPreferences = koinInject()
    AndroidView(
        modifier = modifier,
        factory = { context ->
            WebView(context).apply {

                setBackgroundColor(Color.TRANSPARENT)

                WebView.setWebContentsDebuggingEnabled(SlaxConfig.BUILD_ENV == "dev")

                settings.apply {
                    javaScriptEnabled = true
                    domStorageEnabled = true
                    builtInZoomControls = false
                    displayZoomControls = false
                    allowFileAccess = false
                    allowContentAccess = false
                    cacheMode = WebSettings.LOAD_DEFAULT

                    userAgentString = generateAndroidUserAgent(context, "Android")
                }

                setOnScrollChangeListener { view, scrollX, scrollY, _, _ ->
                    val webView = view as? WebView ?: return@setOnScrollChangeListener
                    val contentHeight = (webView.contentHeight * webView.scale).toDouble()
                    val visibleHeight = webView.height.toDouble()
                    onScroll?.invoke(
                        scrollX.toDouble(),
                        scrollY.toDouble(),
                        contentHeight,
                        visibleHeight
                    )
                }

                webChromeClient = WebChromeClient()
                webViewClient = object : WebViewClient() {
                    override fun onPageFinished(view: WebView?, url: String?) {
                        super.onPageFinished(view, url)

                        onPageLoaded?.invoke()
                        view?.postDelayed({
                            view.let {
                                val contentHeight = (it.contentHeight * it.scale).toDouble()
                                val visibleHeight = it.height.toDouble()
                                onScroll?.invoke(
                                    it.scrollX.toDouble(),
                                    it.scrollY.toDouble(),
                                    contentHeight,
                                    visibleHeight
                                )
                            }
                        }, 300)
                    }
                }

                if (injectUser) {
                    val token = kotlinx.coroutines.runBlocking { appPreference.getAuthInfoSuspend() }
                    if (token.isNullOrEmpty()) {
                        println("[Android WebView] Token为空，跳过Cookie注入")
                        return@apply
                    }

                    val cookieManager = CookieManager.getInstance().apply { setAcceptCookie(true) }
                    val supportsThirdPartyCookies =
                        android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP

                    if (supportsThirdPartyCookies) {
                        cookieManager.setAcceptThirdPartyCookies(this, true)
                    }

                    val cookieString =
                        "token=$token; Domain=${SlaxConfig.WEB_DOMAIN}; Path=/; Secure; SameSite=None"
                    cookieManager.setCookie(url, cookieString)

                    if (supportsThirdPartyCookies) {
                        cookieManager.flush()
                    }
                }

                contentInsets?.setOnWebView(this)
                loadUrl(url)
            }
        },
        update = { view ->
            view.apply {
                contentInsets?.setOnWebView(this)
                loadUrl(url)
            }
        }
    )
}

@Composable
actual fun OpenInBrowser(url: String) {
    val ctx = LocalContext.current

    val builder = CustomTabsIntent.Builder()
    val customTabsIntent = builder.build()
    customTabsIntent.launchUrl(ctx, url.toUri())
}