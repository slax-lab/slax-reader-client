package com.slax.reader.ui.bookmark

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.viewModelScope
import androidx.navigation.NavController
import com.slax.reader.data.database.model.UserBookmark
import com.slax.reader.ui.bookmark.components.*
import com.slax.reader.utils.AppWebView
import com.slax.reader.utils.wrapHtmlWithCSS
import kotlinx.coroutines.launch

@Composable
actual fun DetailScreen(
    navController: NavController,
    detailViewModel: BookmarkDetailViewModel,
    detail: UserBookmark,
    screenState: DetailScreenState
) {
    var htmlContent by remember { mutableStateOf<String?>(null) }
    var error by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(detail.id) {
        error = null
        try {
            htmlContent = detailViewModel.getBookmarkContent(detail.id)
            htmlContent = htmlContent?.let { wrapHtmlWithCSS(it) }
        } catch (e: Exception) {
            error = e.message ?: "加载失败"
        }
    }

    val scrollState = rememberScrollState()
    var manuallyVisible by remember { mutableStateOf(true) }

    SideEffect {
        val shouldShow = scrollState.value <= 10
        if (manuallyVisible != shouldShow) {
            manuallyVisible = shouldShow
        }
    }

    var showTagView by remember { mutableStateOf(false) }
    var showOverviewDialog by remember { mutableStateOf(false) }
    var showToolbar by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFFCFCFC))
    ) {
        if (error != null) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(text = "加载失败: $error", color = Color.Red)
            }
            return
        }
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
        ) {
            HeaderContent(
                detail = detail,
                onHeightChanged = {},
                onTagClick = { showTagView = true },
                onOverviewExpand = { showOverviewDialog = true },
                onOverviewBoundsChanged = screenState.onOverviewBoundsChanged,
                detailView = detailViewModel
            )

            htmlContent?.let { content ->
                AppWebView(
                    htmlContent = content,
                    modifier = Modifier.fillMaxWidth(),
                    topContentInsetPx = 0f,
                    onTap = {
                        manuallyVisible = !manuallyVisible
                    },
                    onScrollChange = null
                )
            }
        }

        // 浮动操作栏
        if (manuallyVisible) {
            DetailFloatingActionBar(
                detail = detail,
                detailView = detailViewModel,
                manuallyVisible = manuallyVisible,
                onMoreClick = { showToolbar = true }
            )
        }

        // 标签管理界面 - 在这里获取currentTags并处理更新
        if (showTagView) {
            AnimatedVisibility(
                visible = showTagView,
                enter = fadeIn(animationSpec = tween(300)),
                exit = fadeOut(animationSpec = tween(300))
            ) {
                TagsManageBottomSheet(
                    detailViewModel = detailViewModel,
                    currentTags = detail.metadataObj?.tags,
                    onDismissRequest = { showTagView = false },
                    enableDrag = false,
                    onConfirm = { selectedTags ->
                        detailViewModel.viewModelScope.launch {
                            detailViewModel.updateBookmarkTags(bookmarkId = detail.id, selectedTags.map { it.id })
                        }
                        showTagView = false
                    }
                )
            }
        }

        // Overview 弹窗
        if (showOverviewDialog) {
            OverviewDialog(
                detailView = detailViewModel,
                visible = showOverviewDialog,
                onDismissRequest = { showOverviewDialog = false },
                sourceBounds = screenState.overviewBounds
            )
        }

        // 工具栏
        if (showToolbar) {
            BottomToolbarSheet(
                detail = detail,
                detailView = detailViewModel,
                visible = showToolbar,
                onDismissRequest = { showToolbar = false },
                onIconClick = { pageId, iconIndex ->
                    println("点击了页面 $pageId 的第 ${iconIndex + 1} 个图标")
                    showToolbar = false
                }
            )
        }

        // 导航栏
        if (manuallyVisible) {
            NavigatorBar(
                navController = navController,
                visible = manuallyVisible
            )
        }
    }
}
