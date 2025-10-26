package com.slax.reader.ui.bookmark

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.viewModelScope
import androidx.navigation.NavController
import com.slax.reader.data.database.model.UserTag
import com.slax.reader.data.network.dto.OverviewResponse
import com.slax.reader.ui.AppViewModel
import com.slax.reader.ui.bookmark.components.*
import kotlinx.coroutines.launch
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel


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
    // println("[watch][UI] recomposition DetailScreen")

    val details by detailView.bookmarkDetail.collectAsState()
    val detail = details.firstOrNull()

    var overview by remember { mutableStateOf("") }
    var keyTakeaways by remember { mutableStateOf<List<String>>(emptyList()) }
    LaunchedEffect(bookmarkId) {
        // 清空旧数据
        overview = ""
        keyTakeaways = emptyList()

        detailView.setBookmarkId(bookmarkId)
        try {
            detailView.getBookmarkOverview(bookmarkId).collect { response ->
                when (response) {
                    is OverviewResponse.Overview -> {
                        overview += response.content
                    }

                    is OverviewResponse.KeyTakeaways -> {
                        keyTakeaways = response.content
                    }

                    is OverviewResponse.Done -> {
                        println("Overview loading completed")
                    }

                    is OverviewResponse.Error -> {
                        println("Error loading overview: ${response.message}")
                    }

                    else -> {
                    }
                }
            }
        } catch (e: Exception) {
            println("Failed to load overview: ${e.message}")
        }
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
    var webViewScrollY by remember { mutableFloatStateOf(0f) }

    // 顶部内容高度 (px) - 包含间距
    var topContentHeightPx by remember { mutableFloatStateOf(0f) }

    // FloatingActionBar 显示状态
    var manuallyVisible by remember { mutableStateOf(true) }
    val isFloatingBarVisible by remember { derivedStateOf { manuallyVisible } }

    // 头部跟随滚动的偏移
    val headerTranslation by remember {
        derivedStateOf {
            -webViewScrollY.coerceIn(0f, topContentHeightPx)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFFCFCFC))
    ) {
        // WebView 全屏滚动，使用原生 contentInset
        BookmarkContentView(
            bookmarkId = bookmarkId,
            topContentInsetPx = topContentHeightPx,
            onScrollChange = { scrollY ->
                webViewScrollY = scrollY
                val shouldShow = scrollY <= 10f
                if (manuallyVisible != shouldShow) {
                    manuallyVisible = shouldShow
                }
            },
            onWebViewTap = {
                manuallyVisible = if (webViewScrollY <= 10f) true else !manuallyVisible
            }
        )

        // 顶部内容跟随滚动
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()  // 避开状态栏
                .graphicsLayer {
                    // 跟随 WebView 滚动偏移，实现 1:1 同步
                    translationY = headerTranslation
                }
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
                    .padding(bottom = 16.dp)  // 底部留出视觉间距
                    .onGloballyPositioned { coordinates ->
                        // 测量 Column 高度（不包括 Box 的 statusBarsPadding）
                        // 所以需要在 ContentView 中手动加上 statusBarsPadding
                        val newHeight = coordinates.size.height.toFloat()
                        topContentHeightPx = newHeight
                        println("[DetailScreen] Column height measured: $newHeight px")
                    }
            ) {
                // 不需要 NavigatorBarSpacer，因为 Box 已经有 statusBarsPadding
                Spacer(modifier = Modifier.height(44.dp))

                Text(
                    text = detail?.displayTitle ?: "",
                    style = TextStyle(
                        fontSize = 20.sp,
                        fontWeight = FontWeight.SemiBold,
                        lineHeight = 30.sp,
                        color = Color(0xFF0f1419)
                    )
                )

                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = detail?.displayTime ?: "",
                        style = TextStyle(fontSize = 14.sp, lineHeight = 20.sp, color = Color(0xFF999999))
                    )
                    Text(
                        "查看原网页",
                        modifier = Modifier.padding(start = 16.dp).clickable {
                            println("被点击了")
                        },
                        style = TextStyle(color = Color(0xFF5490C2), fontSize = 14.sp, lineHeight = 20.sp)
                    )
                }

                TagsView(
                    modifier = Modifier.padding(top = 16.dp),
                    tags = currentTags,
                    onAddTagClick = { showTagView = true }
                )

                if (overview.isNotEmpty()) {
                    OverviewView(
                        modifier = Modifier.padding(top = 20.dp),
                        content = overview,
                        onExpand = {
                            showOverviewDialog = true
                        },
                        onBoundsChanged = { bounds -> overviewBounds = bounds }
                    )
                }
            }
        }

        // 底部悬浮操作栏 - 带有滑动动画
        val floatingBarOffsetY by animateDpAsState(
            targetValue = if (isFloatingBarVisible) 0.dp else 150.dp,
            animationSpec = tween(durationMillis = 300)
        )

        detail?.let {
            FloatingActionBar(
                detail = it,
                detailView = detailView,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 58.dp)
                    .offset(y = floatingBarOffsetY),

                onMoreClick = { showToolbar = true }
            )
        }

        // 标签管理界面
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

        detail?.let {
            // 底部工具栏
            BottomToolbarSheet(
                detail = it,
                detailView = detailView,
                visible = showToolbar,
                onDismissRequest = { showToolbar = false },
                onIconClick = { pageIndex, iconIndex ->
                    println("点击了第 ${pageIndex + 1} 页的第 ${iconIndex + 1} 个图标")
                    showToolbar = false
                }
            )
        }

        NavigatorBar(
            navController = nav,
            visible = isFloatingBarVisible
        )
    }
}
