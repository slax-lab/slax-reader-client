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
import androidx.compose.ui.layout.onSizeChanged
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
import com.slax.reader.utils.WebViewEvent
import com.slax.reader.utils.rememberAppWebViewState
import com.slax.reader.utils.wrapBookmarkDetailHtml
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable

@Serializable
data class WebViewMessage(
    val type: String,
    val height: Int? = null,
    val src: String? = null,
    val allImages: List<String>? = null,
    val position: Int? = null,
    val index: Int? = null,
    val percentage: Double? = null
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
            htmlContent = htmlContent?.let { wrapBookmarkDetailHtml(it) }
        } catch (e: Exception) {
            error = e.message ?: "加载失败"
        }
    }

    val scrollState = rememberScrollState()
    val scrollY by remember { derivedStateOf { scrollState.value.toFloat() } }
    var manuallyVisible by remember { mutableStateOf(true) }

    // 获取屏幕高度（用于计算滚动偏移）
    val configuration = LocalConfiguration.current
    val screenHeightPx = with(LocalDensity.current) { configuration.screenHeightDp.dp.toPx() }

    // 记录 HeaderContent 的高度（px）
    var headerHeightPx by remember { mutableFloatStateOf(0f) }
    var webViewHeightPx by remember { mutableFloatStateOf(0f) }

    val webViewState = rememberAppWebViewState()
    val coroutineScope = rememberCoroutineScope()

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
    val outlineDialogState = rememberOutlineDialogState()

    // 图片浏览器状态
    var showImageViewer by remember { mutableStateOf(false) }
    var currentImageUrl by remember { mutableStateOf("") }
    var allImageUrls by remember { mutableStateOf<List<String>>(emptyList()) }

    LaunchedEffect(webViewState) {
        webViewState.events.collect { event ->
            when (event) {
                is WebViewEvent.ImageClick -> {
                    currentImageUrl = event.src
                    allImageUrls = event.allImages
                    showImageViewer = true
                }
                is WebViewEvent.ScrollToPosition -> {
                    val targetInWebView = webViewHeightPx * event.percentage
                    val target = (headerHeightPx + targetInWebView - screenHeightPx / 4).toInt()
                    coroutineScope.launch { scrollState.animateScrollTo(target.coerceAtLeast(0)) }
                }
                is WebViewEvent.Tap -> {
                    if (!isNearBottom && scrollY > 10f) {
                        manuallyVisible = !manuallyVisible
                    }
                }

                else -> {}
            }
        }
    }

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
                        .preferredFrameRate(FrameRateCategory.High)
                        .onSizeChanged { size ->
                            webViewHeightPx = size.height.toFloat()
                        },
                    webState = webViewState
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
                        "summary" -> outlineDialogState.show()
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

        // Outline弹窗
        if (outlineDialogState.currentState != OutlineDialogState.HIDDEN) {
            OutlineDialog(
                detailViewModel = detailViewModel,
                state = outlineDialogState,
                onScrollToAnchor = { anchor ->
                    webViewState.scrollToAnchor(anchor)
                }
            )
        }
    }
}
