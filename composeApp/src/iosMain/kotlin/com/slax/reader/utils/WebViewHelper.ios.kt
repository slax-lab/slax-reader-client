package com.slax.reader.utils

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.uikit.LocalUIViewController
import androidx.compose.ui.viewinterop.UIKitView
import com.slax.reader.const.HEIGHT_MONITOR_SCRIPT
import com.slax.reader.const.JS_BRIDGE_NAME
import com.slax.reader.const.TAP_LISTENER_SCRIPT
import com.slax.reader.model.BridgeMessageParser
import com.slax.reader.model.HeightMessage
import com.slax.reader.model.TapMessage
import kotlinx.cinterop.ExperimentalForeignApi
import platform.CoreGraphics.CGRectMake
import platform.Foundation.NSURL
import platform.Foundation.NSURLRequest
import platform.SafariServices.SFSafariViewController
import platform.UIKit.UIColor
import platform.UIKit.UIScrollViewContentInsetAdjustmentBehavior
import platform.UIKit.UIView
import platform.WebKit.*
import platform.darwin.NSObject

private class MessageHandler(
    private val onHeightChange: ((Double) -> Unit)?,
    private val onTap: (() -> Unit)?
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
            is TapMessage -> onTap?.invoke()
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

@OptIn(ExperimentalForeignApi::class)
@Composable
actual fun AppWebView(
    url: String?,
    htmlContent: String?,
    modifier: Modifier,
    onHeightChange: ((Double) -> Unit)?,
    onTap: (() -> Unit)?
) {
    val messageHandler = remember(onHeightChange, onTap) {
        MessageHandler(onHeightChange, onTap)
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
                    addUserScript(heightScript)

                    if (onTap != null) {
                        val tapScript = WKUserScript(
                            source = TAP_LISTENER_SCRIPT,
                            injectionTime = WKUserScriptInjectionTime.WKUserScriptInjectionTimeAtDocumentEnd,
                            forMainFrameOnly = true
                        )
                        addUserScript(tapScript)
                    }
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