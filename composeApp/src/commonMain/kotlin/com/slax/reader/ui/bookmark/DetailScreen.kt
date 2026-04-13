package com.slax.reader.ui.bookmark

import androidx.compose.runtime.*
import com.slax.reader.ui.bookmark.components.DetailScreenSkeleton
import com.slax.reader.ui.bookmark.states.ScrollInfo
import com.slax.reader.utils.*
import org.koin.compose.viewmodel.koinViewModel

val LocalToolbarVisible = compositionLocalOf<MutableState<Boolean>> {
    error("LocalToolbarVisible not provided")
}

/** 文本选中菜单是否可见 */
val LocalSelectionMenuVisible = compositionLocalOf<MutableState<Boolean>> {
    error("LocalSelectionMenuVisible not provided")
}

/** 当前选中的文本内容 */
val LocalSelectedText = compositionLocalOf<MutableState<String>> {
    error("LocalSelectedText not provided")
}

/** 选中文本在 WebView 视口中的 Y 坐标（px） */
val LocalSelectionYPx = compositionLocalOf<MutableFloatState> {
    error("LocalSelectionYPx not provided")
}

/** 评论面板是否可见 */
val LocalCommentPanelVisible = compositionLocalOf<MutableState<Boolean>> {
    error("LocalCommentPanelVisible not provided")
}

/** 当前点击划线对应的 MarkItemInfo */
val LocalSelectedMarkItemInfo = compositionLocalOf<MutableState<BridgeMarkItemInfo?>> {
    error("LocalSelectedMarkItemInfo not provided")
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
    val selectionMenuVisible = remember { mutableStateOf(false) }
    val selectedText = remember { mutableStateOf("") }
    val selectionYPx = remember { mutableFloatStateOf(0f) }
    val commentPanelVisible = remember { mutableStateOf(false) }
    val selectedMarkItemInfo = remember { mutableStateOf<BridgeMarkItemInfo?>(null) }

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
                    selectedText.value = event.text
                    selectionYPx.floatValue = event.selectionY
                    selectionMenuVisible.value = true
                }
                is WebViewEvent.TextDeselected -> {
                    selectionMenuVisible.value = false
                    selectedText.value = ""
                    selectionYPx.floatValue = 0f
                }
                is WebViewEvent.PageLoaded -> {
                    // 启动划线选区监听（初始化 JS 侧的 markManager），再拉取并绘制划线数据
                    val currentUserId = viewModel.getCurrentUserId() ?: ""
                    webViewState.evaluateJs("window.SlaxWebViewBridge.startSelectionMonitoring('body', ${currentUserId.toLongOrNull()})")
                    viewModel.loadAndDrawMarks()
                }
                is WebViewEvent.MarkClicked -> {
                    // 点击已有划线时，显示评论面板并展示该划线的文本内容与评论数据
                    selectedText.value = event.text
                    selectedMarkItemInfo.value = event.markItemInfo
                    commentPanelVisible.value = true
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
        LocalSelectionMenuVisible provides selectionMenuVisible,
        LocalSelectedText provides selectedText,
        LocalSelectionYPx provides selectionYPx,
        LocalCommentPanelVisible provides commentPanelVisible,
        LocalSelectedMarkItemInfo provides selectedMarkItemInfo,
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
