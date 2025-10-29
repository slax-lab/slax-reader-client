package com.slax.reader.ui.bookmark

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.viewModelScope
import androidx.navigation.NavController
import com.slax.reader.data.database.model.UserTag
import com.slax.reader.ui.AppViewModel
import com.slax.reader.ui.bookmark.components.*
import kotlinx.coroutines.launch
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel
import kotlin.math.max


data class OverviewViewBounds(
    val x: Float = 0f,
    val y: Float = 0f,
    val width: Float = 0f,
    val height: Float = 0f
)

@Composable
fun DetailScreen(nav: NavController, bookmarkId: String) {
    val detailView = koinViewModel<BookmarkDetailViewModel>()
    val viewModel = koinInject<AppViewModel>()

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        lifecycleOwner.lifecycle.addObserver(viewModel)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(viewModel)
        }
    }
    println("[watch][UI] recomposition DetailScreen")

    val details by detailView.bookmarkDetail.collectAsState()
    val detail = details.firstOrNull()

    val overviewState by detailView.overviewState.collectAsState()
    val overview by remember { derivedStateOf { overviewState.overview } }
    val keyTakeaways by remember { derivedStateOf { overviewState.keyTakeaways } }

    LaunchedEffect(bookmarkId) {
        detailView.setBookmarkId(bookmarkId)
        detailView.loadOverview(bookmarkId)
    }

    var currentTags: List<UserTag> by remember { mutableStateOf(emptyList()) }
    LaunchedEffect(detail?.metadataObj?.tags) {
        if (detail?.metadataObj?.tags != null) {
            currentTags = detailView.getTagNames(detail.metadataObj!!.tags)
        }
    }

    var showTagView by remember { mutableStateOf(false) }
    var showOverviewDialog by remember { mutableStateOf(false) }
    var showToolbar by remember { mutableStateOf(false) }
    var overviewBounds by remember { mutableStateOf(OverviewViewBounds()) }

    // WebView 滚动偏移
    val webViewScrollY = remember { mutableFloatStateOf(0f) }

    // 顶部内容高度 (px)
    var headerMeasuredHeight by remember { mutableFloatStateOf(0f) }

    val animatedTopContentHeight by animateFloatAsState(
        targetValue = headerMeasuredHeight,
        animationSpec = tween(durationMillis = 220),
        label = "TopContentInset"
    )

    var manuallyVisible by remember { mutableStateOf(true) }

    val headerVisible by remember {
        derivedStateOf {
            headerMeasuredHeight == 0f || webViewScrollY.floatValue < headerMeasuredHeight
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFFCFCFC))
    ) {
        BookmarkContentView(
            bookmarkId = bookmarkId,
            topContentInsetPx = animatedTopContentHeight,
            onScrollChange = remember {
                { scrollY ->
                    val clampedScroll = max(scrollY, 0f)
                    webViewScrollY.floatValue = clampedScroll
                    val shouldShow = clampedScroll <= 10f
                    if (manuallyVisible != shouldShow) {
                        manuallyVisible = shouldShow
                    }
                }
            },
            onWebViewTap = {
                manuallyVisible = if (webViewScrollY.floatValue <= 10f) true else !manuallyVisible
            }
        )

        if (headerVisible) {
            HeaderContent(
                detail = detail,
                currentTags = currentTags,
                overview = overview,
                scrollY = webViewScrollY.floatValue,
                onHeightChanged = remember {
                    { height ->
                        headerMeasuredHeight = height
                    }
                },
                onTagClick = remember { { showTagView = true } },
                onOverviewExpand = remember { { showOverviewDialog = true } },
                onOverviewBoundsChanged = remember {
                    { bounds -> overviewBounds = bounds }
                }
            )
        }

        if (detail != null) {
            DetailFloatingActionBar(
                detail = detail,
                detailView = detailView,
                manuallyVisible = manuallyVisible,
                onMoreClick = { showToolbar = true }
            )
        }

        // 标签管理界面
        if (showTagView) {
            TagsManageBottomSheet(
                visible = showTagView,
                onDismissRequest = { showTagView = false },
                enableDrag = false,
                addedTags = currentTags,
                onConfirm = { selectedTags ->
                    currentTags = selectedTags
                    detailView.viewModelScope.launch {
                        detailView.updateBookmarkTags(bookmarkId, selectedTags.map { it.id })
                    }
                }
            )
        }

        // Overview 弹窗
        if (showOverviewDialog) {
            OverviewDialog(
                visible = showOverviewDialog,
                onDismissRequest = { showOverviewDialog = false },
                sourceBounds = overviewBounds,
                overview = overview,
                keyTakeaways = keyTakeaways
            )
        }

        if (detail != null) {
            BottomToolbarSheet(
                detail = detail,
                detailView = detailView,
                visible = showToolbar,
                onDismissRequest = { showToolbar = false },
                onIconClick = { pageIndex, iconIndex ->
                    println("点击了第 ${pageIndex + 1} 页的第 ${iconIndex + 1} 个图标")
                    showToolbar = false
                }
            )
        }

        if (manuallyVisible) {
            NavigatorBar(
                navController = nav,
                visible = manuallyVisible
            )
        }
    }
}
