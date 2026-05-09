package com.slax.reader.ui.bookmark

import androidx.compose.runtime.*
import com.slax.reader.ui.bookmark.components.DetailScreenSkeleton
import com.slax.reader.ui.bookmark.states.LocalMarkInteraction
import com.slax.reader.ui.bookmark.states.MarkInteractionState
import com.slax.reader.ui.bookmark.states.ScrollInfo
import com.slax.reader.utils.*
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import org.koin.compose.viewmodel.koinViewModel
import kotlin.time.Duration.Companion.seconds

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
                is WebViewEvent.MarkItemInfosChanged -> {
                    markInteraction.onMarkItemInfosChanged(event.markItemInfos)
                }
                is WebViewEvent.RequestInfoPack -> {
                    val requestBookmarkId = viewModel.bookmarkId.value
                    coroutineScope.launch {
                        val state = withTimeoutOrNull(3.seconds) {
                            viewModel.bookmarkDelegate.bookmarkDetailState
                                .first { it.displayTitle.isNotEmpty() }
                        } ?: return@launch
                        if (viewModel.bookmarkId.value != requestBookmarkId) return@launch
                        val json = buildJsonObject {
                            put("isStarred", state.isStarred)
                            put("isArchived", state.isArchived)
                            put("displayTitle", state.displayTitle)
                            put("displayTime", state.displayTime)
                            put("metadataUrl", state.metadataUrl)
                        }.toString()
                        webViewState.evaluateJs(
                            "window.SlaxWebViewBridge.receiveInfoPack(`${escapeJsTemplateString(json)}`)"
                        )
                    }
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
