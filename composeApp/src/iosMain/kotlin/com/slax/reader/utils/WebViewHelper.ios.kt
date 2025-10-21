package com.slax.reader.utils

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.uikit.LocalUIViewController
import androidx.compose.ui.viewinterop.UIKitView
import com.slax.reader.const.HEIGHT_MONITOR_SCRIPT
import com.slax.reader.const.JS_BRIDGE_NAME
import kotlinx.cinterop.BetaInteropApi
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.ObjCAction
import platform.CoreGraphics.CGRectMake
import platform.Foundation.NSSelectorFromString
import platform.Foundation.NSURL
import platform.Foundation.NSURLRequest
import platform.SafariServices.SFSafariViewController
import platform.UIKit.*
import platform.WebKit.*
import platform.darwin.NSObject

private class HeightMessageHandler(private val onHeight: (Double) -> Unit) : NSObject(),
    WKScriptMessageHandlerProtocol {
    override fun userContentController(
        userContentController: WKUserContentController,
        didReceiveScriptMessage: WKScriptMessage
    ) {
        val body = didReceiveScriptMessage.body
        val text = body.toString()
        val regex = Regex("\"height\":\\s*([0-9.]+)")
        val v = regex.find(text)?.groupValues?.getOrNull(1)?.toDoubleOrNull()
        if (v != null) onHeight(v)
    }
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

private class TapGestureDelegate : NSObject(), UIGestureRecognizerDelegateProtocol {
    override fun gestureRecognizer(
        gestureRecognizer: UIGestureRecognizer,
        shouldRecognizeSimultaneouslyWithGestureRecognizer: UIGestureRecognizer
    ): Boolean {
        // 允许同时识别多个手势，不阻塞其他手势
        return true
    }

    override fun gestureRecognizer(
        gestureRecognizer: UIGestureRecognizer,
        shouldReceiveTouch: UITouch
    ): Boolean {
        // 总是接收触摸，但不干扰其他手势
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
) {
    val messageHandler = remember(onHeightChange) { HeightMessageHandler { h -> onHeightChange?.invoke(h) } }
    val tapHandler = remember(onTap) {
        TapHandler { onTap?.invoke() }
    }
    val tapGestureDelegate = remember { TapGestureDelegate() }

    UIKitView(
        modifier = modifier,
        factory = {
            val config = WKWebViewConfiguration().apply {
                preferences = WKPreferences().apply {
                    javaScriptEnabled = true
                }
                userContentController = WKUserContentController().apply {
                    addScriptMessageHandler(messageHandler, JS_BRIDGE_NAME)
                    val script = WKUserScript(
                        source = HEIGHT_MONITOR_SCRIPT,
                        injectionTime = WKUserScriptInjectionTime.WKUserScriptInjectionTimeAtDocumentEnd,
                        forMainFrameOnly = true
                    )
                    addUserScript(script)
                }
            }

            val view = WKWebView(frame = CGRectMake(0.0, 0.0, 0.0, 0.0), configuration = config)

            // 添加轻量级的点击手势监听，不干扰其他手势
            val tapGesture = UITapGestureRecognizer(
                target = tapHandler,
                action = NSSelectorFromString("handleTap")
            )
            tapGesture.delegate = tapGestureDelegate
            tapGesture.cancelsTouchesInView = false  // 关键：不取消其他触摸事件
            view.addGestureRecognizer(tapGesture)

            view.scrollView.panGestureRecognizer.enabled = false
            view.scrollView.contentInsetAdjustmentBehavior =
                UIScrollViewContentInsetAdjustmentBehavior.UIScrollViewContentInsetAdjustmentNever
            view.scrollView.showsHorizontalScrollIndicator = false
            view.scrollView.showsVerticalScrollIndicator = false
            view.scrollView.alwaysBounceHorizontal = false
            view.scrollView.opaque = false
            view.scrollView.scrollEnabled = false
            view.scrollView.bounces = false
            view.scrollView.alwaysBounceVertical = false

            view.setUnderPageBackgroundColor(UIColor.whiteColor)
            view.setBackgroundColor(UIColor.clearColor)

            if (url != null) {
                view.loadRequest(NSURLRequest(uRL = NSURL(string = url)))
            } else if (htmlContent != null) {
                view.loadHTMLString(htmlContent, baseURL = null)
            }
            view as UIView
        },
        update = { uiView ->
            val webView = uiView as WKWebView
            when {
                url != null -> {
                    webView.loadRequest(NSURLRequest(uRL = NSURL(string = url)))
                }

                htmlContent != null -> {
                    webView.loadHTMLString(htmlContent, baseURL = null)
                }
            }
        }
    )
}

@Composable
actual fun OpenInBrowserTab(url: String) {
    val viewController = LocalUIViewController.current
    val safariVC = SFSafariViewController(uRL = NSURL(string = url))
    viewController.presentViewController(safariVC, animated = true, completion = null)
}