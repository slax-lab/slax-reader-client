package com.slax.reader.ui.bookmark.components

import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import com.slax.reader.ui.bookmark.BookmarkDetailViewModel
import com.slax.reader.ui.bookmark.states.MarkInteractionState
import com.slax.reader.utils.AppWebViewState

@Composable
fun BoxScope.SelectionMenuCommentPanel(
    markInteraction: MarkInteractionState,
    webViewState: AppWebViewState,
    viewModel: BookmarkDetailViewModel,
    densityScale: Float,
    containerHeightPx: Float,
    minTopPx: Int,
    onCopyText: (String) -> Unit,
    onHighlightAction: () -> Unit,
    onSubmitCommentComplete: () -> Unit,
) {
    var showCopyToast by remember { mutableStateOf(false) }
    val onCopyToastCallback = remember { { showCopyToast = true } }

    SelectionPopup(
        markInteraction = markInteraction,
        webViewState = webViewState,
        viewModel = viewModel,
        densityScale = densityScale,
        containerHeightPx = containerHeightPx,
        minTopPx = minTopPx,
        onCopyToast = onCopyToastCallback,
        onHighlightAction = onHighlightAction,
    )

    CopySuccessToast(
        visible = showCopyToast,
        onDismiss = remember { { showCopyToast = false } },
        modifier = Modifier.align(Alignment.TopCenter).padding(top = 120.dp)
    )

    CommentPanelContent(
        markInteraction = markInteraction,
        webViewState = webViewState,
        viewModel = viewModel,
        onCopyText = onCopyText,
        onCopyToast = onCopyToastCallback,
        onSubmitCommentComplete = onSubmitCommentComplete,
    )
}

@Composable
private fun SelectionPopup(
    markInteraction: MarkInteractionState,
    webViewState: AppWebViewState,
    viewModel: BookmarkDetailViewModel,
    densityScale: Float,
    containerHeightPx: Float,
    minTopPx: Int,
    onCopyToast: () -> Unit,
    onHighlightAction: () -> Unit,
) {
    val density = LocalDensity.current
    val menuGapPx = with(density) { 32.dp.roundToPx() }
    val menuHeightPx = with(density) { 44.dp.roundToPx() }

    val selectionScreenY = markInteraction.selectionY * densityScale
    val showMenu = markInteraction.menuVisible && selectionScreenY > 0f && selectionScreenY < containerHeightPx

    if (showMenu) {
        val touchY = selectionScreenY.toInt()
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
                            onHighlightAction()
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
                        onCopyToast()
                        webViewState.evaluateJs("window.SlaxWebViewBridge.clearSelection()")
                    }
                }
            )
        }
    }
}

@Composable
private fun CommentPanelContent(
    markInteraction: MarkInteractionState,
    webViewState: AppWebViewState,
    viewModel: BookmarkDetailViewModel,
    onCopyText: (String) -> Unit,
    onCopyToast: () -> Unit,
    onSubmitCommentComplete: () -> Unit,
) {
    val selectedText = markInteraction.selectedText
    val selectedMarkItemInfo = markInteraction.selectedMark
    val panelComments by viewModel.commentDelegate.panelCommentsFlow.collectAsState()
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
            markInteraction.dismissPanelAnimated(coroutineScope) { viewModel.commentDelegate.setSelectedMark(null) }
        },
        onSubmitComment = { comment, replyTarget ->
            val markInfo = markInteraction.selectedMark ?: return@CommentPanelSheet
            viewModel.submitComment(
                markItemInfo = markInfo,
                comment = comment,
                replyMarkId = replyTarget?.markId,
                onComplete = onSubmitCommentComplete
            )
        },
        onDeleteComment = { markId ->
            viewModel.deleteComment(markId)
        },
        onActionClick = { actionId ->
            when (actionId) {
                CommentPanelActionId.COPY -> {
                    onCopyText(selectedText)
                    onCopyToast()
                    markInteraction.dismissPanelAnimated(coroutineScope) { viewModel.commentDelegate.setSelectedMark(null) }
                    webViewState.evaluateJs("window.SlaxWebViewBridge.clearSelection()")
                }
                CommentPanelActionId.HIGHLIGHT -> {
                    val markInfo = markInteraction.selectedMark ?: return@CommentPanelSheet
                    highlightLoading = true
                    viewModel.addStrokeToMark(
                        markItemInfo = markInfo,
                        onComplete = {
                            highlightLoading = false
                            markInteraction.dismissPanelAnimated(coroutineScope) { viewModel.commentDelegate.setSelectedMark(null) }
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
                            markInteraction.dismissPanelAnimated(coroutineScope) { viewModel.commentDelegate.setSelectedMark(null) }
                            webViewState.evaluateJs("window.SlaxWebViewBridge.clearSelection()")
                        }
                    )
                }
            }
        }
    )
}
