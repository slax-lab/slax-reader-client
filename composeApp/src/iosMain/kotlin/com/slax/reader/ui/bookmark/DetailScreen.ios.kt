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
    val selectionY: Float? = null,
    val markId: String? = null,
    val markItemInfo: String? = null,
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
                        val totalInsetPx = webViewState.topContentInsetPx + 16f * density.density

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
                    val totalInsetPx = webViewState.topContentInsetPx + 16f * density.density
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
        val selectionMenuVisible by LocalSelectionMenuVisible.current
        // selectionYPx 是 hitTest 实时获取的屏幕坐标（UIKit points），转为 Compose px
        val selectionYPx by LocalSelectionYPx.current
        // 提前在 @Composable 上下文中捕获 MutableState 引用，供非 @Composable lambda 使用
        val selectionMenuState = LocalSelectionMenuVisible.current
        val commentPanelState = LocalCommentPanelVisible.current
        val selectedMarkItemInfoState = LocalSelectedMarkItemInfo.current
        val selectedMarkItemInfo by selectedMarkItemInfoState
        val selectionScreenPx = selectionYPx * densityScale

        val minTopPx = (statusBarHeightPx + 20.dp.value * densityScale).toInt()
        val menuGapPx = with(density) { 32.dp.roundToPx() }
        val menuHeightPx = with(density) { 44.dp.roundToPx() }

        // 复制成功 Toast 状态
        var showCopyToast by remember { mutableStateOf(false) }

        // 当前选区是否命中已有划线（用于 SelectionActionBar 的"划线"/"取消划线"切换）
        val selectionHasStroke = selectedMarkItemInfo?.stroke?.isNotEmpty() == true

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
                SelectionActionBar(
                    visible = true,
                    actions = rememberSelectionActions(hasStroke = selectionHasStroke),
                    onActionClick = { actionId ->
                        handleSelectionAction(
                            actionId = actionId,
                            webViewState = webViewState,
                            onDismiss = {
                                // 隐藏选中菜单
                                selectionMenuState.value = false
                            },
                            onHighlightRequest = {
                                val markInfo = selectedMarkItemInfoState.value ?: return@handleSelectionAction
                                viewModel.addStrokeToMark(
                                    webViewState = webViewState,
                                    markItemInfo = markInfo,
                                    onComplete = { updatedInfo ->
                                        selectedMarkItemInfoState.value = updatedInfo
                                    }
                                )
                            },
                            onRemoveHighlightRequest = {
                                // 删除已有 mark 的划线
                                val markInfo = selectedMarkItemInfoState.value ?: return@handleSelectionAction
                                viewModel.removeStrokeFromMark(
                                    webViewState = webViewState,
                                    markItemInfo = markInfo,
                                    onComplete = { updatedInfo ->
                                        selectedMarkItemInfoState.value = updatedInfo
                                    }
                                )
                            },
                            onCommentRequest = {
                                // 显示评论面板
                                commentPanelState.value = true
                            }
                        )
                        if (actionId == SelectionActionId.COPY) {
                            showCopyToast = true
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
        val commentPanelVisible by commentPanelState
        val selectedText by LocalSelectedText.current
        var highlightLoading by remember { mutableStateOf(false) }
        var commentLoading by remember { mutableStateOf(false) }
        CommentPanelSheet(
            highlightedText = selectedText,
            markItemInfo = selectedMarkItemInfo,
            highlightLoading = highlightLoading,
            commentLoading = commentLoading,
            userAvatarUrl = viewModel.userInfo.value?.picture,
            visible = commentPanelVisible,
            onDismiss = {
                selectedMarkItemInfoState.value = null
                commentPanelState.value = false
            },
            onActionClick = { actionId ->
                when (actionId) {
                    CommentPanelActionId.COPY -> {
                        // 将选中文本复制到系统剪贴板
                        UIPasteboard.generalPasteboard.string = selectedText
                    }
                    CommentPanelActionId.HIGHLIGHT -> {
                        // 为已有 mark 添加划线
                        val markInfo = selectedMarkItemInfoState.value ?: return@CommentPanelSheet
                        highlightLoading = true
                        viewModel.addStrokeToMark(
                            webViewState = webViewState,
                            markItemInfo = markInfo,
                            onComplete = { updatedInfo ->
                                selectedMarkItemInfoState.value = updatedInfo
                                highlightLoading = false
                            }
                        )
                    }
                    CommentPanelActionId.REMOVE_HIGHLIGHT -> {
                        // 删除已有 mark 的划线
                        val markInfo = selectedMarkItemInfoState.value ?: return@CommentPanelSheet
                        highlightLoading = true
                        viewModel.removeStrokeFromMark(
                            webViewState = webViewState,
                            markItemInfo = markInfo,
                            onComplete = { updatedInfo ->
                                selectedMarkItemInfoState.value = updatedInfo
                                highlightLoading = false
                            }
                        )
                    }
                }
            },
            onSubmitComment = { comment, parentId ->
                val markInfo = selectedMarkItemInfoState.value ?: return@CommentPanelSheet
                commentLoading = true
                viewModel.addCommentToMark(
                    webViewState = webViewState,
                    markItemInfo = markInfo,
                    comment = comment,
                    parentId = parentId,
                    onComplete = { updatedInfo ->
                        selectedMarkItemInfoState.value = updatedInfo
                        commentLoading = false
                    }
                )
            }
        )
    }

    BookmarkDetailOverlays()
}
