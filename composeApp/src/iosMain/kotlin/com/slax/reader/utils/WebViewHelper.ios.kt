package com.slax.reader.utils

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.uikit.LocalUIViewController
import androidx.compose.ui.viewinterop.UIKitView
import com.slax.reader.const.HEIGHT_MONITOR_SCRIPT
import com.slax.reader.const.JS_BRIDGE_NAME
import com.slax.reader.model.BridgeMessageParser
import com.slax.reader.model.HeightMessage
import kotlinx.cinterop.*
import platform.CoreGraphics.CGPointMake
import platform.CoreGraphics.CGRectMake
import platform.Foundation.NSSelectorFromString
import platform.Foundation.NSURL
import platform.Foundation.NSURLRequest
import platform.SafariServices.SFSafariViewController
import platform.UIKit.*
import platform.WebKit.*
import platform.darwin.NSObject

private class MessageHandler(
    private val onHeightChange: ((Double) -> Unit)?
) : NSObject(), WKScriptMessageHandlerProtocol {

    override fun userContentController(
        userContentController: WKUserContentController,
        didReceiveScriptMessage: WKScriptMessage
    ) {
        val body = didReceiveScriptMessage.body
        val messageText = body.toString()

        val message = BridgeMessageParser.parse(messageText) ?: return

        when (message) {
            is HeightMessage -> onHeightChange?.invoke(message.height)
        }
    }
}

