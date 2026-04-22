package com.slax.reader.ui.bookmark

import androidx.compose.runtime.*
import com.slax.reader.ui.bookmark.components.DetailScreenSkeleton
import com.slax.reader.ui.bookmark.states.LocalMarkInteraction
import com.slax.reader.ui.bookmark.states.MarkInteractionState
import com.slax.reader.ui.bookmark.states.ScrollInfo
import com.slax.reader.utils.*
import org.koin.compose.viewmodel.koinViewModel

val LocalToolbarVisible = compositionLocalOf<MutableState<Boolean>> {
    error("LocalToolbarVisible not provided")
}

sealed interface DetailScreenEvent {
    data object BackClick : DetailScreenEvent
    data object NavigateToSubscription : DetailScreenEvent
    data class NavigateToFeedback(val params: FeedbackPageParams) : DetailScreenEvent
}

@Composable
fun DetailScreen(bookmarkId: String, onEvent: (DetailScreenEvent) -> Unit) {
    val viewModel = koinViewModel<BookmarkDetailViewModel>()
    val coroutineScope = rememberCoroutineScope()

    val toolbarVisible = remember { mutableStateOf(true) }
    val scrollInfo = remember { mutableStateOf(ScrollInfo(0f, false)) }
    val markInteraction = remember { MarkInteractionState() }

    val webViewState = rememberAppWebViewState(coroutineScope)

    LaunchedEffect(bookmarkId) {
        viewModel.bind(bookmarkId)

        viewModel.effects.collect { effect ->
            when (effect) {
                BookmarkDetailEffect.NavigateBack -> onEvent(DetailScreenEvent.BackClick)
                BookmarkDetailEffect.NavigateToSubscription -> onEvent(DetailScreenEvent.NavigateToSubscription)
                is BookmarkDetailEffect.NavigateToFeedback -> {
                    onEvent(DetailScreenEvent.NavigateToFeedback(effect.params))
                }
                is BookmarkDetailEffect.ScrollToAnchor -> {
                    webViewState.scrollToAnchor(effect.anchor)
                }
                is BookmarkDetailEffect.DrawMarks -> {
                    webViewState.evaluateJs(
                        "window.SlaxWebViewBridge.drawMarks(`${escapeJsTemplateString(effect.markDetailJson)}`)"
                    )
                }
            }
        }
    }

    LaunchedEffect(Unit) {
        LifeCycleHelper.lifecycleState.collect { state ->
            when (state) {
                AppLifecycleState.ON_STOP -> {
                    viewModel.flushReadPosition()
                    viewModel.flushOutlineScrollPosition()
                    viewModel.onStopRecordContinue()
                }
                AppLifecycleState.ON_RESUME -> {
                    viewModel.onResumeClearContinue()
                }
                else -> {}
            }
        }
    }

    LaunchedEffect(webViewState) {
        webViewState.events.collect { event ->
            when (event) {
                is WebViewEvent.ImageClick -> {
                    viewModel.overlayDelegate.onWebViewImageClick(event.src, event.allImages)
                }
                is WebViewEvent.Tap -> {
                    val info = scrollInfo.value
                    if (!info.isNearBottom && info.scrollY > 10f) {
                        toolbarVisible.value = !toolbarVisible.value
                    }
                }
                is WebViewEvent.RefreshContent -> {
                    viewModel.refreshContent()
                }
                is WebViewEvent.Feedback -> {
                    println("feedback")
                }
                is WebViewEvent.TextSelected -> {
                    markInteraction.onTextSelected(event.text, event.selectionY, event.markItemInfo)
                }
                is WebViewEvent.TextDeselected -> {
                    markInteraction.onTextDeselected()
                }
                is WebViewEvent.PageLoaded -> {
                    val userIdLong = viewModel.commentDelegate.currentUserIdLong
                    webViewState.evaluateJs("window.SlaxWebViewBridge.startSelectionMonitoring('body', $userIdLong)")
                    viewModel.startObservingMarks()
                }
                is WebViewEvent.MarkClicked -> {
                    val info = event.markItemInfo ?: return@collect
                    markInteraction.onMarkClicked(event.text, info)
                    viewModel.commentDelegate.setSelectedMark(info.source)
                }
                is WebViewEvent.SelectionMarkItemInfo -> {
                    // 选中文本时，Bridge 通知当前选区对应的 MarkItemInfo（用于 SelectionActionBar 判断划线状态）
                    markInteraction.onSelectionMarkInfoChanged(event.markItemInfo)
                }
                is WebViewEvent.MarkItemInfosChanged -> {
                    // JS 端 markItemInfos 列表变化时，同步更新 selectedMark 的 stroke/comments 数据
                    markInteraction.onMarkItemInfosChanged(event.markItemInfos)
                }
                else -> {}
            }
        }
    }

    LaunchedEffect(Unit) {
        snapshotFlow { scrollInfo.value }
            .collect { info ->
                toolbarVisible.value = when {
                    info.isNearBottom -> true
                    info.scrollY <= 10f -> true
                    else -> false
                }
                viewModel.saveReadPosition(info.scrollY)
            }
    }

    val contentState by viewModel.contentState.collectAsState()

    if (contentState.htmlContent == null || contentState.isLoading) {
        DetailScreenSkeleton()
        return
    }

    CompositionLocalProvider(
        LocalToolbarVisible provides toolbarVisible,
        LocalMarkInteraction provides markInteraction,
    ) {
        DetailScreen(
            bookmarkId = bookmarkId,
            htmlContent = contentState.htmlContent!!,
            webViewState = webViewState,
            onScrollInfoChanged = { scrollInfo.value = it }
        )
    }
}

@Composable
expect fun DetailScreen(
    bookmarkId: String,
    htmlContent: String,
    webViewState: AppWebViewState,
    onScrollInfoChanged: (ScrollInfo) -> Unit
)
