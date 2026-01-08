package com.slax.reader.utils

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.statusBars
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.uikit.LocalUIViewController
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.viewinterop.UIKitInteropInteractionMode
import androidx.compose.ui.viewinterop.UIKitInteropProperties
import androidx.compose.ui.viewinterop.UIKitView
import app.slax.reader.SlaxConfig
import com.slax.reader.const.JS_BRIDGE_NAME
import com.slax.reader.data.preferences.AppPreferences
import com.slax.reader.ui.bookmark.WebViewMessage
import kotlinx.cinterop.*
import kotlinx.serialization.json.Json
import org.koin.compose.koinInject
import platform.CoreGraphics.CGPointMake
import platform.CoreGraphics.CGRectMake
import platform.Foundation.*
import platform.SafariServices.SFSafariViewController
import platform.UIKit.*
import platform.WebKit.*
import platform.darwin.NSObject
import platform.darwin.dispatch_after
import platform.darwin.dispatch_get_main_queue
import platform.darwin.dispatch_time
import platform.darwin.sel_registerName
import kotlin.math.abs
import kotlin.math.max
import kotlin.time.ExperimentalTime

private class TapHandler(
    private val onTap: () -> Unit
) : NSObject() {
    @OptIn(BetaInteropApi::class)
    @ObjCAction
    fun handleTap() {
        onTap()
    }
}

private class SingleTapGestureDelegate : NSObject(), UIGestureRecognizerDelegateProtocol {
    override fun gestureRecognizer(
        gestureRecognizer: UIGestureRecognizer,
        shouldRecognizeSimultaneouslyWithGestureRecognizer: UIGestureRecognizer
    ): Boolean {
        return true
    }

    @OptIn(ExperimentalTime::class)
    override fun gestureRecognizer(
        gestureRecognizer: UIGestureRecognizer,
        shouldReceiveTouch: UITouch
    ): Boolean {
        return shouldReceiveTouch.tapCount == 1uL
    }
}

private class ScriptMessageHandler(
    private val onMessage: (String) -> Unit
) : NSObject(), WKScriptMessageHandlerProtocol {
    override fun userContentController(
        userContentController: WKUserContentController,
        didReceiveScriptMessage: WKScriptMessage
    ) {
        val body = didReceiveScriptMessage.body as? String
        if (body != null) {
            onMessage(body)
        }
    }
}

