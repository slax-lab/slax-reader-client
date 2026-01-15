package com.slax.reader.ui.bookmark

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.FrameRateCategory
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.preferredFrameRate
import androidx.compose.ui.unit.dp
import com.slax.reader.ui.bookmark.components.*
import com.slax.reader.utils.*
import kotlinx.serialization.Serializable
import org.koin.compose.viewmodel.koinViewModel
import kotlin.math.max

@Serializable
data class WebViewMessage(
    val type: String,
    val height: Int? = null,
    val src: String? = null,
    val allImages: List<String>? = null,
    val position: Int? = null,
    val index: Int? = null,
    val percentage: Double? = null,

    val productId: String? = null,
    val orderId: String? = null,
    val offerId: String? = null,
    val keyID: String? = null,
    val nonce: String? = null,
    val signature: String? = null,
    val timestamp: Long? = null
)

@Composable
actual fun DetailScreen(htmlContent: String) {
    println("[watch][UI] recomposition DetailScreen.ios")

    val viewModel = koinViewModel<BookmarkDetailViewModel>()
    val bookmarkId by viewModel.bookmarkId.collectAsState()

    val wrappedHtmlContent = remember(htmlContent) { wrapBookmarkDetailHtml(htmlContent) }

    // WebView 滚动偏移
    val webViewScrollY = remember { mutableFloatStateOf(0f) }

    var contentHeightPx by remember { mutableFloatStateOf(0f) }
    var visibleHeightPx by remember { mutableFloatStateOf(0f) }

    // 顶部内容高度 (px)
    val headerMeasuredHeightState = remember { mutableFloatStateOf(0f) }

    var manuallyVisible by remember { mutableStateOf(true) }

    val coroutineScope = rememberCoroutineScope()
    val webViewState = rememberAppWebViewState(coroutineScope)

    val bottomThresholdPx = with(LocalDensity.current) { 100.dp.toPx() }

    LaunchedEffect(Unit) {
        LifeCycleHelper.lifecycleState.collect { state ->
            when (state) {
                AppLifecycleState.ON_STOP -> {
                    viewModel.onStopRecordContinue(webViewScrollY.floatValue.toInt())
                }

                AppLifecycleState.ON_RESUME -> {
                    viewModel.onResumeClearContinue()
                }

                else -> {
                }
            }
        }
    }

    // 距离底部的距离
    val isNearBottom by remember {
        derivedStateOf {
            val distanceToBottom = contentHeightPx - (webViewScrollY.floatValue + visibleHeightPx) + headerMeasuredHeightState.floatValue
            distanceToBottom < bottomThresholdPx
        }
    }

    // 统一处理 manuallyVisible 的自动更新逻辑
    LaunchedEffect(Unit) {
        snapshotFlow { isNearBottom to webViewScrollY.floatValue }
            .collect { (nearBottom, scrollY) ->
                manuallyVisible = when {
                    nearBottom -> true  // 在底部区域，强制显示
                    scrollY <= 10f -> true  // 在顶部区域，显示
                    else -> false  // 中间区域，自动隐藏
                }
            }
    }

    val headerVisible by remember {
        derivedStateOf {
            headerMeasuredHeightState.floatValue == 0f || webViewScrollY.floatValue < headerMeasuredHeightState.floatValue
        }
    }

    // 设置 webState 的参数
    LaunchedEffect(headerMeasuredHeightState.floatValue) {
        webViewState.topContentInsetPx = headerMeasuredHeightState.floatValue
    }

    LaunchedEffect(viewModel) {
        viewModel.effects.collect { effect ->
            when (effect) {
                is BookmarkDetailEffect.ScrollToAnchor -> {
                    webViewState.scrollToAnchor(effect.anchor)
                }
                else -> {}
            }
        }
    }

    // 监听 WebView 事件
    LaunchedEffect(webViewState) {
        webViewState.events.collect { event ->
            when (event) {
                is WebViewEvent.ImageClick -> {
                    viewModel.overlayDelegate.onWebViewImageClick(event.src, event.allImages)
                }
                is WebViewEvent.ScrollChange -> {
                    webViewScrollY.floatValue = max(event.scrollY, 0f)
                    contentHeightPx = event.contentHeight
                    visibleHeightPx = event.visibleHeight
                }
                is WebViewEvent.ScrollToPosition -> {
                    webViewState.evaluateJs("window.scrollTo(0, document.body.scrollHeight * ${event.percentage})")
                }
                is WebViewEvent.Tap -> {
                    if (!isNearBottom && webViewScrollY.floatValue > 10f) {
                        manuallyVisible = !manuallyVisible
                    }
                }

                is WebViewEvent.RefreshContent -> {
                    viewModel.refreshContent()
                }

                is WebViewEvent.Feedback -> {}

                else -> {}
            }
        }
    }

    val onHeightChanged = remember {
        { height: Float -> headerMeasuredHeightState.floatValue = height }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFFCFCFC))
    ) {
        key(bookmarkId) {
            AppWebView(
                htmlContent = wrappedHtmlContent,
                modifier = Modifier.fillMaxSize().preferredFrameRate(FrameRateCategory.High),
                webState = webViewState
            )
        }

        if (headerVisible) {
            Box(
                modifier = Modifier.graphicsLayer {
                    translationY = -webViewScrollY.floatValue
                }
            ) {
                HeaderContent(onHeightChanged = onHeightChanged)
            }
        }

        NavigatorBar(visible = manuallyVisible)

        FloatingActionBar(
            visible = manuallyVisible,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 58.dp),
        )

        OutlineDialog()
    }

    BookmarkDetailOverlays()
}
