package com.slax.reader.ui.bookmark

import android.annotation.SuppressLint
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.FrameRateCategory
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.preferredFrameRate
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.slax.reader.ui.bookmark.components.*
import com.slax.reader.ui.bookmark.states.ScrollInfo
import com.slax.reader.utils.AppWebView
import com.slax.reader.utils.AppWebViewState
import com.slax.reader.utils.WebViewEvent
import com.slax.reader.utils.wrapBookmarkDetailHtml
import kotlinx.serialization.Serializable

@Serializable
data class WebViewMessage(
    val type: String,
    val height: Int? = null,
    val src: String? = null,
    val allImages: List<String>? = null,
    val position: Int? = null,
    val index: Int? = null,
    val percentage: Double? = null
)

@SuppressLint("UseKtx", "ConfigurationScreenWidthHeight")
@Composable
actual fun DetailScreen(
    htmlContent: String,
    webViewState: AppWebViewState,
    onScrollInfoChanged: (ScrollInfo) -> Unit
) {
    println("[watch][UI] recomposition DetailScreen")

    val wrappedHtmlContent = remember(htmlContent) { wrapBookmarkDetailHtml(htmlContent) }

    val scrollState = rememberScrollState()

    // 获取屏幕高度（用于计算滚动偏移）
    val configuration = LocalConfiguration.current
    val screenHeightPx = with(LocalDensity.current) { configuration.screenHeightDp.dp.toPx() }

    // 记录 HeaderContent 的高度（px）
    val headerHeightState = remember { mutableFloatStateOf(0f) }
    val webViewHeightState = remember { mutableFloatStateOf(0f) }

    val bottomThresholdPx = with(LocalDensity.current) { 100.dp.toPx() }

    // 是否接近底部
    val isNearBottom by remember {
        derivedStateOf {
            scrollState.maxValue - scrollState.value < bottomThresholdPx
        }
    }

    LaunchedEffect(Unit) {
        snapshotFlow { scrollState.value.toFloat() to isNearBottom }
            .collect { (scrollY, nearBottom) ->
                onScrollInfoChanged(ScrollInfo(scrollY, nearBottom))
            }
    }

    LaunchedEffect(webViewState) {
        webViewState.events.collect { event ->
            when (event) {
                is WebViewEvent.ScrollToPosition -> {
                    val targetInWebView = webViewHeightState.floatValue * event.percentage
                    val target = (headerHeightState.floatValue + targetInWebView - screenHeightPx / 4).toInt()
                    scrollState.animateScrollTo(target.coerceAtLeast(0))
                }
                else -> {}
            }
        }
    }

    val onHeightChanged = remember {
        { height: Float -> headerHeightState.floatValue = height }
    }

    val onWebViewSizeChanged: (IntSize) -> Unit = remember {
        { size -> webViewHeightState.floatValue = size.height.toFloat() }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFFCFCFC))
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
        ) {
            HeaderContent(onHeightChanged = onHeightChanged)

            AppWebView(
                htmlContent = wrappedHtmlContent,
                modifier = Modifier
                    .fillMaxWidth()
                    .preferredFrameRate(FrameRateCategory.High)
                    .onSizeChanged(onWebViewSizeChanged),
                webState = webViewState
            )
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
