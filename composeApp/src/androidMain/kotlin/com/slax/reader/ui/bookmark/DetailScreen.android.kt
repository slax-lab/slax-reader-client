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
import com.slax.reader.ui.bookmark.states.LocalMarkInteraction
import com.slax.reader.ui.bookmark.states.ScrollInfo
import com.slax.reader.utils.AppWebView
import com.slax.reader.utils.AppWebViewState
import com.slax.reader.utils.WebViewEvent
import com.slax.reader.utils.wrapBookmarkDetailHtml
import kotlinx.serialization.Serializable
import org.koin.compose.viewmodel.koinViewModel
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.platform.ClipEntry
import android.content.ClipData
import kotlinx.coroutines.launch

@Serializable
data class WebViewMessage(
    val type: String,
    val height: Int? = null,
    val src: String? = null,
    val allImages: List<String>? = null,
    val percentage: Double? = null,
    val text: String? = null,
    val selectionY: Float? = null,
    val markId: String? = null,
    val markItemInfos: String? = null,
    val data: String? = null,
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
        val markInteraction = LocalMarkInteraction.current
        val selectionMenuVisible = markInteraction.menuVisible
        val selectionYPx = markInteraction.selectionY
        val density = LocalDensity.current
        val minTopPx = with(density) { 60.dp.roundToPx() }
        val menuGapPx = with(density) { 32.dp.roundToPx() }
        val menuHeightPx = with(density) { 44.dp.roundToPx() }

        // 复制成功 Toast 状态
        var showCopyToast by remember { mutableStateOf(false) }

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
                                viewModel.strokeHighlight(webViewState)
                            },
                            onRemoveHighlightRequest = {
                                val markInfo = markInteraction.capturedSelectionMark ?: return@handleSelectionAction
                                viewModel.removeStrokeFromMark(
                                    markItemInfo = markInfo,
                                    onComplete = {
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
        val clipboard = LocalClipboard.current
        val coroutineScope = rememberCoroutineScope()
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
                )
            },
            onDeleteComment = { markId ->
                viewModel.deleteComment(markId)
            },
            onActionClick = { actionId ->
                when (actionId) {
                    CommentPanelActionId.COPY -> {
                        val clipData = ClipData.newPlainText("slax_highlight", selectedText)
                        coroutineScope.launch {
                            clipboard.setClipEntry(ClipEntry(clipData))
                        }
                    }
                    CommentPanelActionId.HIGHLIGHT -> {
                        val markInfo = markInteraction.selectedMark ?: return@CommentPanelSheet
                        highlightLoading = true
                        viewModel.addStrokeToMark(
                            markItemInfo = markInfo,
                            onComplete = {
                                highlightLoading = false
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
                            }
                        )
                    }
                }
            }
        )
    }

    BookmarkDetailOverlays()
}
