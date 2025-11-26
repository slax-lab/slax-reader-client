package com.slax.reader.ui.bookmark

import android.annotation.SuppressLint
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
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewModelScope
import com.slax.reader.data.database.model.UserBookmark
import com.slax.reader.data.model.PositionInfo
import com.slax.reader.data.model.SelectionData
import com.slax.reader.data.model.MarkData
import com.slax.reader.ui.bookmark.components.*
import com.slax.reader.utils.AppLifecycleState
import com.slax.reader.utils.AppWebView
import com.slax.reader.utils.LifeCycleHelper
import com.slax.reader.utils.wrapHtmlWithCSS
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Serializable
data class WebViewMessage(
    val type: String,
    // For content height
    val height: Int? = null,
    // For image clicks
    val src: String? = null,
    val allImages: List<String>? = null,
    // For selection bridge
    val data: String? = null,
    val position: String? = null,
    val markId: String? = null,
    val success: Boolean? = null,
    val error: String? = null
)

@SuppressLint("UseKtx")
@Composable
actual fun DetailScreen(
    detailViewModel: BookmarkDetailViewModel,
    detail: UserBookmark,
    screenState: DetailScreenState,
    onBackClick: (() -> Unit),
) {
    val context = androidx.compose.ui.platform.LocalContext.current
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

    val bottomThresholdPx = with(LocalDensity.current) { 100.dp.toPx() }

    LaunchedEffect(Unit) {
        LifeCycleHelper.lifecycleState.collect { state ->
            when (state) {
                AppLifecycleState.ON_STOP -> {
                    detailViewModel.viewModelScope.launch {
                        detailViewModel.recordContinueBookmark(scrollState.value)
                    }
                }

                AppLifecycleState.ON_RESUME -> {
                    detailViewModel.viewModelScope.launch {
                        detailViewModel.clearContinueBookmark()
                    }
                }

                else -> {
                }
            }
        }
    }

    // 是否接近底部
    val isNearBottom by remember {
        derivedStateOf {
            scrollState.maxValue - scrollState.value < bottomThresholdPx
        }
    }

    LaunchedEffect(Unit) {
        snapshotFlow { isNearBottom to scrollState.value }
            .collect { (nearBottom, scrollValue) ->
                manuallyVisible = when {
                    nearBottom -> true  // 在底部区域，强制显示
                    scrollValue <= 10 -> true  // 在顶部区域，显示
                    else -> false  // 中间区域，自动隐藏
                }
            }
    }

    var showTagView by remember { mutableStateOf(false) }
    var showOverviewDialog by remember { mutableStateOf(false) }
    var showToolbar by remember { mutableStateOf(false) }

    // 图片浏览器状态
    var showImageViewer by remember { mutableStateOf(false) }
    var currentImageUrl by remember { mutableStateOf("") }
    var allImageUrls by remember { mutableStateOf<List<String>>(emptyList()) }

    // 选择桥接状态
    var showSelectionMenu by remember { mutableStateOf(false) }
    var selectionData by remember { mutableStateOf<SelectionData?>(null) }
    var selectionPosition by remember { mutableStateOf<PositionInfo?>(null) }

    var showMarkMenu by remember { mutableStateOf(false) }
    var clickedMarkId by remember { mutableStateOf<String?>(null) }
    var markData by remember { mutableStateOf<MarkData?>(null) }
    var markPosition by remember { mutableStateOf<PositionInfo?>(null) }

    var showCommentDialog by remember { mutableStateOf(false) }
    var commentSelectionData by remember { mutableStateOf<SelectionData?>(null) }

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
                        // 只在非底部且非顶部区域才切换显示状态
                        if (!isNearBottom && scrollY > 10f) {
                            manuallyVisible = !manuallyVisible
                        }
                    },
                    onScrollChange = null,
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

                                "textSelected" -> {
                                    val dataStr = webViewMessage.data
                                    val positionStr = webViewMessage.position

                                    if (dataStr != null && positionStr != null) {
                                        try {
                                            selectionData = json.decodeFromString<SelectionData>(dataStr)
                                            selectionPosition = json.decodeFromString<PositionInfo>(positionStr)
                                            showSelectionMenu = true
                                        } catch (e: Exception) {
                                            println("[WebView] Failed to parse selection data: ${e.message}")
                                        }
                                    }
                                }

                                "markClicked" -> {
                                    val markId = webViewMessage.markId
                                    val dataStr = webViewMessage.data
                                    val positionStr = webViewMessage.position

                                    if (markId != null && dataStr != null && positionStr != null) {
                                        try {
                                            clickedMarkId = markId
                                            markData = json.decodeFromString<MarkData>(dataStr)
                                            markPosition = json.decodeFromString<PositionInfo>(positionStr)
                                            showMarkMenu = true
                                        } catch (e: Exception) {
                                            println("[WebView] Failed to parse mark data: ${e.message}")
                                        }
                                    }
                                }

                                "bridgeInitialized" -> {
                                    println("[WebView] Selection bridge initialized")
                                    // 桥接已初始化，可以在这里加载标记
                                    // TODO: 实现加载标记的逻辑
                                }

                                "markRendered" -> {
                                    val markId = webViewMessage.markId
                                    val success = webViewMessage.success
                                    println("[WebView] Mark rendered: $markId, success: $success")
                                }

                                "error" -> {
                                    val error = webViewMessage.error
                                    println("[WebView] Bridge error: $error")
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

        // 选择菜单
        if (showSelectionMenu && selectionPosition != null) {
            SelectionMenu(
                position = selectionPosition!!,
                visible = showSelectionMenu,
                onCopyClick = {
                    // 复制文本到剪贴板
                    selectionData?.selection?.let { selections ->
                        val textItems = selections.mapNotNull { item ->
                            when (item) {
                                is com.slax.reader.data.model.SelectionItem.Text -> item.text
                                else -> null
                            }
                        }
                        val combinedText = textItems.joinToString("")
                        if (combinedText.isNotEmpty()) {
                            val clipboardManager = context.getSystemService(android.content.Context.CLIPBOARD_SERVICE) as? android.content.ClipboardManager
                            clipboardManager?.setPrimaryClip(
                                android.content.ClipData.newPlainText("selected_text", combinedText)
                            )
                        }
                    }
                },
                onHighlightClick = {
                    // 创建划线标记
                    selectionData?.let { data ->
                        // TODO: 获取当前用户 ID
                        val userId = 1
                        detailViewModel.createHighlightMark(data, userId)
                    }
                },
                onCommentClick = {
                    commentSelectionData = selectionData
                    showCommentDialog = true
                },
                onDismiss = {
                    showSelectionMenu = false
                }
            )
        }

        // 标记菜单
        if (showMarkMenu && markPosition != null) {
            MarkMenu(
                markId = clickedMarkId ?: "",
                position = markPosition!!,
                visible = showMarkMenu,
                onViewClick = {
                    // TODO: 查看标记详情
                    println("[Mark] View clicked: $clickedMarkId")
                },
                onDeleteClick = {
                    // 删除标记
                    clickedMarkId?.let { markId ->
                        detailViewModel.deleteMark(markId)
                    }
                },
                onDismiss = {
                    showMarkMenu = false
                }
            )
        }

        // 评论对话框
        CommentDialog(
            visible = showCommentDialog,
            onConfirm = { commentText ->
                // 创建评论标记
                commentSelectionData?.let { data ->
                    // TODO: 获取当前用户 ID
                    val userId = 1
                    detailViewModel.createCommentMark(data, commentText, userId)
                }
                showCommentDialog = false
                commentSelectionData = null
            },
            onDismiss = {
                showCommentDialog = false
                commentSelectionData = null
            }
        )

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
                visible = showTagView,
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
                }
            )
        }

        // 导航栏
        if (manuallyVisible) {
            NavigatorBar(
                visible = manuallyVisible,
                onBackClick = onBackClick
            )
        }

        // 图片浏览器
        if (showImageViewer) {
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
}
