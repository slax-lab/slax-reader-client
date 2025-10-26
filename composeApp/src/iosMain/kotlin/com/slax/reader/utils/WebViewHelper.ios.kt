package com.slax.reader.utils

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.uikit.LocalUIViewController
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.viewinterop.UIKitInteropInteractionMode
import androidx.compose.ui.viewinterop.UIKitInteropProperties
import androidx.compose.ui.viewinterop.UIKitView
import app.slax.reader.SlaxConfig
import kotlinx.cinterop.BetaInteropApi
import kotlinx.cinterop.CValue
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.ObjCAction
import kotlinx.cinterop.useContents
import platform.CoreGraphics.CGRectMake
import platform.Foundation.NSSelectorFromString
import platform.Foundation.NSURL
import platform.Foundation.NSURLRequest
import platform.Foundation.NSValue
import platform.SafariServices.SFSafariViewController
import platform.UIKit.CGPointValue
import platform.UIKit.UIColor
import platform.UIKit.UIEdgeInsets
import platform.UIKit.UIEdgeInsetsMake
import platform.UIKit.UIGestureRecognizer
import platform.UIKit.UIGestureRecognizerDelegateProtocol
import platform.UIKit.UIScrollView
import platform.UIKit.UIScrollViewContentInsetAdjustmentBehavior
import platform.UIKit.UIScrollViewDecelerationRateNormal
import platform.UIKit.UIScrollViewDelegateProtocol
import platform.UIKit.UITapGestureRecognizer
import platform.UIKit.UITouch
import platform.UIKit.UIView
import platform.WebKit.WKPreferences
import platform.WebKit.WKWebView
import platform.WebKit.WKWebViewConfiguration
import platform.WebKit.javaScriptEnabled
import platform.darwin.NSObject

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
    topContentInsetPx: Float,
    onTap: (() -> Unit)?,
    onScrollChange: ((scrollY: Float) -> Unit)?,
) {
    val tapHandler = remember(onTap) {
        TapHandler { onTap?.invoke() }
    }
    val tapGestureDelegate = remember { TapGestureDelegate() }
    val density = LocalDensity.current
    val densityScale = density.density
    val topInsetPoints by remember(topContentInsetPx, densityScale) {
        derivedStateOf { (topContentInsetPx / densityScale).coerceAtLeast(0f) }
    }
    val onScrollChangeState = rememberUpdatedState(onScrollChange)
    var observedScrollView by remember { mutableStateOf<UIScrollView?>(null) }
    val scrollObserver = remember {
        KVOObserver { keyPath, newValue, _ ->
            if (keyPath == "contentOffset") {
                val offsetPoints =
                    (newValue as? NSValue)?.CGPointValue()?.useContents { y }?.toFloat() ?: 0f
                val offsetPx = offsetPoints * densityScale
                println("[iOS WebView Scroll] offsetY(points)=$offsetPoints -> offsetPx=$offsetPx")
                onScrollChangeState.value?.invoke(offsetPx)
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
            }

            val view = WKWebView(frame = CGRectMake(0.0, 0.0, 0.0, 0.0), configuration = config)
            view.inspectable = SlaxConfig.BUILD_ENV == "dev"

            // 添加点击手势
            val tapGesture = UITapGestureRecognizer(
                target = tapHandler,
                action = NSSelectorFromString(tapHandler::handleTap.name)
            )
            tapGesture.delegate = tapGestureDelegate
            tapGesture.cancelsTouchesInView = false
            view.addGestureRecognizer(tapGesture)

            // 配置滚动视图
            view.scrollView.apply {
                contentInsetAdjustmentBehavior =
                    UIScrollViewContentInsetAdjustmentBehavior.UIScrollViewContentInsetAdjustmentNever
                showsHorizontalScrollIndicator = false
                showsVerticalScrollIndicator = false
                alwaysBounceHorizontal = false
                bounces = true
                alwaysBounceVertical = true

                // 启用滚动
                scrollEnabled = true

                // 性能优化
                delaysContentTouches = false
                canCancelContentTouches = true
                decelerationRate = UIScrollViewDecelerationRateNormal
            }
            if (observedScrollView !== view.scrollView) {
                observedScrollView = view.scrollView
            }

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
            val scrollView = webView.scrollView
            if (observedScrollView !== scrollView) {
                observedScrollView = scrollView
            }
        },
        // 使用 UIKitInteropProperties 配置交互模式
        properties = UIKitInteropProperties(
            interactionMode = UIKitInteropInteractionMode.Cooperative()
        )
    )
}

@Composable
actual fun OpenInBrowserTab(url: String) {
    val viewController = LocalUIViewController.current
    val safariVC = SFSafariViewController(uRL = NSURL(string = url))
    viewController.presentViewController(safariVC, animated = true, completion = null)
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
    url: String?,
    htmlContent: String?,
    modifier: Modifier,
    contentInsets: PaddingValues?,
    onScroll: ((x: Double, y: Double) -> Unit)?
) {

    val scrollDelegate = remember {
        object : NSObject(), UIScrollViewDelegateProtocol {
            override fun scrollViewDidScroll(scrollView: UIScrollView) {
                val contentOffset = scrollView.contentOffset
                onScroll?.invoke(
                    contentOffset.useContents { x },
                    contentOffset.useContents { y }
                )
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
                inspectable = SlaxConfig.BUILD_ENV == "dev"
                backgroundColor = Color(0xFFFCFCFC).toUIColor()

                scrollView.contentInsetAdjustmentBehavior =
                    UIScrollViewContentInsetAdjustmentBehavior.UIScrollViewContentInsetAdjustmentNever
                scrollView.alwaysBounceVertical = true
                scrollView.alwaysBounceHorizontal = false
                scrollView.delegate = scrollDelegate

                scrollView.contentInset = contentInsets?.toUIEdgeInsets ?: UIEdgeInsets_zero
                when {
                    url != null -> loadRequest(NSURLRequest(uRL = NSURL(string = url)))
                    htmlContent != null -> loadHTMLString(htmlContent, baseURL = null)
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
