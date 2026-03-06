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
import com.slax.reader.ui.bookmark.states.ScrollInfo
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
actual fun DetailScreen(
    htmlContent: String,
    webViewState: AppWebViewState,
    onScrollInfoChanged: (ScrollInfo) -> Unit
) {
    println("[watch][UI] recomposition DetailScreen.ios")

    val viewModel = koinViewModel<BookmarkDetailViewModel>()
    val bookmarkId by viewModel.bookmarkId.collectAsState()
    val savedPosition by viewModel.savedPosition.collectAsState()
    val hasRestoredPosition by viewModel.hasRestoredPosition.collectAsState()

    val wrappedHtmlContent = remember(htmlContent) { wrapBookmarkDetailHtml(htmlContent) }

    // WebView 滚动偏移
    val webViewScrollY = remember { mutableFloatStateOf(0f) }

    var contentHeightPx by remember { mutableFloatStateOf(0f) }
    var visibleHeightPx by remember { mutableFloatStateOf(0f) }

    // 顶部内容高度 (px)
    val headerMeasuredHeightState = remember { mutableFloatStateOf(0f) }

    val bottomThresholdPx = with(LocalDensity.current) { 100.dp.toPx() }

    // 距离底部的距离
    val isNearBottom by remember {
        derivedStateOf {
            val distanceToBottom = contentHeightPx - (webViewScrollY.floatValue + visibleHeightPx) + headerMeasuredHeightState.floatValue
            distanceToBottom < bottomThresholdPx
        }
    }

    // 标记是否已恢复位置
    LaunchedEffect(Unit) {
        snapshotFlow { webViewScrollY.floatValue to isNearBottom }
            .collect { (scrollY, nearBottom) ->
                // 只在恢复位置后才处理滚动事件
                if (hasRestoredPosition) {
                    onScrollInfoChanged(ScrollInfo(scrollY, nearBottom))
                    print("[watch][UI] scrollY: $scrollY, isNearBottom: $nearBottom")
                    viewModel.saveReadPosition(scrollY)
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

    val density = LocalDensity.current
    val densityScale = density.density

    // 获取 statusBarsPadding 高度
    val windowInsets = WindowInsets.statusBars
    val statusBarHeightPx = windowInsets.getTop(density).toFloat()

    LaunchedEffect(webViewState) {
        webViewState.events.collect { event ->
            when (event) {
                is WebViewEvent.PageLoaded -> {
                    // WebView 加载完成后恢复滚动位置
                    val position = savedPosition
                    if (position != null && position > 0f && !hasRestoredPosition) {
                        // iOS contentInset 需要完整高度：Column + statusBarsPadding + 视觉间距
                        val totalInsetPx = webViewState.topContentInsetPx + statusBarHeightPx + 16f * density.density

                        println("[watch][UI] restoring scroll position: $position px")
                        kotlinx.coroutines.delay(100) // 等待布局稳定

                        // 将像素值转换为 points（逻辑像素）
                        val positionPoints = (position - totalInsetPx) / densityScale
                        println("[watch][UI] converted to points: $positionPoints")
                        webViewState.evaluateJs("window.scrollTo(0, $positionPoints)")
                    }

                    viewModel.markPositionRestored()
                }
                is WebViewEvent.ScrollChange -> {
                    webViewScrollY.floatValue = max(event.scrollY, 0f)
                    contentHeightPx = event.contentHeight
                    visibleHeightPx = event.visibleHeight
                }
                is WebViewEvent.ScrollToPosition -> {
                    webViewState.evaluateJs("window.scrollTo(0, document.body.scrollHeight * ${event.percentage})")
                }
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

        NavigatorBar()

        FloatingActionBar(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 58.dp),
        )

        OutlineDialog()
    }

    BookmarkDetailOverlays()
}
