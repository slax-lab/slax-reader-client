package com.slax.reader.ui.bookmark

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewModelScope
import com.slax.reader.data.database.model.UserBookmark
import com.slax.reader.ui.bookmark.components.*
import com.slax.reader.utils.AppWebView
import com.slax.reader.utils.wrapHtmlWithCSS
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlin.math.max

@Serializable
data class WebViewMessage(
    val type: String,
    val height: Int? = null,
    val src: String? = null,
    val allImages: List<String>? = null
)

@Composable
actual fun DetailScreen(
    detailViewModel: BookmarkDetailViewModel,
    detail: UserBookmark,
    screenState: DetailScreenState,
    onBackClick: (() -> Unit)
) {
    var htmlContent by remember { mutableStateOf<String?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    val backClickHandler = remember { onBackClick }

    LaunchedEffect(detail.id) {
        error = null
        try {
            htmlContent = detailViewModel.getBookmarkContent(detail.id)
            htmlContent = htmlContent?.let { wrapHtmlWithCSS(it) }
        } catch (e: Exception) {
            error = e.message ?: "加载失败"
        }
    }

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

    var showTagView by remember { mutableStateOf(false) }
    var showOverviewDialog by remember { mutableStateOf(false) }
    var showToolbar by remember { mutableStateOf(false) }

    // 图片浏览器状态
    var showImageViewer by remember { mutableStateOf(false) }
    var currentImageUrl by remember { mutableStateOf("") }
    var allImageUrls by remember { mutableStateOf<List<String>>(emptyList()) }

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


        htmlContent?.let { content ->
            key(detail.id) {
                AppWebView(
                    htmlContent = content,
                    modifier = Modifier.fillMaxSize(),
                    topContentInsetPx = animatedTopContentHeight,
                    onTap = {
                        manuallyVisible = if (webViewScrollY.floatValue <= 10f) true else !manuallyVisible
                    },
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
                    onJsMessage = { message ->
                        try {
                            val json = Json { ignoreUnknownKeys = true }
                            val webViewMessage = json.decodeFromString<WebViewMessage>(message)

                            when (webViewMessage.type) {
                                "imageClick" -> {
                                    val src = webViewMessage.src
                                    val allImages = webViewMessage.allImages

                                    if (src != null && !allImages.isNullOrEmpty()) {
                                        currentImageUrl = src
                                        allImageUrls = allImages
                                        showImageViewer = true
                                    }
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

        if (headerVisible) {
            Box(
                modifier = Modifier.graphicsLayer {
                    translationY = -webViewScrollY.floatValue
                }
            ) {
                HeaderContent(
                    detail = detail,
                    detailView = detailViewModel,
                    onHeightChanged = remember {
                        { height ->
                            headerMeasuredHeight = height
                        }
                    },
                    onTagClick = { showTagView = true },
                    onOverviewExpand = { showOverviewDialog = true },
                    onOverviewBoundsChanged = screenState.onOverviewBoundsChanged
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
            TagsManageBottomSheet(
                detailViewModel = detailViewModel,
                onDismissRequest = { showTagView = false },
                enableDrag = false,
                onConfirm = { selectedTags ->
                    detailViewModel.viewModelScope.launch {
                        detailViewModel.updateBookmarkTags(detail.id, selectedTags.map { it.id })
                    }
                    showTagView = false
                }
            )
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

        if (manuallyVisible) {
            NavigatorBar(
                visible = manuallyVisible,
                onBackClick = backClickHandler
            )
        }

        // 图片浏览器
        ImageViewer(
            imageUrls = allImageUrls,
            initialImageUrl = currentImageUrl,
            visible = showImageViewer,
            onDismiss = {
                println("[ImageViewer] Dismissed")
                showImageViewer = false
            }
        )
    }
}
