package com.slax.reader.utils

import androidx.compose.foundation.layout.statusBars
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.uikit.LocalUIViewController
import androidx.compose.ui.viewinterop.UIKitInteropInteractionMode
import androidx.compose.ui.viewinterop.UIKitInteropProperties
import androidx.compose.ui.viewinterop.UIKitView
import app.slax.reader.SlaxConfig
import kotlinx.cinterop.BetaInteropApi
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.ObjCAction
import kotlinx.cinterop.useContents
import platform.CoreGraphics.CGRectMake
import platform.Foundation.NSSelectorFromString
import platform.Foundation.NSURL
import platform.Foundation.NSURLRequest
import platform.Foundation.NSValue
import platform.SafariServices.SFSafariViewController
import platform.UIKit.*
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
    val densityScaleState = rememberUpdatedState(densityScale)

    // 获取 statusBarsPadding 高度
    val windowInsets = androidx.compose.foundation.layout.WindowInsets.statusBars
    val statusBarHeightPx = windowInsets.getTop(density).toFloat()

    // iOS contentInset 需要完整高度：Column + statusBarsPadding + 视觉间距
    val totalInsetPx = topContentInsetPx + statusBarHeightPx + 16f * density.density

    val topInsetPoints by remember(totalInsetPx, densityScale) {
        derivedStateOf { (totalInsetPx / densityScale).coerceAtLeast(0f) }
    }
    val topInsetPointsState = rememberUpdatedState(topInsetPoints)
    val onScrollChangeState = rememberUpdatedState(onScrollChange)
    var observedScrollView by remember { mutableStateOf<UIScrollView?>(null) }
    val scrollObserver = remember {
        KVOObserver { keyPath, newValue, _ ->
            if (keyPath == "contentOffset") {
                val offsetPoints = (newValue as? NSValue)?.CGPointValue()?.useContents { y }?.toFloat() ?: 0f
                // contentInset 会让初始 contentOffset.y = -contentInset.top
                // 加上 contentInset.top 得到实际滚动距离
                val adjustedOffsetPoints = offsetPoints + topInsetPointsState.value
                val offsetPx = adjustedOffsetPoints * densityScaleState.value
                println("[iOS WebView Scroll] offsetY=$offsetPoints, inset=${topInsetPointsState.value}, adjusted=$adjustedOffsetPoints, px=$offsetPx")
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

                // 禁用左右滑动
                scrollEnabled = true

                // 性能优化
                delaysContentTouches = false
                canCancelContentTouches = true
                decelerationRate = UIScrollViewDecelerationRateNormal

                // 设置 contentInset
                contentInset = platform.UIKit.UIEdgeInsetsMake(
                    top = topInsetPoints.toDouble(),
                    left = 0.0,
                    bottom = 0.0,
                    right = 0.0
                )
            }

            // 禁用 WebView 的左右滑动手势
            view.scrollView.delegate = object : NSObject(), UIScrollViewDelegateProtocol {
                override fun scrollViewDidScroll(scrollView: UIScrollView) {
                    // 强制限制水平滚动为 0
                    if (scrollView.contentOffset.useContents { x } != 0.0) {
                        scrollView.setContentOffset(
                            platform.CoreGraphics.CGPointMake(
                                0.0,
                                scrollView.contentOffset.useContents { y }
                            ),
                            animated = false
                        )
                    }
                }
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

            // 保存当前的实际滚动位置（调整后的）
            val currentOffsetPoints = scrollView.contentOffset.useContents { y }.toFloat()
            val currentAdjustedOffset = currentOffsetPoints + (scrollView.contentInset.useContents { top }.toFloat())

            // 更新 contentInset
            scrollView.contentInset = platform.UIKit.UIEdgeInsetsMake(
                top = topInsetPoints.toDouble(),
                left = 0.0,
                bottom = 0.0,
                right = 0.0
            )

            // 恢复实际滚动位置，避免跳动
            // 新的 contentOffset = 实际位置 - 新的 contentInset.top
            val newOffsetY = currentAdjustedOffset - topInsetPoints
            scrollView.setContentOffset(
                platform.CoreGraphics.CGPointMake(0.0, newOffsetY.toDouble()),
                animated = false
            )

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
@Composable
actual fun WebView(
    url: String?,
    htmlContent: String?,
    modifier: Modifier,
    onScroll: ((x: Double, y: Double) -> Unit)?
) {

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

                scrollView.contentInsetAdjustmentBehavior =
                    UIScrollViewContentInsetAdjustmentBehavior.UIScrollViewContentInsetAdjustmentNever
                scrollView.alwaysBounceVertical = true
                scrollView.alwaysBounceHorizontal = false

                scrollView.delegate = object : NSObject(), UIScrollViewDelegateProtocol {
                    override fun scrollViewDidScroll(scrollView: UIScrollView) {
                        val offset = scrollView.contentOffset
                        onScroll?.invoke(offset.useContents { x }, offset.useContents { y })
                    }
                }

                backgroundColor = Color(0xFFFCFCFC).toUIColor()
                when {
                    url != null -> loadRequest(NSURLRequest(uRL = NSURL(string = url)))
                    htmlContent != null -> loadHTMLString(htmlContent, baseURL = null)
                }
            }
        },
        update = { view ->
            view.apply {
                when {
                    url != null -> loadRequest(NSURLRequest(uRL = NSURL(string = url)))
                    htmlContent != null -> loadHTMLString(htmlContent, baseURL = null)
                }
            }
        }
    )
}