@OptIn(ExperimentalForeignApi::class)
@Composable
actual fun AppWebView(
    htmlContent: String,
    modifier: Modifier,
    webState: AppWebViewState
) {
    val tapHandler = remember {
        TapHandler { webState.dispatchEvent(WebViewEvent.Tap) }
    }
    val singleTapGestureDelegate = remember { SingleTapGestureDelegate() }

    val scriptMessageHandler = remember {
        ScriptMessageHandler { message ->
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

                        "refreshContent" -> {
                            webState.dispatchEvent(
                                WebViewEvent.refreshContent
                            )
                        }

                        "feedback" -> {
                            webState.dispatchEvent(
                                WebViewEvent.feedback
                            )
                        }
                    }
                }
        }
    }

    val density = LocalDensity.current
    val densityScale = density.density
    val densityScaleState = rememberUpdatedState(densityScale)

    // 获取 statusBarsPadding 高度
    val windowInsets = WindowInsets.statusBars
    val statusBarHeightPx = windowInsets.getTop(density).toFloat()

    // iOS contentInset 需要完整高度：Column + statusBarsPadding + 视觉间距
    val totalInsetPx = webState.topContentInsetPx + statusBarHeightPx + 16f * density.density

    var externalUrl by remember { mutableStateOf<String?>(null) }
    val appPreference: AppPreferences = koinInject()
    var doNotAlert by remember { mutableStateOf<Boolean?>(null) }

    val topInsetPoints by remember(totalInsetPx, densityScale) {
        derivedStateOf { (totalInsetPx / densityScale).coerceAtLeast(0f) }
    }
    var observedScrollView by remember { mutableStateOf<UIScrollView?>(null) }
    val scrollObserver = remember {
        KVOObserver { keyPath, newValue, _ ->
            val scrollView = observedScrollView
            if (keyPath == "contentOffset" && scrollView != null) {
                val offsetPoints =
                    (newValue as? NSValue)?.CGPointValue()?.useContents { y }?.toFloat() ?: 0f
                val insetPoints = scrollView.contentInset.useContents { top }.toFloat()
                // contentInset 会让初始 contentOffset.y = -contentInset.top
                // 加上当前 contentInset.top 得到实际滚动距离
                val adjustedOffsetPoints = offsetPoints + insetPoints
                val offsetPx = max(adjustedOffsetPoints * densityScaleState.value, 0f)

                // 获取内容高度和可视高度
                val contentHeightPoints = scrollView.contentSize.useContents { height }.toFloat()
                val contentHeightPx = contentHeightPoints * densityScaleState.value

                val visibleHeightPoints = scrollView.bounds.useContents { size.height }.toFloat()
                val visibleHeightPx = visibleHeightPoints * densityScaleState.value

                webState.dispatchEvent(WebViewEvent.ScrollChange(offsetPx, contentHeightPx, visibleHeightPx))
            }
        }
    }

    val navigationDelegate = remember {
        object : NSObject(), WKNavigationDelegateProtocol {

            // webview被杀后尝试重载
            override fun webViewWebContentProcessDidTerminate(
                webView: WKWebView
            ) {
                webView.loadHTMLString(htmlContent, baseURL = null)
            }

            override fun webView(webView: WKWebView, didFinishNavigation: WKNavigation?) {
                // 移除导致双击上下翻页的双击手势识别器
                webView.scrollView.subviews.forEach { subview ->
                    (subview as UIView).gestureRecognizers?.forEach {
                        if (it is UITapGestureRecognizer && it.numberOfTapsRequired == 2UL && it.numberOfTouchesRequired == 1UL) {
                            subview.removeGestureRecognizer(it)
                        }
                    }
                }
            }

            override fun webView(
                webView: WKWebView,
                decidePolicyForNavigationAction: WKNavigationAction,
                decisionHandler: (WKNavigationActionPolicy) -> Unit
            ) {
                val navigationType = decidePolicyForNavigationAction.navigationType
                val requestUrl = decidePolicyForNavigationAction.request.URL?.absoluteString

                if (navigationType == WKNavigationTypeLinkActivated && requestUrl != null) {
                    if (requestUrl.startsWith("http://") || requestUrl.startsWith("https://")) {
                        externalUrl = requestUrl
                    }
                    decisionHandler(WKNavigationActionPolicy.WKNavigationActionPolicyCancel)
                    return
                }
                decisionHandler(WKNavigationActionPolicy.WKNavigationActionPolicyAllow)
                return
            }
        }
    }

    DisposableEffect(scrollObserver, observedScrollView) {
        val scrollView = observedScrollView
        if (scrollView != null) {
            scrollObserver.observe("contentOffset", scrollView)
            onDispose {
                scrollObserver.unobserve("contentOffset", scrollView)
            }
        } else {
            onDispose { }
        }
    }

    UIKitView(
        modifier = modifier,
        factory = {
            val config = WKWebViewConfiguration().apply {
                preferences = WKPreferences().apply {
                    javaScriptEnabled = true
                }

                val userContentController = WKUserContentController()
                userContentController.addScriptMessageHandler(
                    scriptMessageHandler = scriptMessageHandler,
                    name = JS_BRIDGE_NAME
                )
                this.userContentController = userContentController
            }

            val view = WKWebView(frame = CGRectMake(0.0, 0.0, 0.0, 0.0), configuration = config)

            webState.webView = view

            if (available("16.4")) {
                view.inspectable = SlaxConfig.BUILD_ENV == "dev"
            }

            view.navigationDelegate = navigationDelegate

            // 添加点击手势
            val singleTapGesture = UITapGestureRecognizer(
                target = tapHandler,
                action = NSSelectorFromString(tapHandler::handleTap.name)
            )
            singleTapGesture.delegate = singleTapGestureDelegate
            singleTapGesture.cancelsTouchesInView = false
            view.addGestureRecognizer(singleTapGesture)

            // 配置滚动视图
            view.scrollView.apply {
                contentInsetAdjustmentBehavior =
                    UIScrollViewContentInsetAdjustmentBehavior.UIScrollViewContentInsetAdjustmentNever
                showsHorizontalScrollIndicator = false
                showsVerticalScrollIndicator = false
                alwaysBounceHorizontal = false
                bounces = true
                alwaysBounceVertical = true
                scrollEnabled = true

                // 性能优化
                delaysContentTouches = false
                canCancelContentTouches = true
                decelerationRate = UIScrollViewDecelerationRateNormal

                // 设置 contentInset
                contentInset = UIEdgeInsetsMake(
                    top = topInsetPoints.toDouble(),
                    left = 0.0,
                    bottom = 0.0,
                    right = 0.0
                )
            }

            if (observedScrollView !== view.scrollView) {
                observedScrollView = view.scrollView
            }

            val color = Color(0xFFFCFCFC).toUIColor()
            view.backgroundColor = color
            view.opaque = false
            view.loadHTMLString(htmlContent, baseURL = null)

            view as UIView
        },
        update = { uiView ->
            val webView = uiView as WKWebView

            webState.webView = webView

            val scrollView = webView.scrollView
            val currentInsetPoints = scrollView.contentInset.useContents { top }.toFloat()
            if (abs(currentInsetPoints - topInsetPoints) > 0.1f) {
                // 保存当前的实际滚动位置（调整后的）
                val currentOffsetPoints = scrollView.contentOffset.useContents { y }.toFloat()
                val actualScrollPosition = currentOffsetPoints + currentInsetPoints

                // 更新 contentInset
                scrollView.contentInset = UIEdgeInsetsMake(
                    top = topInsetPoints.toDouble(),
                    left = 0.0,
                    bottom = 0.0,
                    right = 0.0
                )

                // 恢复实际滚动位置，避免跳动
                // 新的 contentOffset = 实际位置 - 新的 contentInset.top
                val newOffsetY = actualScrollPosition - topInsetPoints
                scrollView.setContentOffset(
                    contentOffset = CGPointMake(0.0, newOffsetY.toDouble()),
                    animated = false
                )
            }

            if (observedScrollView !== scrollView) {
                observedScrollView = scrollView
            }
        },
        properties = UIKitInteropProperties(
            interactionMode = UIKitInteropInteractionMode.Cooperative()
        )
    )

    LaunchedEffect(webState) {
        webState.commands.collect { cmd ->
            when (cmd) {
                is WebViewCommand.EvaluateJs -> {
                    webState.webView?.evaluateJavaScript(cmd.script) { result, error ->
                        if (error != null) {
                            println("[iOS WebView] JS 执行失败: ${error.localizedDescription}")
                        }
                        cmd.callback?.invoke(result?.toString() ?: "")
                    }
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

private fun getTopViewController(
    base: UIViewController? = UIApplication.sharedApplication.keyWindow?.rootViewController
): UIViewController? {

    if (base is UINavigationController) {
        return getTopViewController(base.visibleViewController)
    }

    if (base is UITabBarController) {
        return getTopViewController(base.selectedViewController)
    }

    if (base?.presentedViewController != null) {
        return getTopViewController(base.presentedViewController)
    }

    return base
}

@OptIn(ExperimentalForeignApi::class, BetaInteropApi::class)
@Composable
fun OpenInBrowserTab(
    url: String,
    doNotAlert: Boolean,
    onDismiss: () -> Unit,
    onDoNotAlert: () -> Unit,
) {
    val viewController = LocalUIViewController.current

    fun openInBrowserTab() {
        val safariVC = SFSafariViewController(uRL = NSURL(string = url))
        viewController.presentViewController(safariVC, animated = true, completion = null)
    }

    if (doNotAlert) {
        onDismiss()
        openInBrowserTab()
        return
    }

    val topViewController = getTopViewController() ?: return

    val alertController = UIAlertController.alertControllerWithTitle(
        title = "external_link_alert_title".i18n(),
        message = "${"external_link_alert_message".i18n()}$url",
        preferredStyle = UIAlertControllerStyleAlert
    )

    val contentVC = UIViewController()
    val buttonHeight = 40.0

    contentVC.setPreferredContentSize(platform.CoreGraphics.CGSizeMake(0.0, buttonHeight))


    val checkboxButton = UIButton.buttonWithType(UIButtonTypeCustom)
    checkboxButton.translatesAutoresizingMaskIntoConstraints = false
    checkboxButton.setTitle("external_link_do_not_alert".i18n(), forState = UIControlStateNormal)
    checkboxButton.setTitleColor(UIColor.darkGrayColor, forState = UIControlStateNormal)
    checkboxButton.titleLabel?.font = UIFont.systemFontOfSize(14.0)

    val uncheckedIcon = UIImage.systemImageNamed("square")
    val checkedIcon = UIImage.systemImageNamed("checkmark.square.fill")
    checkboxButton.setImage(uncheckedIcon, forState = UIControlStateNormal)

    val checkboxHandler = object : NSObject() {
        var isChecked = false

        @ObjCAction
        fun toggle() {
            isChecked = !isChecked
            val icon = if (isChecked) checkedIcon else uncheckedIcon
            checkboxButton.setImage(icon, forState = UIControlStateNormal)
            val feedback = UIImpactFeedbackGenerator(style = UIImpactFeedbackStyle.UIImpactFeedbackStyleLight)
            feedback.impactOccurred()
        }
    }

    checkboxButton.addTarget(
        target = checkboxHandler,
        action = sel_registerName("toggle"),
        forControlEvents = UIControlEventTouchUpInside
    )

    contentVC.view.addSubview(checkboxButton)
    NSLayoutConstraint.activateConstraints(
        listOf(
            checkboxButton.centerXAnchor.constraintEqualToAnchor(contentVC.view.centerXAnchor),
            checkboxButton.centerYAnchor.constraintEqualToAnchor(contentVC.view.centerYAnchor),
            checkboxButton.heightAnchor.constraintEqualToConstant(buttonHeight)
        )
    )

    alertController.setValue(contentVC, forKey = "contentViewController")

    val confirmAction = UIAlertAction.actionWithTitle(
        title = "btn_ok".i18n(),
        style = UIAlertActionStyleDefault
    ) { _ ->
        openInBrowserTab()
        if (checkboxHandler.isChecked) {
            onDoNotAlert()
        }

        onDismiss()
    }

    val cancelAction = UIAlertAction.actionWithTitle(
        title = "btn_cancel".i18n(),
        style = UIAlertActionStyleCancel
    ) { _ ->
        onDismiss()
    }

    alertController.addAction(confirmAction)
    alertController.addAction(cancelAction)

    topViewController.presentViewController(alertController, animated = true, completion = null)
}

@OptIn(ExperimentalForeignApi::class)
val UIEdgeInsets_zero: CValue<UIEdgeInsets>
    get() = UIEdgeInsetsMake(0.0, 0.0, 0.0, 0.0)

@OptIn(ExperimentalForeignApi::class)
val PaddingValues.toUIEdgeInsets: CValue<UIEdgeInsets>
    get() = UIEdgeInsetsMake(
        calculateTopPadding().value.toDouble(),
        calculateLeftPadding(LayoutDirection.Ltr).value.toDouble(),
        calculateBottomPadding().value.toDouble(),
        calculateRightPadding(LayoutDirection.Ltr).value.toDouble(),
    )

@OptIn(ExperimentalForeignApi::class)
@Composable
actual fun WebView(
    url: String,
    modifier: Modifier,
    webState: AppWebViewState,
    contentInsets: PaddingValues?,
) {
    val scriptMessageHandler = remember {
        ScriptMessageHandler { message ->
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

                        "purchase" -> {
                            webState.dispatchEvent(
                                WebViewEvent.Purchase(msg.productId!!, msg.orderId!!)
                            )
                        }

                        "purchaseWithOffer" -> {
                            webState.dispatchEvent(
                                WebViewEvent.PurchaseWithOffer(
                                    msg.productId!!,
                                    msg.orderId!!,
                                    IAPProductOffer(
                                        offerId = msg.offerId!!,
                                        signature = msg.signature!!,
                                        keyID = msg.keyID!!,
                                        nonce = msg.nonce!!,
                                        timestamp = msg.timestamp!!
                                    ),
                                )
                            )
                        }
                    }
                }
        }
    }

    val scrollDelegate = remember(webState) {
        object : NSObject(), UIScrollViewDelegateProtocol {
            override fun scrollViewDidScroll(scrollView: UIScrollView) {
                val contentOffset = scrollView.contentOffset
                val scrollX = contentOffset.useContents { x }
                val scrollY = contentOffset.useContents { y }
                val contentHeight = scrollView.contentSize.useContents { height }
                val visibleHeight = scrollView.bounds.useContents { size.height }
                webState.dispatchEvent(
                    WebViewEvent.Scroll(scrollX, scrollY, contentHeight, visibleHeight)
                )
            }
        }
    }

    val navigationDelegate = remember(webState) {
        object : NSObject(), WKNavigationDelegateProtocol {
            override fun webView(webView: WKWebView, didFinishNavigation: WKNavigation?) {
                webState.dispatchEvent(WebViewEvent.PageLoaded)
                dispatch_after(
                    dispatch_time(platform.darwin.DISPATCH_TIME_NOW, 300_000_000L),
                    dispatch_get_main_queue()
                ) {
                    val scrollView = webView.scrollView
                    val contentOffset = scrollView.contentOffset
                    val scrollX = contentOffset.useContents { x }
                    val scrollY = contentOffset.useContents { y }
                    val contentHeight = scrollView.contentSize.useContents { height }
                    val visibleHeight = scrollView.bounds.useContents { size.height }
                    webState.dispatchEvent(
                        WebViewEvent.Scroll(scrollX, scrollY, contentHeight, visibleHeight)
                    )
                }
            }
        }
    }

    UIKitView(
        modifier = modifier,
        factory = {
            val config = WKWebViewConfiguration().apply {
                preferences = WKPreferences().apply {
                    javaScriptEnabled = true
                }
                val userContentController = WKUserContentController()
                userContentController.addScriptMessageHandler(
                    scriptMessageHandler = scriptMessageHandler,
                    name = JS_BRIDGE_NAME
                )
                this.userContentController = userContentController
            }

            WKWebView(
                frame = CGRectMake(0.0, 0.0, 0.0, 0.0),
                configuration = config,
            ).apply {
                webState.webView = this
                if (available("16.4")) {
                    inspectable = SlaxConfig.BUILD_ENV == "dev"
                }
                backgroundColor = Color(0xFFFCFCFC).toUIColor()

                scrollView.contentInsetAdjustmentBehavior =
                    UIScrollViewContentInsetAdjustmentBehavior.UIScrollViewContentInsetAdjustmentNever
                scrollView.alwaysBounceVertical = true
                scrollView.alwaysBounceHorizontal = false
                scrollView.delegate = scrollDelegate

                scrollView.contentInset = contentInsets?.toUIEdgeInsets ?: UIEdgeInsets_zero

                this.navigationDelegate = navigationDelegate

                val request = NSURLRequest(uRL = NSURL(string = url))
                if (webState.initialCookies != null && webState.initialCookies.isNotEmpty()) {
                    val cookieStore = configuration.websiteDataStore.httpCookieStore
                    var cookiesSet = 0
                    val totalCookies = webState.initialCookies.size

                    webState.initialCookies.forEach { cookie ->
                        val properties = mutableMapOf<Any?, Any?>(
                            NSHTTPCookieName to cookie.name,
                            NSHTTPCookieValue to cookie.value,
                            NSHTTPCookieDomain to cookie.domain,
                            NSHTTPCookiePath to cookie.path
                        )

                        if (cookie.secure) {
                            properties[NSHTTPCookieSecure] = "TRUE"
                        }

                        cookie.expiresDate?.let {
                            val timeInterval = it / 1000.0
                            properties[NSHTTPCookieExpires] = NSDate.dateWithTimeIntervalSince1970(timeInterval)
                        }

                        NSHTTPCookie.cookieWithProperties(properties)?.let { nsCookie ->
                            cookieStore.setCookie(nsCookie) {
                                cookiesSet++
                                if (cookiesSet == totalCookies) {
                                    loadRequest(request)
                                }
                            }
                        }
                    }
                } else {
                    loadRequest(request)
                }
            }
        },
        update = { view ->
            view.apply {
                scrollView.contentInset = contentInsets?.toUIEdgeInsets ?: UIEdgeInsets_zero
            }
        }
    )
}

@Composable
actual fun OpenInBrowser(url: String) {
    val viewController = LocalUIViewController.current
    val safariVC = SFSafariViewController(uRL = NSURL(string = url))
    viewController.presentViewController(safariVC, animated = true, completion = null)
}
