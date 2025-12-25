package com.slax.reader.ui.bookmark

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.FrameRateCategory
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.preferredFrameRate
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewModelScope
import com.slax.reader.const.component.EditNameDialog
import com.slax.reader.data.database.model.UserBookmark
import com.slax.reader.ui.bookmark.components.*
import com.slax.reader.utils.*
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import kotlin.math.max

@Serializable
data class WebViewMessage(
    val type: String,
    val height: Int? = null,
    val src: String? = null,
    val allImages: List<String>? = null,
    val position: Int? = null,
    val index: Int? = null,
    val percentage: Double? = null,

    val productId: String? = null,
    val orderId: String? = null,
    val offerId: String? = null,
    val keyID: String? = null,
    val nonce: String? = null,
    val signature: String? = null,
    val timestamp: Long? = null
)

@Composable
actual fun DetailScreen(
    detailViewModel: BookmarkDetailViewModel,
    detail: UserBookmark,
    htmlContent: String,
    screenState: DetailScreenState,
    onBackClick: (() -> Unit),
    onNavigateToSubscription: (() -> Unit)?,
) {
    val wrappedHtmlContent = remember(htmlContent) { wrapBookmarkDetailHtml(htmlContent) }

    // WebView 滚动偏移
    val webViewScrollY = remember { mutableFloatStateOf(0f) }

    var contentHeightPx by remember { mutableFloatStateOf(0f) }
    var visibleHeightPx by remember { mutableFloatStateOf(0f) }

    // 顶部内容高度 (px)
    var headerMeasuredHeight by remember { mutableFloatStateOf(0f) }

    var manuallyVisible by remember { mutableStateOf(true) }

    val coroutineScope = rememberCoroutineScope()
    val webViewState = rememberAppWebViewState(coroutineScope)

    val bottomThresholdPx = with(LocalDensity.current) { 100.dp.toPx() }

    LaunchedEffect(Unit) {
        LifeCycleHelper.lifecycleState.collect { state ->
            when (state) {
                AppLifecycleState.ON_STOP -> {
                    detailViewModel.viewModelScope.launch {
                        detailViewModel.recordContinueBookmark(webViewScrollY.floatValue.toInt())
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

    // 距离底部的距离
    val distanceToBottom by remember {
        derivedStateOf {
            contentHeightPx - (webViewScrollY.floatValue + visibleHeightPx) + headerMeasuredHeight
        }
    }

    // 是否接近底部
    val isNearBottom by remember {
        derivedStateOf {
            distanceToBottom < bottomThresholdPx
        }
    }

    // 统一处理 manuallyVisible 的自动更新逻辑
    LaunchedEffect(Unit) {
        snapshotFlow { isNearBottom to webViewScrollY.floatValue }
            .collect { (nearBottom, scrollY) ->
                manuallyVisible = when {
                    nearBottom -> true  // 在底部区域，强制显示
                    scrollY <= 10f -> true  // 在顶部区域，显示
                    else -> false  // 中间区域，自动隐藏
                }
            }
    }

    val headerVisible by remember {
        derivedStateOf {
            headerMeasuredHeight == 0f || webViewScrollY.floatValue < headerMeasuredHeight
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

    // 设置 webState 的参数
    LaunchedEffect(headerMeasuredHeight) {
        webViewState.topContentInsetPx = headerMeasuredHeight
    }

    // 监听 WebView 事件
    LaunchedEffect(webViewState) {
        webViewState.events.collect { event ->
            when (event) {
                is WebViewEvent.ImageClick -> {
                    currentImageUrl = event.src
                    allImageUrls = event.allImages
                    showImageViewer = true
                }
                is WebViewEvent.ScrollChange -> {
                    webViewScrollY.floatValue = max(event.scrollY, 0f)
                    contentHeightPx = event.contentHeight
                    visibleHeightPx = event.visibleHeight
                }
                is WebViewEvent.ScrollToPosition -> {
                    webViewState.evaluateJs("window.scrollTo(0, document.body.scrollHeight * ${event.percentage})")
                }
                is WebViewEvent.Tap -> {
                    if (!isNearBottom && webViewScrollY.floatValue > 10f) {
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
        key(detail.id) {
            AppWebView(
                htmlContent = wrappedHtmlContent,
                modifier = Modifier.fillMaxSize().preferredFrameRate(FrameRateCategory.High),
                webState = webViewState
            )
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
                onDismissRequest = { showOverviewDialog = false },
                sourceBounds = screenState.overviewBounds
            )
        }

        if (showToolbar) {
            BottomToolbarSheet(
                detail = detail,
                detailView = detailViewModel,
                onDismissRequest = { showToolbar = false },
                onSubscriptionRequired = onNavigateToSubscription,
                onIconClick = { pageId, iconIndex ->
                    println("点击了页面 $pageId 的第 ${iconIndex + 1} 个图标")
                    when (pageId) {
                        "edit_title" -> showEditNameDialog = true
                        "summary" -> outlineDialogState.show()
                    }
                }
            )
        }

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
