package com.slax.reader.utils

import android.annotation.SuppressLint
import android.graphics.Color
import android.view.ActionMode
import android.view.Menu
import android.view.MenuItem
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
import com.slax.reader.const.JS_BRIDGE_NAME
import com.slax.reader.data.preferences.AppPreferences
import com.slax.reader.domain.image.ImageDownloadManager
import com.slax.reader.ui.bookmark.WebViewMessage
import org.koin.compose.koinInject
import kotlin.math.roundToInt

@SuppressLint("SetJavaScriptEnabled", "JavascriptInterface", "ClickableViewAccessibility")
@Composable
actual fun AppWebView(
    htmlContent: String,
    modifier: Modifier,
    webState: AppWebViewState,
    bookmarkId: String
) {
    println("[watch][UI] recomposition AppWebView")

    var externalUrl by remember { mutableStateOf<String?>(null) }
    val appPreference: AppPreferences = koinInject()
    val imageDownloadManager: ImageDownloadManager = koinInject()
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

                /**
                 * 拦截 ActionMode 启动，保留文本选中能力但清空系统菜单项。
                 *
                 * 不能返回 null，否则 WebView 会认为选中操作失败并取消选中。
                 * 通过传入自定义 Callback 包装器，在 onPrepareActionMode 中清空菜单，
                 * 实现"选中有效、菜单为空"的效果。
                 */
                override fun startActionMode(callback: ActionMode.Callback?): ActionMode? {
                    val wrappedCallback = callback?.let { EmptyMenuActionModeCallback(it) }
                    return super.startActionMode(wrappedCallback)
                }

                override fun startActionMode(callback: ActionMode.Callback?, type: Int): ActionMode? {
                    val wrappedCallback = callback?.let { EmptyMenuActionModeCallback(it) }
                    return super.startActionMode(wrappedCallback, type)
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
                }

                var longPressScreenY = 0f
                val gestureDetector = android.view.GestureDetector(context,
                    object : android.view.GestureDetector.SimpleOnGestureListener() {
                        override fun onLongPress(e: MotionEvent) {
                            longPressScreenY = e.rawY
                        }
                    }
                )

                addJavascriptInterface(object {
                    @JavascriptInterface
                    fun postMessage(message: String) {
                        runCatching { bridgeJson.decodeFromString<WebViewMessage>(message) }
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

                                    "refreshContent" -> {
                                        webState.dispatchEvent(WebViewEvent.RefreshContent)
                                    }

                                    "feedback" -> {
                                        webState.dispatchEvent(WebViewEvent.Feedback)
                                    }

                                    "textSelected" -> {
                                        val text = msg.text
                                        if (!text.isNullOrBlank()) {
                                            webState.dispatchEvent(WebViewEvent.TextSelected(text, longPressScreenY))
                                        }
                                    }

                                    "textDeselected" -> {
                                        webState.dispatchEvent(WebViewEvent.TextDeselected)
                                    }

                                    "markClicked" -> {
                                        val markId = msg.markId
                                        val text = msg.text
                                        if (!markId.isNullOrBlank()) {
                                            val markItemInfo = msg.markItemInfo?.let {
                                                runCatching {
                                                    bridgeJson.decodeFromString<BridgeMarkItemInfo>(it)
                                                }.getOrNull()
                                            }
                                            webState.dispatchEvent(
                                                WebViewEvent.MarkClicked(markId, text ?: "", markItemInfo)
                                            )
                                        }
                                    }

                                    "selectionMarkItemInfo" -> {
                                        val markItemInfo = msg.markItemInfo?.let {
                                            runCatching {
                                                bridgeJson.decodeFromString<BridgeMarkItemInfo>(it)
                                            }.getOrNull()
                                        }
                                        webState.dispatchEvent(
                                            WebViewEvent.SelectionMarkItemInfo(markItemInfo)
                                        )
                                    }
                                }
                            }
                    }
                }, JS_BRIDGE_NAME)

                setOnTouchListener { _, event ->
                    gestureDetector.onTouchEvent(event)
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

                    override fun onPageFinished(view: WebView?, url: String?) {
                        super.onPageFinished(view, url)
                        webState.dispatchEvent(WebViewEvent.PageLoaded)
                    }

                    override fun shouldInterceptRequest(
                        view: WebView?,
                        request: WebResourceRequest?
                    ): WebResourceResponse? {
                        val url = request?.url?.toString() ?: return null

                        if (!url.startsWith("slaxstatics://") && !url.startsWith("slaxstatic://")) {
                            return super.shouldInterceptRequest(view, request)
                        }

                        val cached = imageDownloadManager.getCachedData(url, bookmarkId)
                        if (cached != null) {
                            return WebResourceResponse(getMimeTypeFromUrl(url), null, cached.inputStream())
                        }

                        return try {
                            val originalUrl = imageDownloadManager.resolveUrl(url)
                            val connection = java.net.URL(originalUrl).openConnection() as java.net.HttpURLConnection
                            connection.connectTimeout = 15_000
                            connection.readTimeout = 30_000

                            WebResourceResponse(
                                connection.contentType?.substringBefore(";") ?: getMimeTypeFromUrl(url),
                                null,
                                CachingInputStream(connection, url, bookmarkId, imageDownloadManager)
                            )
                        } catch (e: Exception) {
                            println("[SchemeHandler] 代理失败: $url, ${e.message}")
                            null
                        }
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
        title = { Text("external_link_alert_title".i18n()) },
        text = {
            Column {
                Text("${"external_link_alert_message".i18n()}$url")

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
    webState: AppWebViewState,
    contentInsets: PaddingValues?,
) {
    AndroidView(
        modifier = modifier,
        factory = { context ->
            WebView(context).apply {
                webState.webView = this
                setBackgroundColor(Color.TRANSPARENT)
                settings.apply {
                    javaScriptEnabled = true
                    domStorageEnabled = true
                    builtInZoomControls = false
                    displayZoomControls = false
                    allowFileAccess = false
                    allowContentAccess = false
                    cacheMode = WebSettings.LOAD_DEFAULT
                    @Suppress("DEPRECATION")
                    setRenderPriority(WebSettings.RenderPriority.HIGH)
                }

                setOnScrollChangeListener { view, scrollX, scrollY, _, _ ->
                    val webView = view as? WebView ?: return@setOnScrollChangeListener
                    val contentHeight = (webView.contentHeight * webView.scale).toDouble()
                    val visibleHeight = webView.height.toDouble()
                    webState.dispatchEvent(
                        WebViewEvent.Scroll(
                            scrollX.toDouble(),
                            scrollY.toDouble(),
                            contentHeight,
                            visibleHeight
                        )
                    )
                }

                webChromeClient = WebChromeClient()
                webViewClient = object : WebViewClient() {
                    override fun onPageFinished(view: WebView?, url: String?) {
                        super.onPageFinished(view, url)

                        webState.dispatchEvent(WebViewEvent.PageLoaded)
                        view?.postDelayed({
                            view.let {
                                val contentHeight = (it.contentHeight * it.scale).toDouble()
                                val visibleHeight = it.height.toDouble()
                                webState.dispatchEvent(
                                    WebViewEvent.Scroll(
                                        it.scrollX.toDouble(),
                                        it.scrollY.toDouble(),
                                        contentHeight,
                                        visibleHeight
                                    )
                                )
                            }
                        }, 300)
                    }
                }

                contentInsets?.setOnWebView(this)

                webState.initialCookies?.let { cookies ->
                    webState.setCookies(cookies, url)
                }

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

/**
 * ActionMode.Callback 包装器，保留选中行为但清空菜单项。
 *
 * Android WebView 的文本选中流程依赖 ActionMode 正常启动，
 * 如果 startActionMode 返回 null 会导致选中被取消。
 * 此包装器在 onPrepareActionMode 中清空 Menu，
 * 让 ActionMode 生命周期正常运行，但不显示任何系统菜单项。
 */
private class EmptyMenuActionModeCallback(
    private val delegate: ActionMode.Callback
) : ActionMode.Callback {
    override fun onCreateActionMode(mode: ActionMode?, menu: Menu?): Boolean {
        return delegate.onCreateActionMode(mode, menu)
    }

    override fun onPrepareActionMode(mode: ActionMode?, menu: Menu?): Boolean {
        // 清空所有系统菜单项
        menu?.clear()
        return true
    }

    override fun onActionItemClicked(mode: ActionMode?, item: MenuItem?): Boolean {
        return delegate.onActionItemClicked(mode, item)
    }

    override fun onDestroyActionMode(mode: ActionMode?) {
        delegate.onDestroyActionMode(mode)
    }
}

private class CachingInputStream(
    private val connection: java.net.HttpURLConnection,
    private val url: String,
    private val bookmarkId: String,
    private val manager: ImageDownloadManager
) : java.io.InputStream() {
    private val source: java.io.InputStream = connection.inputStream
    private val buffer = java.io.ByteArrayOutputStream()
    private var completed = false

    override fun read(): Int {
        val b = source.read()
        if (b != -1) buffer.write(b)
        else completed = true
        return b
    }

    override fun read(b: ByteArray, off: Int, len: Int): Int {
        val n = source.read(b, off, len)
        if (n > 0) buffer.write(b, off, n)
        else if (n == -1) completed = true
        return n
    }

    override fun available(): Int = source.available()

    override fun close() {
        source.close()
        connection.disconnect()
        if (completed) {
            val data = buffer.toByteArray()
            if (data.isNotEmpty()) {
                try {
                    manager.cacheData(url, bookmarkId, data)
                } catch (e: Exception) {
                    println("[SchemeHandler] 缓存写入失败: $url, ${e.message}")
                }
            }
        }
    }
}
