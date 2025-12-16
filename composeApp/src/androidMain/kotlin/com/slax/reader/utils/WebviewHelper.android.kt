package com.slax.reader.utils

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Color
import android.webkit.*
import androidx.browser.customtabs.CustomTabsIntent
import androidx.webkit.WebViewCompat
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
import org.koin.compose.koinInject
import kotlin.math.roundToInt

/**
 * 生成Android端自定义UserAgent
 * 格式: com.slax.reader/版本号 (Android 系统版本; 语言; 设备型号; Build/构建号; Webkit/版本)
 *
 * @param context Android Context，用于获取WebView包信息
 * @param format UserAgent格式
 * @return 自定义UserAgent字符串
 */
private fun generateAndroidUserAgent(context: Context, format: String = "Android"): String {
    val androidVersion = android.os.Build.VERSION.RELEASE
    val deviceModel = android.os.Build.MODEL
    val locale = java.util.Locale.getDefault().toString()

    // 使用 WebViewCompat 获取 WebView 内核版本（更好的兼容性）
    val webkitVersion = try {
        val webViewPackage = WebViewCompat.getCurrentWebViewPackage(context)
        webViewPackage?.versionName ?: "0.0.0"
    } catch (e: Exception) {
        println("[Android WebView] 获取 WebKit 版本失败: ${e.message}")
        "0.0.0"
    }

    val prefix = if (format == "Linux") "Linux; U; " else ""
    return "com.slax.reader/${SlaxConfig.APP_VERSION_NAME} " +
            "($prefix$format $androidVersion; $locale; $deviceModel; " +
            "Build/${SlaxConfig.APP_VERSION_CODE}; Webkit/$webkitVersion)"
}


@SuppressLint("SetJavaScriptEnabled", "JavascriptInterface", "ClickableViewAccessibility")
@Composable
actual fun AppWebView(
    htmlContent: String,
    modifier: Modifier,
    topContentInsetPx: Float,
    onTap: (() -> Unit)?,
    onScrollChange: ((scrollY: Float, contentHeight: Float, visibleHeight: Float) -> Unit)?,
    onJsMessage: ((message: String) -> Unit)?,
    evaluateJsCommand: String?,  // 新增：JS 执行命令
) {
    println("[watch][UI] recomposition AppWebView")

    val context = LocalContext.current
    val onTapCallback = remember(onTap) { onTap }
    val onJsMessageCallback = remember(onJsMessage) { onJsMessage }
    var externalUrl by remember { mutableStateOf<String?>(null) }
    val appPreference: AppPreferences = koinInject()
    var doNotAlert by remember { mutableStateOf<Boolean?>(null) }

    // 创建资源加载器
    val assetLoader = remember { AndroidWebViewAssetLoader(context) }

    // 缓存 WebView 引用
    var webViewRef by remember { mutableStateOf<WebView?>(null) }

    // 监听 JS 命令变化并执行
    LaunchedEffect(evaluateJsCommand) {
        if (evaluateJsCommand != null && webViewRef != null) {
            webViewRef?.evaluateJavascript(evaluateJsCommand) { result ->
                println("[Android WebView] JS 执行结果: $result")
            }
        }
    }

    AndroidView(
        modifier = modifier,
        factory = { ctx ->
            object : WebView(ctx) {
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
                // 保存引用
                webViewRef = this

                setBackgroundColor(Color.TRANSPARENT)

                // 启用硬件加速以提升滚动性能
                setLayerType(android.view.View.LAYER_TYPE_HARDWARE, null)

                // 禁用嵌套滚动支持
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

                    // 设置自定义 UserAgent
                    userAgentString = generateAndroidUserAgent(ctx, "Android")
                }

                // 添加 JavaScript Interface 用于接收来自 JS 的消息
                if (onJsMessageCallback != null) {
                    addJavascriptInterface(object {
                        @JavascriptInterface
                        fun postMessage(message: String) {
                            onJsMessageCallback.invoke(message)
                        }
                    }, JS_BRIDGE_NAME)
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
                webViewClient = object : WebViewClient() {
                    override fun shouldInterceptRequest(
                        view: WebView?,
                        request: WebResourceRequest
                    ): WebResourceResponse? {
                        // 拦截资源请求，使用WebViewAssetLoader处理
                        return assetLoader.shouldInterceptRequest(request)
                            ?: super.shouldInterceptRequest(view, request)
                    }

                    override fun onPageFinished(view: WebView?, url: String?) {
                        super.onPageFinished(view, url)
                        println("[Android WebView] 页面加载完成: $url")
                        // 注意：JS代码现在通过HTML模板中的<script src>标签加载
                        // 不再需要evaluateJavascript注入
                    }

                    override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
                        val url = request?.url?.toString() ?: "null"

                        // 允许加载自定义域名的资源
                        if (url.startsWith("https://appassets.local")) {
                            return false
                        }

                        // 拦截外部链接
                        if (url.startsWith("http://") || url.startsWith("https://")) {
                            externalUrl = url
                            return true
                        }

                        return false
                    }
                }

                // 使用自定义域名加载HTML
                loadDataWithBaseURL(
                    "https://appassets.local/",
                    htmlContent,
                    "text/html",
                    "utf-8",
                    null
                )
            }
        }
    )

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

                    // 设置自定义 UserAgent
                    userAgentString = generateAndroidUserAgent(context, "Android")
                }

                // 添加滚动监听器
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
                        }, 300) // 延迟300ms等待页面渲染完成
                    }
                }

                // 如果需要注入用户Cookie
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