package com.slax.reader.ui.bookmark

import androidx.compose.runtime.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.slax.reader.ui.bookmark.components.DetailScreenSkeleton
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
    val detailState by viewModel.bookmarkDelegate.bookmarkDetailState.collectAsState()

    if (contentState.htmlContent == null || contentState.isLoading) {
        DetailScreenSkeleton()
        return
    }

    // 删除加载对话框
    if (detailState.isDeleting) {
        Dialog(
            onDismissRequest = { },
            properties = DialogProperties(dismissOnBackPress = false, dismissOnClickOutside = false)
        ) {
            Box(
                modifier = Modifier
                    .size(200.dp)
                    .background(Color.White, shape = RoundedCornerShape(8.dp))
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    CircularProgressIndicator(
                        color = Color(0xFF16B998),
                        strokeWidth = 3.dp,
                        modifier = Modifier.size(48.dp)
                    )
                    Text(
                        text = "删除中...",
                        style = TextStyle(fontSize = 16.sp)
                    )
                }
            }
        }
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
