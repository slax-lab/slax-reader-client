package com.slax.reader.ui.bookmark

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.FrameRateCategory
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.preferredFrameRate
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.window.Popup
import com.slax.reader.ui.bookmark.components.*
import com.slax.reader.ui.bookmark.states.LocalMarkInteraction
import com.slax.reader.ui.bookmark.states.ScrollInfo
import com.slax.reader.utils.*
import kotlinx.serialization.Serializable
import org.koin.compose.viewmodel.koinViewModel
import kotlin.math.max
import platform.UIKit.UIPasteboard

@Serializable
data class WebViewMessage(
    val type: String,
    val height: Int? = null,
    val src: String? = null,
    val allImages: List<String>? = null,
    val percentage: Double? = null,

    val productId: String? = null,
    val orderId: String? = null,
    val offerId: String? = null,
    val keyID: String? = null,
    val nonce: String? = null,
    val signature: String? = null,
    val timestamp: Long? = null,

    val text: String? = null,
    val selectionY: Float? = null,
    val markId: String? = null,
    val markItemInfos: String? = null,
    val data: String? = null,
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

    var containerHeightPx by remember { mutableFloatStateOf(0f) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFFCFCFC))
            .onSizeChanged { containerHeightPx = it.height.toFloat() }
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

        // 文本选中操作菜单
        val markInteraction = LocalMarkInteraction.current

        // 当评论面板显示时，禁用状态栏点击触发的 scrollsToTop 行为，
        // 防止点击状态栏区域导致背后的 WebView 滚动到顶部
        val panelVisible = markInteraction.panelVisible
        LaunchedEffect(panelVisible) {
            webViewState.webView?.scrollView?.scrollsToTop = !panelVisible
        }
        val selectionMenuVisible = markInteraction.menuVisible
        // selectionYPx 是 hitTest 实时获取的屏幕坐标（UIKit points），转为 Compose px
        val selectionYPx = markInteraction.selectionY
        val selectionScreenPx = selectionYPx * densityScale

        val minTopPx = (statusBarHeightPx + 20.dp.value * densityScale).toInt()
        val menuGapPx = with(density) { 32.dp.roundToPx() }
        val menuHeightPx = with(density) { 44.dp.roundToPx() }

        // 复制成功 Toast 状态
        var showCopyToast by remember { mutableStateOf(false) }

        val showMenu = selectionMenuVisible && selectionScreenPx > 0f && selectionScreenPx < containerHeightPx

        if (showMenu) {
            val touchY = selectionScreenPx.toInt()
            val isTopArea = touchY < (containerHeightPx * 0.2f).toInt()
            val offsetY = if (isTopArea) {
                touchY + menuGapPx
            } else {
                touchY - menuHeightPx - menuGapPx
            }.coerceIn(minTopPx, (containerHeightPx - menuHeightPx).toInt())

            Popup(
                alignment = Alignment.TopCenter,
                offset = IntOffset(0, offsetY)
            ) {
                // 在 Popup 内部读取 capturedSelectionMark，确保状态订阅注册在 Popup 的子 Composition 中
                val selectionHasStroke = markInteraction.capturedSelectionMark?.stroke?.isNotEmpty() == true
                SelectionActionBar(
                    visible = true,
                    actions = rememberSelectionActions(hasStroke = selectionHasStroke),
                    onActionClick = { actionId ->
                        handleSelectionAction(
                            actionId = actionId,
                            webViewState = webViewState,
                            onDismiss = {
                                markInteraction.dismissMenu()
                            },
                            onHighlightRequest = {
                                val markInfo = markInteraction.capturedSelectionMark
                                if (markInfo != null) {
                                    viewModel.addStrokeToMark(
                                        markItemInfo = markInfo,
                                        onComplete = {
                                            webViewState.evaluateJs("window.SlaxWebViewBridge.clearSelection()")
                                        }
                                    )
                                } else {
                                    viewModel.strokeHighlight(webViewState) {
                                        webViewState.evaluateJs("window.SlaxWebViewBridge.clearSelection()")
                                    }
                                }
                            },
                            onRemoveHighlightRequest = {
                                val markInfo = markInteraction.capturedSelectionMark ?: return@handleSelectionAction
                                viewModel.removeStrokeFromMark(
                                    markItemInfo = markInfo,
                                    onComplete = {
                                        webViewState.evaluateJs("window.SlaxWebViewBridge.clearSelection()")
                                    }
                                )
                            },
                            onCommentRequest = {
                                markInteraction.dismissMenu()
                                viewModel.captureSelectionForComment(webViewState) { text, markInfo ->
                                    markInteraction.openPanelForNewComment(text, markInfo)
                                    viewModel.commentDelegate.setSelectedMark(markInfo.source)
                                }
                            }
                        )
                        if (actionId == SelectionActionId.COPY) {
                            showCopyToast = true
                            webViewState.evaluateJs("window.SlaxWebViewBridge.clearSelection()")
                        }
                    }
                )
            }
        }

        // 复制成功 Toast（独立于 Popup，菜单隐藏后仍可见）
        CopySuccessToast(
            visible = showCopyToast,
            onDismiss = { showCopyToast = false },
            modifier = Modifier.align(Alignment.TopCenter).padding(top = 120.dp)
        )

        OutlineDialog()

        // 评论面板
        val selectedText = markInteraction.selectedText
        val selectedMarkItemInfo = markInteraction.selectedMark
        val panelComments by viewModel.commentDelegate.panelCommentsFlow.collectAsState()
        var highlightLoading by remember { mutableStateOf(false) }
        CommentPanelSheet(
            highlightedText = selectedText,
            markItemInfo = selectedMarkItemInfo,
            panelComments = panelComments,
            highlightLoading = highlightLoading,
            autoFocusInput = markInteraction.shouldAutoFocus,
            userAvatarUrl = viewModel.userInfo.value?.picture,
            visible = markInteraction.panelVisible,
            onDismiss = {
                markInteraction.dismissPanel()
                viewModel.commentDelegate.setSelectedMark(null)
            },
            onSubmitComment = { comment, replyTarget ->
                val markInfo = markInteraction.selectedMark ?: return@CommentPanelSheet
                viewModel.submitComment(
                    markItemInfo = markInfo,
                    comment = comment,
                    replyMarkId = replyTarget?.markId,
                    onComplete = {}
                )
            },
            onDeleteComment = { markId ->
                viewModel.deleteComment(markId)
            },
            onActionClick = { actionId ->
                when (actionId) {
                    CommentPanelActionId.COPY -> {
                        UIPasteboard.generalPasteboard.string = selectedText
                        showCopyToast = true
                        markInteraction.dismissPanel()
                        viewModel.commentDelegate.setSelectedMark(null)
                        webViewState.evaluateJs("window.SlaxWebViewBridge.clearSelection()")
                    }
                    CommentPanelActionId.HIGHLIGHT -> {
                        val markInfo = markInteraction.selectedMark ?: return@CommentPanelSheet
                        highlightLoading = true
                        viewModel.addStrokeToMark(
                            markItemInfo = markInfo,
                            onComplete = {
                                highlightLoading = false
                                markInteraction.dismissPanel()
                                viewModel.commentDelegate.setSelectedMark(null)
                                webViewState.evaluateJs("window.SlaxWebViewBridge.clearSelection()")
                            }
                        )
                    }
                    CommentPanelActionId.REMOVE_HIGHLIGHT -> {
                        val markInfo = markInteraction.selectedMark ?: return@CommentPanelSheet
                        highlightLoading = true
                        viewModel.removeStrokeFromMark(
                            markItemInfo = markInfo,
                            onComplete = {
                                highlightLoading = false
                                markInteraction.dismissPanel()
                                viewModel.commentDelegate.setSelectedMark(null)
                                webViewState.evaluateJs("window.SlaxWebViewBridge.clearSelection()")
                            }
                        )
                    }
                }
            }
        )
    }

    BookmarkDetailOverlays()
}