fun Color.toUIColor(): UIColor {
    val argb = this.toArgb()
    return UIColor(
        red = ((argb shr 16) and 0xFF) / 255.0,
        green = ((argb shr 8) and 0xFF) / 255.0,
        blue = (argb and 0xFF) / 255.0,
        alpha = ((argb shr 24) and 0xFF) / 255.0
    )
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

// 嵌套滚动协调器：监听外层和内层滚动，自动切换
private class NestedScrollCoordinator : NSObject() {
    var webView: WKWebView? = null
    var webViewStartY: Double = 0.0
    var isWebViewScrollEnabled: Boolean = false

    // 添加缓冲区，避免频繁切换
    private val bufferZone = 5.0

    // 根据外层滚动位置判断是否需要启用 WebView 滚动
    @OptIn(ExperimentalForeignApi::class)
    fun onOuterScrollChanged(outerScrollOffset: Double) {
        // 防护：如果 startY 还没初始化，忽略
        if (webViewStartY <= 0.0) {
            println("[Scroll Debug] Ignoring scroll update, startY not initialized yet")
            return
        }

        val web = webView ?: return
        val scrollView = web.scrollView

        // 当外层滚动到 WebView 起始位置时（带缓冲区），启用 WebView 内部滚动
        val shouldEnable = outerScrollOffset >= (webViewStartY - bufferZone)

        if (shouldEnable != isWebViewScrollEnabled) {
            isWebViewScrollEnabled = shouldEnable
            // 先设置状态，避免状态切换冲突
            scrollView.scrollEnabled = shouldEnable
            scrollView.userInteractionEnabled = shouldEnable
            web.userInteractionEnabled = shouldEnable
            println("[Scroll Debug] Switch scrollEnabled=$shouldEnable at outerScroll=$outerScrollOffset, startY=$webViewStartY")
        }
    }
}

private class WebViewScrollDelegate(
    private val coordinator: NestedScrollCoordinator
) : NSObject(), UIScrollViewDelegateProtocol {

    // 添加触发阈值，避免轻微触碰就禁用滚动
    private val topThreshold = -10.0

    @ObjCSignatureOverride
    @OptIn(ExperimentalForeignApi::class)
    override fun scrollViewDidScroll(scrollView: UIScrollView) {
        if (!scrollView.userInteractionEnabled) return

        val offset = scrollView.contentOffset.useContents { y }

        // 只有当向上滚动超过阈值（实际上是向上滚动了一小段）才禁用
        if (offset <= topThreshold) {
            val web = coordinator.webView ?: return
            scrollView.userInteractionEnabled = false
            scrollView.scrollEnabled = false
            scrollView.contentOffset = CGPointMake(0.0, 0.0)
            web.userInteractionEnabled = false
            coordinator.isWebViewScrollEnabled = false
            println("[Scroll Debug] Disabled scroll at top, offset=$offset")
        }
    }

    @ObjCSignatureOverride
    @OptIn(ExperimentalForeignApi::class)
    override fun scrollViewDidEndDragging(scrollView: UIScrollView, willDecelerate: Boolean) {
        // 拖动结束时检查是否到达边界
        if (!scrollView.userInteractionEnabled) return

        val offset = scrollView.contentOffset.useContents { y }
        val contentHeight = scrollView.contentSize.useContents { height }
        val frameHeight = scrollView.frame.useContents { size.height }

        // 到达顶部
        if (offset <= topThreshold) {
            val web = coordinator.webView ?: return
            scrollView.userInteractionEnabled = false
            scrollView.scrollEnabled = false
            web.userInteractionEnabled = false
            coordinator.isWebViewScrollEnabled = false
            println("[Scroll Debug] Disabled at top on drag end, offset=$offset")
        }
        // 到达底部（预留 50px 的缓冲区）
        else if (offset + frameHeight >= contentHeight - 50.0) {
            println("[Scroll Debug] Reached bottom, offset=$offset, contentHeight=$contentHeight, frameHeight=$frameHeight")
            // 到底部不禁用，但记录状态
        }
    }
}

private class TapGestureDelegate : NSObject(), UIGestureRecognizerDelegateProtocol {
    override fun gestureRecognizer(
        gestureRecognizer: UIGestureRecognizer,
        shouldRecognizeSimultaneouslyWithGestureRecognizer: UIGestureRecognizer
    ): Boolean {
        return true
    }

    override fun gestureRecognizer(
        gestureRecognizer: UIGestureRecognizer,
        shouldReceiveTouch: UITouch
    ): Boolean {
        return true
    }
}

@OptIn(ExperimentalForeignApi::class)
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
    val messageHandler = remember(onHeightChange, onTap) {
        MessageHandler(onHeightChange)
    }

    val tapHandler = remember(onTap) {
        TapHandler { onTap?.invoke() }
    }
    val tapGestureDelegate = remember { TapGestureDelegate() }

    val coordinator = remember { NestedScrollCoordinator() }
    val scrollDelegate = remember { WebViewScrollDelegate(coordinator) }

    // 通过 LaunchedEffect 设置 webViewStartY 并立即检查状态
    LaunchedEffect(webViewStartY) {
        if (webViewStartY > 0.0) {
            coordinator.webViewStartY = webViewStartY
            // 立即触发一次检查，确保初始状态正确
            println("[Scroll Debug] Initial check with startY=$webViewStartY")
        }
    }

    // 提供回调给 Compose 层，用于通知滚动位置变化
    LaunchedEffect(Unit) {
        onWebViewPositioned?.invoke { scrollOffset: Double ->
            coordinator.onOuterScrollChanged(scrollOffset)
        }
    }

    UIKitView(
        modifier = modifier,
        factory = {
            val config = WKWebViewConfiguration().apply {
                preferences = WKPreferences().apply {
                    javaScriptEnabled = true
                }
                userContentController = WKUserContentController().apply {
                    addScriptMessageHandler(messageHandler, JS_BRIDGE_NAME)

                    val heightScript = WKUserScript(
                        source = HEIGHT_MONITOR_SCRIPT,
                        injectionTime = WKUserScriptInjectionTime.WKUserScriptInjectionTimeAtDocumentEnd,
                        forMainFrameOnly = true
                    )

                    suppressesIncrementalRendering = false
                    addUserScript(heightScript)
                }
            }

            val view = WKWebView(frame = CGRectMake(0.0, 0.0, 0.0, 0.0), configuration = config)
            coordinator.webView = view

            // 添加点击手势
            val tapGesture = UITapGestureRecognizer(
                target = tapHandler,
                action = NSSelectorFromString("handleTap")
            )
            tapGesture.delegate = tapGestureDelegate
            tapGesture.cancelsTouchesInView = false
            view.addGestureRecognizer(tapGesture)

            // 配置滚动行为
            view.scrollView.delegate = scrollDelegate
            view.scrollView.contentInsetAdjustmentBehavior =
                UIScrollViewContentInsetAdjustmentBehavior.UIScrollViewContentInsetAdjustmentNever
            view.scrollView.showsHorizontalScrollIndicator = false
            view.scrollView.showsVerticalScrollIndicator = false
            view.scrollView.alwaysBounceHorizontal = false
            view.scrollView.bounces = true
            view.scrollView.alwaysBounceVertical = false
            view.scrollView.opaque = false

            // 性能优化：减少滚动延迟
            view.scrollView.delaysContentTouches = false
            view.scrollView.canCancelContentTouches = true
            view.scrollView.decelerationRate = UIScrollViewDecelerationRateNormal

            // 初始状态：禁用滚动
            view.scrollView.scrollEnabled = false
            view.scrollView.userInteractionEnabled = false

            // 关键：禁用 WebView 本身的用户交互，防止在禁用状态下仍可滚动
            view.userInteractionEnabled = false

            println("[Scroll Debug] WebView created, interaction disabled")

            val color = Color(0xFFFCFCFC).toUIColor()
            view.backgroundColor = color
            view.opaque = false
            if (url != null) {
                view.loadRequest(NSURLRequest(uRL = NSURL(string = url)))
            } else if (htmlContent != null) {
                view.loadHTMLString(htmlContent, baseURL = null)
            }
            view as UIView
        },
        update = { _ ->
            // update 块保持空白
        }
    )
}

@Composable
actual fun OpenInBrowserTab(url: String) {
    val viewController = LocalUIViewController.current
    val safariVC = SFSafariViewController(uRL = NSURL(string = url))
    viewController.presentViewController(safariVC, animated = true, completion = null)
}
