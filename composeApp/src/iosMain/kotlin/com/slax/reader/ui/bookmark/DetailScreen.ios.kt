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
import androidx.compose.ui.unit.IntOffset
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
    val timestamp: Long? = null,

    val text: String? = null,
    val selectionY: Float? = null
)

@Composable
actual fun DetailScreen(
    bookmarkId: String,
    htmlContent: String,
    webViewState: AppWebViewState,
    onScrollInfoChanged: (ScrollInfo) -> Unit
) {
    println("[watch][UI] recomposition DetailScreen.ios")

    val viewModel = koinViewModel<BookmarkDetailViewModel>()

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

    LaunchedEffect(Unit) {
        snapshotFlow { webViewScrollY.floatValue to isNearBottom }
            .collect { (scrollY, nearBottom) ->
                onScrollInfoChanged(ScrollInfo(scrollY, nearBottom))
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

    val windowInsets = WindowInsets.statusBars
    val statusBarHeightPx = windowInsets.getTop(density).toFloat()

    LaunchedEffect(webViewState) {
        webViewState.events.collect { event ->
            when (event) {
                is WebViewEvent.PageLoaded -> {
                    viewModel.consumeInitialReadPosition()?.let { position ->
                        val totalInsetPx = webViewState.topContentInsetPx + statusBarHeightPx +
                                16f * density.density

                        val positionPoints = (position - totalInsetPx) / densityScale
                        webViewState.evaluateJs("window.scrollTo(0, $positionPoints)")
                    }
                }
                is WebViewEvent.ScrollChange -> {
                    webViewScrollY.floatValue = max(event.scrollY, 0f)
                    contentHeightPx = event.contentHeight
                    visibleHeightPx = event.visibleHeight
                }
                is WebViewEvent.ScrollToPosition -> {
                    val totalInsetPx = webViewState.topContentInsetPx + statusBarHeightPx +
                            16f * density.density
                    println(totalInsetPx / densityScale)
                    webViewState.evaluateJs("window.scrollTo(0, Math.max(0,document.body.scrollHeight * ${event.percentage} - ${totalInsetPx / densityScale}))")
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
                webState = webViewState,
                bookmarkId = bookmarkId
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

        // 文本选中操作菜单 - 水平居中，垂直方向跟随选中位置
        val selectionMenuVisible by LocalSelectionMenuVisible.current
        val selectionYPx by LocalSelectionYPx.current

        /**
         * 菜单垂直定位计算逻辑：
         *
         * iOS 中 WebView 占满屏幕，内部滚动。
         * JS 返回的 selectionY 是相对于 WebView 视口（可见区域）的坐标（CSS points）。
         * 由于 WebView 占满屏幕，视口顶部即屏幕顶部，
         * 所以 selectionY（points）直接对应屏幕上的位置。
         *
         * 转换到 Compose 像素：selectionY * densityScale
         *
         * 菜单默认显示在选中位置上方，留出间距；
         * 若上方空间不足则显示在下方。
         */
        val menuOffsetY by remember {
            derivedStateOf {
                if (selectionYPx <= 0f) return@derivedStateOf 0

                // selectionYPx 是 CSS points，转换为 Compose px
                val selectionScreenY = selectionYPx * densityScale

                // 菜单显示在选中位置上方，留出 48dp 间距
                val menuGapPx = 48.dp.value * densityScale
                val minTopPx = (statusBarHeightPx + 20.dp.value * densityScale)

                val targetY = selectionScreenY - menuGapPx

                // 确保菜单不超出屏幕顶部（考虑状态栏）
                targetY.toInt().coerceAtLeast(minTopPx.toInt())
            }
        }

        SelectionActionBar(
            visible = selectionMenuVisible,
            actions = rememberSelectionActions(),
            onActionClick = { actionId ->
                handleSelectionAction(actionId, webViewState)
            },
            modifier = Modifier
                .align(Alignment.TopCenter)
                .offset { IntOffset(0, menuOffsetY) }
        )

        OutlineDialog()
    }

    BookmarkDetailOverlays()
}
