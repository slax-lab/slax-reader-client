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
import com.slax.reader.const.INJECTED_SCRIPT
import com.slax.reader.const.JS_BRIDGE_NAME
import com.slax.reader.data.preferences.AppPreferences
import kotlinx.cinterop.*
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

/**
 * 生成iOS端自定义UserAgent
 * 格式: SlaxReader/版本号 Build/构建号 CFNetwork/版本 Darwin/版本
 *
 * @return 自定义UserAgent字符串
 */
@OptIn(ExperimentalForeignApi::class)
private fun generateIOSUserAgent(): String {
    val darwinVersion = getDarwinVersion()
    val cfNetworkVersion = getCFNetworkVersion()

    return "SlaxReader/${SlaxConfig.APP_VERSION_NAME} " +
            "Build/${SlaxConfig.APP_VERSION_CODE} " +
            "CFNetwork/$cfNetworkVersion " +
            "Darwin/$darwinVersion"
}

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
@Suppress("UNUSED_PARAMETER")
actual fun AppWebView(
    htmlContent: String,
    modifier: Modifier,
    webState: AppWebViewState
) {

    val tapHandler = remember(onTap) {
        TapHandler { onTap?.invoke() }
    }
    val singleTapGestureDelegate = remember { SingleTapGestureDelegate() }
    val onJsMessageCallback = rememberUpdatedState(onJsMessage)

    val scriptMessageHandler = remember(onJsMessageCallback.value) {
        if (onJsMessageCallback.value != null) {
            ScriptMessageHandler { message ->
                onJsMessageCallback.value?.invoke(message)
            }
        } else null
    }

    // 创建URL Scheme Handler
    val urlSchemeHandler = remember { IOSWebViewURLSchemeHandler() }

    val density = LocalDensity.current
    val densityScale = density.density
    val densityScaleState = rememberUpdatedState(densityScale)

    // 获取 statusBarsPadding 高度
    val windowInsets = WindowInsets.statusBars
    val statusBarHeightPx = windowInsets.getTop(density).toFloat()

    // iOS contentInset 需要完整高度：Column + statusBarsPadding + 视觉间距
    val totalInsetPx = topContentInsetPx + statusBarHeightPx + 16f * density.density

    var externalUrl by remember { mutableStateOf<String?>(null) }
    val appPreference: AppPreferences = koinInject()
    var doNotAlert by remember { mutableStateOf<Boolean?>(null) }

    val topInsetPoints by remember(totalInsetPx, densityScale) {
        derivedStateOf { (totalInsetPx / densityScale).coerceAtLeast(0f) }
    }
    val onScrollChangeState = rememberUpdatedState(onScrollChange)
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

                onScrollChangeState.value?.invoke(offsetPx, contentHeightPx, visibleHeightPx)
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

    // 缓存 WKWebView 引用
    var webViewRef by remember { mutableStateOf<WKWebView?>(null) }

    // 监听 JS 命令变化并执行
    LaunchedEffect(evaluateJsCommand) {
        if (evaluateJsCommand != null && webViewRef != null) {
            webViewRef?.evaluateJavaScript(evaluateJsCommand) { result, error ->
                if (error != null) {
                    println("[iOS WebView] JS 执行失败: ${error.localizedDescription}")
                } else {
                    println("[iOS WebView] JS 执行结果: $result")
                }
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

                // 注册自定义URL Scheme Handler（用于处理 appassets://local/ 请求）
                // 注意：iOS不允许拦截原生的https scheme，因此必须使用自定义scheme
                setURLSchemeHandler(urlSchemeHandler, forURLScheme = "appassets")

                // 配置 JS Bridge 消息处理器
                if (scriptMessageHandler != null) {
                    val userContentController = WKUserContentController()

                    // 添加消息处理器
                    userContentController.addScriptMessageHandler(
                        scriptMessageHandler = scriptMessageHandler,
                        name = JS_BRIDGE_NAME
                    )

                    // 注意：移除了WKUserScript注入，因为JS现在通过HTML模板中的<script src>标签加载

                    this.userContentController = userContentController
                }
            }

            val view = WKWebView(frame = CGRectMake(0.0, 0.0, 0.0, 0.0), configuration = config)

            // 保存引用
            webViewRef = view

            if (available("16.4")) {
                view.inspectable = SlaxConfig.BUILD_ENV == "dev"
            }

            // 设置自定义 UserAgent
            view.customUserAgent = generateIOSUserAgent()
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

                tintColor = Color(0x33ffd999).toUIColor()
            }

            if (observedScrollView !== view.scrollView) {
                observedScrollView = view.scrollView
            }

            val color = Color(0xFFFCFCFC).toUIColor()
            view.backgroundColor = color
            view.opaque = false

            // 使用平台特定的自定义域名作为baseURL加载HTML
            // iOS: appassets://local，Android: https://appassets.local
            view.loadHTMLString(
                htmlContent,
                baseURL = NSURL(string = WebViewAssets.ASSET_DOMAIN + "/")
            )

            view as UIView
        },
        update = { uiView ->
            val webView = uiView as WKWebView

            // 更新引用
            webViewRef = webView

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
        title = "你即将跳转到第三方页面",
        message = "是否确认在浏览器中打开此链接？\n$url",
        preferredStyle = UIAlertControllerStyleAlert
    )

    val contentVC = UIViewController()
    val buttonHeight = 40.0

    contentVC.setPreferredContentSize(platform.CoreGraphics.CGSizeMake(0.0, buttonHeight))


    val checkboxButton = UIButton.buttonWithType(UIButtonTypeCustom)
    checkboxButton.translatesAutoresizingMaskIntoConstraints = false
    checkboxButton.setTitle(" 不再提示", forState = UIControlStateNormal)
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
        title = "确定",
        style = UIAlertActionStyleDefault
    ) { _ ->
        openInBrowserTab()
        if (checkboxHandler.isChecked) {
            onDoNotAlert()
        }

        onDismiss()
    }

    val cancelAction = UIAlertAction.actionWithTitle(
        title = "取消",
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
    contentInsets: PaddingValues?,
    onScroll: ((scrollX: Double, scrollY: Double, contentHeight: Double, visibleHeight: Double) -> Unit)?,
    onPageLoaded: (() -> Unit)?,
    injectUser: Boolean,
) {
    val appPreference: AppPreferences = koinInject()
    val webViewRef = remember { mutableStateOf<WKWebView?>(null) }

    val scrollDelegate = remember {
        object : NSObject(), UIScrollViewDelegateProtocol {
            override fun scrollViewDidScroll(scrollView: UIScrollView) {
                val contentOffset = scrollView.contentOffset
                val scrollX = contentOffset.useContents { x }
                val scrollY = contentOffset.useContents { y }
                val contentHeight = scrollView.contentSize.useContents { height }
                val visibleHeight = scrollView.bounds.useContents { size.height }
                onScroll?.invoke(scrollX, scrollY, contentHeight, visibleHeight)
            }
        }
    }

    val navigationDelegate = remember {
        object : NSObject(), WKNavigationDelegateProtocol {
            override fun webView(webView: WKWebView, didFinishNavigation: WKNavigation?) {
                // 通知页面加载完成
                onPageLoaded?.invoke()
                dispatch_after(
                    dispatch_time(platform.darwin.DISPATCH_TIME_NOW, 300_000_000L), // 300ms in nanoseconds
                    dispatch_get_main_queue()
                ) {
                    val scrollView = webView.scrollView
                    val contentOffset = scrollView.contentOffset
                    val scrollX = contentOffset.useContents { x }
                    val scrollY = contentOffset.useContents { y }
                    val contentHeight = scrollView.contentSize.useContents { height }
                    val visibleHeight = scrollView.bounds.useContents { size.height }
                    onScroll?.invoke(scrollX, scrollY, contentHeight, visibleHeight)
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
            }

            WKWebView(
                frame = CGRectMake(0.0, 0.0, 0.0, 0.0),
                configuration = config,
            ).apply {
                if (available("16.4")) {
                    inspectable = SlaxConfig.BUILD_ENV == "dev"
                }
                backgroundColor = Color(0xFFFCFCFC).toUIColor()

                // 设置自定义 UserAgent
                customUserAgent = generateIOSUserAgent()

                scrollView.contentInsetAdjustmentBehavior =
                    UIScrollViewContentInsetAdjustmentBehavior.UIScrollViewContentInsetAdjustmentNever
                scrollView.alwaysBounceVertical = true
                scrollView.alwaysBounceHorizontal = false
                scrollView.delegate = scrollDelegate

                scrollView.contentInset = contentInsets?.toUIEdgeInsets ?: UIEdgeInsets_zero

                this.navigationDelegate = navigationDelegate
                webViewRef.value = this
            }
        },
        update = { view ->
            view.apply {
                scrollView.contentInset = contentInsets?.toUIEdgeInsets ?: UIEdgeInsets_zero
            }
        }
    )

    LaunchedEffect(url, webViewRef, injectUser) {
        val webView = webViewRef.value ?: return@LaunchedEffect

        // 如果需要注入用户Cookie
        if (injectUser) {
            val token = appPreference.getAuthInfoSuspend()
            if (!token.isNullOrEmpty()) {
                val cookieStore = WKWebsiteDataStore.defaultDataStore()?.httpCookieStore

                val cookieProperties = mapOf<Any?, Any?>(
                    NSHTTPCookieName to "token",
                    NSHTTPCookieValue to token,
                    NSHTTPCookieDomain to SlaxConfig.WEB_DOMAIN,
                    NSHTTPCookiePath to "/",
                    NSHTTPCookieSecure to "TRUE"
                )

                val cookie = NSHTTPCookie.cookieWithProperties(cookieProperties)

                if (cookie != null && cookieStore != null) {
                    kotlinx.coroutines.suspendCancellableCoroutine { continuation ->
                        cookieStore.setCookie(cookie) {
                            println("[iOS WebView] Cookie已注入: Domain=${SlaxConfig.WEB_DOMAIN}")
                            continuation.resumeWith(Result.success(Unit))
                        }
                    }
                }
            }
        }

        // Cookie设置完成后再加载URL
        webView.loadRequest(NSURLRequest(uRL = NSURL(string = url)))
    }
}

@Composable
actual fun OpenInBrowser(url: String) {
    val viewController = LocalUIViewController.current
    val safariVC = SFSafariViewController(uRL = NSURL(string = url))
    viewController.presentViewController(safariVC, animated = true, completion = null)
}

/**
 * 获取 Darwin 内核版本
 * 通过 ProcessInfo 获取操作系统版本，映射到 Darwin 版本
 *
 * @return Darwin 版本字符串，如 "22.3.0"
 */
@OptIn(ExperimentalForeignApi::class)
private fun getDarwinVersion(): String {
    return try {
        val osVersion = NSProcessInfo.processInfo.operatingSystemVersion
        val majorVersion = osVersion.useContents { majorVersion }
        val minorVersion = osVersion.useContents { minorVersion }
        val patchVersion = osVersion.useContents { patchVersion }

        // iOS 版本到 Darwin 内核版本的映射
        // iOS 16.x -> Darwin 22.x
        // iOS 17.x -> Darwin 23.x
        // iOS 15.x -> Darwin 21.x
        val darwinMajor = majorVersion + 6

        "$darwinMajor.$minorVersion.$patchVersion"
    } catch (e: Exception) {
        println("[iOS WebView] 获取 Darwin 版本失败: ${e.message}")
        "22.3.0" // 降级默认值（对应 iOS 16.3）
    }
}

/**
 * 动态获取 CFNetwork 版本
 * 优先从系统加载的 CFNetwork Bundle 中读取真实版本号，
 * 避免维护 iOS 版本映射表。
 *
 * @return CFNetwork 版本字符串，如 "1404.0.5"
 */
@OptIn(ExperimentalForeignApi::class)
private fun getCFNetworkVersion(): String {
    return try {
        // 1. 尝试直接获取 CFNetwork Bundle 的信息
        // CFNetwork 的 Bundle Identifier 是 "com.apple.CFNetwork"
        val bundle = NSBundle.bundleWithIdentifier("com.apple.CFNetwork")

        // 2. 读取 CFBundleVersion (即 Build 版本号，通常就是我们需要的如 1404.0.5)
        // 注意：kCFBundleVersionKey 在 Kotlin Native 中可能需要直接用字符串 "CFBundleVersion"
        val version = bundle?.objectForInfoDictionaryKey("CFBundleVersion") as? String

        // 3. 如果获取成功直接返回，否则进入兜底逻辑
        if (!version.isNullOrBlank()) {
            return version
        }

        // -------------------------------------------------
        // 下面是兜底逻辑 (Fallback)
        // 只有在极极端情况（如无法加载 Bundle）才会走到这里
        // -------------------------------------------------
        getFallbackCFNetworkVersion()

    } catch (e: Exception) {
        // 异常兜底
        println("[KMP] Failed to load CFNetwork bundle: ${e.message}")
        "1404.0.5" // 默认返回 iOS 16 左右的版本作为安全值
    }
}

/**
 * 兜底估算逻辑
 * 仅保留最基础的推算，用于 Bundle 读取失败的情况
 */
@OptIn(ExperimentalForeignApi::class)
private fun getFallbackCFNetworkVersion(): String {
    val osVersion = NSProcessInfo.processInfo.operatingSystemVersion
    val major = osVersion.useContents { majorVersion }

    // 简化的估算：iOS 16 对应 1400 左右，每升一级大约 +70~100
    // 不需要太精确，因为走到这里的概率极低
    val baseVersion = 1404 // iOS 16 benchmark
    val estimatedMajor = if (major >= 16) {
        baseVersion + (major - 16) * 90
    } else {
        baseVersion - (16 - major) * 90
    }

    return "$estimatedMajor.0.0"
}
