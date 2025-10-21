package com.slax.reader.utils

import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.UIKitView
import kotlinx.cinterop.ExperimentalForeignApi
import platform.CoreGraphics.CGRectMake
import platform.Foundation.NSURL
import platform.Foundation.NSURLRequest
import platform.UIKit.UIColor
import platform.UIKit.UIScrollViewContentInsetAdjustmentBehavior
import platform.UIKit.UIView
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

@OptIn(ExperimentalForeignApi::class)
@Composable
actual fun AppWebView(
    url: String?,
    htmlContent: String?,
    updateKey: String,
    modifier: Modifier,
    onHeightChange: ((Double) -> Unit)?,
) {
    val messageHandler = remember(onHeightChange) { HeightMessageHandler { h -> onHeightChange?.invoke(h) } }
    var lastSignature by remember { mutableStateOf<String?>(null) }

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
            lastSignature = updateKey
            view as UIView
        },
        update = { uiView ->
            val web = uiView as WKWebView
            if (lastSignature != updateKey) {
                if (url != null) {
                    val current = web.URL?.absoluteString
                    if (current != url) {
                        web.loadRequest(NSURLRequest(uRL = NSURL(string = url)))
                    }
                } else if (htmlContent != null) {
                    web.loadHTMLString(htmlContent, baseURL = null)
                }
                lastSignature = updateKey
            }
        }
    )
}
