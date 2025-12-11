package com.slax.reader.ui.bookmark

import android.annotation.SuppressLint
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.FrameRateCategory
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.preferredFrameRate
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewModelScope
import com.slax.reader.const.component.EditNameDialog
import com.slax.reader.data.database.model.UserBookmark
import com.slax.reader.ui.bookmark.components.*
import com.slax.reader.utils.AppLifecycleState
import com.slax.reader.utils.AppWebView
import com.slax.reader.utils.LifeCycleHelper
import com.slax.reader.utils.generateHtmlWithExternalResources
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Serializable
data class WebViewMessage(
    val type: String,
    val height: Int? = null,
    val src: String? = null,
    val allImages: List<String>? = null,
    val position: Int? = null  // 新增：滚动位置
)

@SuppressLint("UseKtx")
@Composable
actual fun DetailScreen(
    detailViewModel: BookmarkDetailViewModel,
    detail: UserBookmark,
    screenState: DetailScreenState,
    onBackClick: (() -> Unit),
) {
    var htmlContent by remember { mutableStateOf<String?>(null) }
    var error by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(detail.id) {
        error = null
        try {
            htmlContent = detailViewModel.getBookmarkContent(detail.id)
            // 使用新的HTML生成函数（支持外部CSS和JS引用）
            htmlContent = htmlContent?.let { generateHtmlWithExternalResources(it) }
        } catch (e: Exception) {
            error = e.message ?: "加载失败"
        }
    }

    val scrollState = rememberScrollState()
    val scrollY by remember { derivedStateOf { scrollState.value.toFloat() } }
    var manuallyVisible by remember { mutableStateOf(true) }

    // 获取设备密度（用于 CSS pixels → 物理像素 转换）
    val density = LocalDensity.current.density

    // 获取屏幕高度（用于计算滚动偏移）
    val configuration = LocalConfiguration.current
    val screenHeightPx = with(LocalDensity.current) { configuration.screenHeightDp.dp.toPx() }

    // 记录 HeaderContent 的高度（px）
    var headerHeightPx by remember { mutableFloatStateOf(0f) }

    // JS 命令状态（用于执行 WebView 中的 JavaScript）
    var jsCommand by remember { mutableStateOf<String?>(null) }
    val coroutineScope = rememberCoroutineScope()

    // 监听锚点滚动事件
    LaunchedEffect(Unit) {
        detailViewModel.scrollToAnchorEvent.collect { anchorText ->
            println("[DetailScreen Android] 收到锚点滚动事件: $anchorText")
            jsCommand = "window.SlaxWebViewBridge.scrollToAnchor('$anchorText')"
            // 执行后清空命令
            kotlinx.coroutines.delay(100)
            jsCommand = null
        }
    }

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
    var showEditNameDialog by remember { mutableStateOf(false) }
    var showOutlineDialog by remember { mutableStateOf(false) }

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
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
        ) {
            HeaderContent(
                detail = detail,
                onHeightChanged = { height ->
                    headerHeightPx = height
                    println("[Android DetailScreen] HeaderContent 高度: $height px")
                },
                onTagClick = { showTagView = true },
                onOverviewExpand = { showOverviewDialog = true },
                onOverviewBoundsChanged = screenState.onOverviewBoundsChanged,
                detailView = detailViewModel
            )

            htmlContent?.let { content ->
                AppWebView(
                    htmlContent = content,
                    modifier = Modifier
                        .fillMaxWidth()
                        .preferredFrameRate(FrameRateCategory.High),
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

                                "scrollToPosition" -> {
                                    val webViewPositionCssPx = webViewMessage.position ?: 0
                                    // WebView 的 CSS pixels 转换为物理像素
                                    val webViewPositionPx = webViewPositionCssPx * density
                                    // Column 的滚动位置 = HeaderContent 高度（物理像素）+ WebView 内部位置（物理像素）+ 屏幕高度的1/4
                                    val targetPosition = (headerHeightPx + webViewPositionPx + screenHeightPx / 4).toInt()
                                    println("[Android WebView] WebView CSS pixels: $webViewPositionCssPx")
                                    println("[Android WebView] 设备密度: $density")
                                    println("[Android WebView] WebView 物理像素: $webViewPositionPx px")
                                    println("[Android WebView] HeaderContent 高度: $headerHeightPx px")
                                    println("[Android WebView] 目标滚动位置: $targetPosition px")
                                    // 使用 Compose 的 ScrollState 滚动
                                    coroutineScope.launch {
                                        scrollState.animateScrollTo(targetPosition)
                                    }
                                }

                                else -> {
                                    println("[WebView] Unknown message type: ${webViewMessage.type}")
                                }
                            }
                        } catch (e: Exception) {
                            println("[WebView] Failed to parse message: $message, error: ${e.message}")
                        }
                    },
                    evaluateJsCommand = jsCommand  // 新增：传递 JS 执行命令
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
                onDismissRequest = { showOverviewDialog = false },
                sourceBounds = screenState.overviewBounds
            )
        }

        // 工具栏
        if (showToolbar) {
            BottomToolbarSheet(
                detail = detail,
                detailView = detailViewModel,
                onDismissRequest = { showToolbar = false },
                onIconClick = { pageId, iconIndex ->
                    println("点击了页面 $pageId 的第 ${iconIndex + 1} 个图标")
                    when (pageId) {
                        "edit_title" -> showEditNameDialog = true
                        "summary" -> showOutlineDialog = true
                    }
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

        // 编辑标题弹窗
        if (showEditNameDialog) {
            EditNameDialog(
                initialTitle = detail.displayTitle,
                onConfirm = { title ->
                    detailViewModel.viewModelScope.launch {
                        detailViewModel.updateBookmarkTitle(title)
                    }
                },
                onDismissRequest = { showEditNameDialog = false }
            )
        }

        // 图片浏览器
        if (showImageViewer) {
            ImageViewer(
                imageUrls = allImageUrls,
                initialImageUrl = currentImageUrl,
                onDismiss = {
                    println("[ImageViewer] Dismissed")
                    showImageViewer = false
                }
            )
        }

        // 大纲弹窗
        if (showOutlineDialog) {
            OutlineDialog(
                detailViewModel = detailViewModel,
                initialState = OutlineDialogState.EXPANDED,
                onDismissRequest = { showOutlineDialog = false }
            )
        }
    }
}
