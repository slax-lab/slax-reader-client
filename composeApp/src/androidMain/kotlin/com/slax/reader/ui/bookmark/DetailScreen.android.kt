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
import androidx.compose.ui.unit.dp
import com.slax.reader.ui.bookmark.components.*
import com.slax.reader.utils.AppLifecycleState
import com.slax.reader.utils.AppWebView
import com.slax.reader.utils.LifeCycleHelper
import com.slax.reader.utils.WebViewEvent
import com.slax.reader.utils.rememberAppWebViewState
import com.slax.reader.utils.wrapBookmarkDetailHtml
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import org.koin.compose.viewmodel.koinViewModel

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
actual fun DetailScreen(htmlContent: String) {
    println("[watch][UI] recomposition DetailScreen")

    val viewModel = koinViewModel<BookmarkDetailViewModel>()

    val wrappedHtmlContent = remember(htmlContent) { wrapBookmarkDetailHtml(htmlContent) }

    val scrollState = rememberScrollState()
    val scrollY by remember { derivedStateOf { scrollState.value.toFloat() } }
    var manuallyVisible by remember { mutableStateOf(true) }

    // 获取屏幕高度（用于计算滚动偏移）
    val configuration = LocalConfiguration.current
    val screenHeightPx = with(LocalDensity.current) { configuration.screenHeightDp.dp.toPx() }

    // 记录 HeaderContent 的高度（px）
    val headerHeightState = remember { mutableFloatStateOf(0f) }
    val webViewHeightState = remember { mutableFloatStateOf(0f) }

    val coroutineScope = rememberCoroutineScope()
    val webViewState = rememberAppWebViewState(coroutineScope)

    val bottomThresholdPx = with(LocalDensity.current) { 100.dp.toPx() }

    LaunchedEffect(Unit) {
        LifeCycleHelper.lifecycleState.collect { state ->
            when (state) {
                AppLifecycleState.ON_STOP -> {
                    viewModel.onStopRecordContinue(scrollState.value)
                }

                AppLifecycleState.ON_RESUME -> {
                    viewModel.onResumeClearContinue()
                }

                else -> {
                }
            }
        }
    }

    // 是否接近底部
    val isNearBottom by remember {
        derivedStateOf {
            scrollState.maxValue - scrollState.value < bottomThresholdPx
        }
    }

    LaunchedEffect(Unit) {
        snapshotFlow { isNearBottom to scrollState.value }
            .collect { (nearBottom, scrollValue) ->
                manuallyVisible = when {
                    nearBottom -> true  // 在底部区域，强制显示
                    scrollValue <= 10 -> true  // 在顶部区域，显示
                    else -> false  // 中间区域，自动隐藏
                }
            }
    }

    LaunchedEffect(webViewState) {
        webViewState.events.collect { event ->
            when (event) {
                is WebViewEvent.ImageClick -> {
                    viewModel.overlayDelegate.onWebViewImageClick(event.src, event.allImages)
                }
                is WebViewEvent.ScrollToPosition -> {
                    val targetInWebView = webViewHeightState.floatValue * event.percentage
                    val target = (headerHeightState.floatValue + targetInWebView - screenHeightPx / 4).toInt()
                    coroutineScope.launch { scrollState.animateScrollTo(target.coerceAtLeast(0)) }
                }
                is WebViewEvent.Tap -> {
                    if (!isNearBottom && scrollY > 10f) {
                        manuallyVisible = !manuallyVisible
                    }
                }

                is WebViewEvent.RefreshContent -> {
                    viewModel.refreshContent()
                }

                is WebViewEvent.Feedback -> {
                    println("feedback")
                }

                else -> {}
            }
        }
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

    val onHeightChanged = remember {
        { height: Float -> headerHeightState.floatValue = height }
    }

    val onWebViewSizeChanged: (androidx.compose.ui.unit.IntSize) -> Unit = remember {
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
