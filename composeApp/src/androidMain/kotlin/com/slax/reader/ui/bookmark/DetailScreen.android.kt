package com.slax.reader.ui.bookmark

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewModelScope
import androidx.navigation.NavController
import com.slax.reader.data.database.model.UserBookmark
import com.slax.reader.ui.bookmark.components.*
import com.slax.reader.utils.AppWebView
import com.slax.reader.utils.wrapHtmlWithCSS
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Serializable
data class WebViewMessage(
    val type: String,
    val height: Int? = null,
    val src: String? = null
)

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
    val scrollY by remember { derivedStateOf { scrollState.value.toFloat() } }
    var manuallyVisible by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        snapshotFlow { scrollState.value }
            .collect { scrollValue ->
                val shouldShow = scrollValue <= 10
                if (manuallyVisible != shouldShow) {
                    manuallyVisible = shouldShow
                }
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
                        manuallyVisible = if (scrollY <= 10f) true else !manuallyVisible
                    },
                    onScrollChange = null,
                    onJsMessage = { message ->
                        try {
                            val json = Json { ignoreUnknownKeys = true }
                            val webViewMessage = json.decodeFromString<WebViewMessage>(message)

                            when (webViewMessage.type) {
                                "height" -> {
                                    // 处理高度变化消息
                                    val height = webViewMessage.height
                                    println("[WebView] Content height changed: $height")
                                }
                                "imageClick" -> {
                                    // 处理图片点击消息
                                    val src = webViewMessage.src
                                    println("[WebView] Image clicked: $src")
                                    // 这里可以添加图片预览功能
                                }
                                else -> {
                                    println("[WebView] Unknown message type: ${webViewMessage.type}")
                                }
                            }
                        } catch (e: Exception) {
                            println("[WebView] Failed to parse message: $message, error: ${e.message}")
                        }
                    }
                )
            }
        }

        // 浮动操作栏
        FloatingActionBar(
            detail = detail,
            detailView = detailViewModel,
            visible = manuallyVisible,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 58.dp),
            onMoreClick = { showToolbar = true }
        )

        // 标签管理界面
        if (showTagView) {
            AnimatedVisibility(
                visible = showTagView,
                enter = fadeIn(animationSpec = tween(300)),
                exit = fadeOut(animationSpec = tween(300))
            ) {
                TagsManageBottomSheet(
                    detailViewModel = detailViewModel,
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
