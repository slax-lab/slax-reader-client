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
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.window.Popup
import com.slax.reader.ui.bookmark.components.*
import com.slax.reader.ui.bookmark.states.ScrollInfo
import com.slax.reader.utils.AppWebView
import com.slax.reader.utils.AppWebViewState
import com.slax.reader.utils.WebViewEvent
import com.slax.reader.utils.wrapBookmarkDetailHtml
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
    val percentage: Double? = null,
    val text: String? = null,
    val selectionY: Float? = null,
    val markId: String? = null,
    val markItemInfo: String? = null,
)

@SuppressLint("UseKtx", "ConfigurationScreenWidthHeight")
@Composable
actual fun DetailScreen(
    bookmarkId: String,
    htmlContent: String,
    webViewState: AppWebViewState,
    onScrollInfoChanged: (ScrollInfo) -> Unit
) {
    println("[watch][UI] recomposition DetailScreen")

    val viewModel = koinViewModel<BookmarkDetailViewModel>()

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
                is WebViewEvent.PageLoaded -> {
                    viewModel.consumeInitialReadPosition()?.let { position ->
                        scrollState.scrollTo(position.toInt())
                    }
                }
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
                webState = webViewState,
                bookmarkId = bookmarkId
            )
        }

        NavigatorBar()

        FloatingActionBar(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 58.dp),
        )

        // 文本选中操作菜单
        val selectionMenuVisible by LocalSelectionMenuVisible.current
        val selectionYPx by LocalSelectionYPx.current
        // 提前在 @Composable 上下文中捕获 MutableState 引用，供非 @Composable lambda 使用
        val selectionMenuState = LocalSelectionMenuVisible.current
        val commentPanelState = LocalCommentPanelVisible.current
        val density = LocalDensity.current
        val minTopPx = with(density) { 60.dp.roundToPx() }
        val menuGapPx = with(density) { 8.dp.roundToPx() }
        val menuHeightPx = with(density) { 38.dp.roundToPx() }

        val showMenu = selectionMenuVisible && selectionYPx > 0f && selectionYPx < screenHeightPx

        if (showMenu) {
            val touchY = selectionYPx.toInt()
            val isTopArea = touchY < screenHeightPx * 0.2f
            val offsetY = if (isTopArea) {
                touchY + menuGapPx
            } else {
                touchY - menuHeightPx - menuGapPx
            }.coerceIn(minTopPx, (screenHeightPx - menuHeightPx).toInt())

            Popup(
                alignment = Alignment.TopCenter,
                offset = IntOffset(0, offsetY)
            ) {
                SelectionActionBar(
                    visible = true,
                    actions = rememberSelectionActions(),
                    onActionClick = { actionId ->
                        handleSelectionAction(
                            actionId = actionId,
                            webViewState = webViewState,
                            onHighlightRequest = {
                                // 隐藏选中菜单，触发划线流程
                                selectionMenuState.value = false
                                viewModel.strokeHighlight(webViewState)
                            }
                        )
                    }
                )
            }
        }

        OutlineDialog()

        // 评论面板
        val commentPanelVisible by commentPanelState
        val selectedText by LocalSelectedText.current
        val selectedMarkItemInfoState = LocalSelectedMarkItemInfo.current
        val selectedMarkItemInfo by selectedMarkItemInfoState
        CommentPanelSheet(
            highlightedText = selectedText,
            markItemInfo = selectedMarkItemInfo,
            userAvatarUrl = viewModel.userInfo.value?.picture,
            visible = commentPanelVisible,
            onDismiss = {
                selectedMarkItemInfoState.value = null
                commentPanelState.value = false
            },
            onActionClick = { /* 后续实现各操作逻辑 */ }
        )
    }

    BookmarkDetailOverlays()
}
