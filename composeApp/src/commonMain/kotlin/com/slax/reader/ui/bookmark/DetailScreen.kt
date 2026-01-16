package com.slax.reader.ui.bookmark

import androidx.compose.runtime.*
import com.slax.reader.ui.bookmark.components.DetailScreenSkeleton
import com.slax.reader.ui.bookmark.states.ScrollInfo
import com.slax.reader.utils.*
import org.koin.compose.viewmodel.koinViewModel

val LocalToolbarVisible = compositionLocalOf<MutableState<Boolean>> {
    error("LocalToolbarVisible not provided")
}

@Composable
fun DetailScreen(bookmarkId: String, onBackClick: (() -> Unit), onNavigateToSubscription: (() -> Unit)? = null) {
    val viewModel = koinViewModel<BookmarkDetailViewModel>()
    val coroutineScope = rememberCoroutineScope()

    val toolbarVisible = remember { mutableStateOf(true) }
    val scrollInfo = remember { mutableStateOf(ScrollInfo(0f, false)) }

    val webViewState = rememberAppWebViewState(coroutineScope)

    LaunchedEffect(bookmarkId) {
        viewModel.bind(bookmarkId)

        viewModel.effects.collect { effect ->
            when (effect) {
                BookmarkDetailEffect.NavigateBack -> onBackClick()
                BookmarkDetailEffect.NavigateToSubscription -> onNavigateToSubscription?.invoke()
                is BookmarkDetailEffect.ScrollToAnchor -> {
                    webViewState.scrollToAnchor(effect.anchor)
                }
            }
        }
    }

    LaunchedEffect(Unit) {
        LifeCycleHelper.lifecycleState.collect { state ->
            when (state) {
                AppLifecycleState.ON_STOP -> {
                    viewModel.onStopRecordContinue(scrollInfo.value.scrollY.toInt())
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
            }
    }

    val contentState by viewModel.contentState.collectAsState()

    if (contentState.htmlContent == null || contentState.isLoading) {
        DetailScreenSkeleton()
        return
    }

    CompositionLocalProvider(LocalToolbarVisible provides toolbarVisible) {
        DetailScreen(
            htmlContent = contentState.htmlContent!!,
            webViewState = webViewState,
            onScrollInfoChanged = { scrollInfo.value = it }
        )
    }
}

@Composable
expect fun DetailScreen(
    htmlContent: String,
    webViewState: AppWebViewState,
    onScrollInfoChanged: (ScrollInfo) -> Unit
)
