package com.slax.reader.ui.bookmark

import androidx.compose.animation.*
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.multiplatform.webview.setting.PlatformWebSettings
import com.multiplatform.webview.util.KLogSeverity
import com.multiplatform.webview.web.LoadingState
import com.multiplatform.webview.web.WebView
import com.multiplatform.webview.web.rememberWebViewNavigator
import com.multiplatform.webview.web.rememberWebViewStateWithHTMLData
import com.slax.reader.data.database.model.UserTag
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.painterResource
import org.koin.compose.viewmodel.koinViewModel
import slax_reader_client.composeapp.generated.resources.*
import kotlin.math.roundToInt

// ================================================================================================
// 数据类
// ================================================================================================

data class ToolbarIcon(
    val label: String,
    val iconRes: DrawableResource? = null
)

// ================================================================================================
// 详情页页面
// ================================================================================================

@Composable
fun DetailScreen(nav: NavController, bookmarkId: String) {
    val detailView = koinViewModel<BookmarkDetailViewModel>()

    LaunchedEffect(bookmarkId) {
        detailView.setBookmarkId(bookmarkId)
    }

    val details by detailView.bookmarkDetail.collectAsState()
    val detail = details.firstOrNull()

    var currentTags: List<UserTag> by remember { mutableStateOf(emptyList()) }
    LaunchedEffect(detail?.metadataObj?.tags) {
        println("metadata object ====== ${detail?.metadataObj}")
        launch(Dispatchers.IO) {
            if (detail?.metadataObj?.tags != null) {
                currentTags = detailView.getTagNames(detail.metadataObj!!.tags)
            }
        }
    }

    val allAvailableTags by detailView.userTagList.collectAsState(emptyList())

    var showTagView by remember { mutableStateOf(false) }
    var showOverviewDialog by remember { mutableStateOf(false) }
    var showToolbar by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()

    val toolbarPages = remember {
        listOf(
            listOf(
                ToolbarIcon("Chat", Res.drawable.ic_bottom_panel_chatbot),
                ToolbarIcon("总结全文", Res.drawable.ic_bottom_panel_summary),
                ToolbarIcon("加星", Res.drawable.ic_bottom_panel_star),
                ToolbarIcon("归档", Res.drawable.ic_bottom_panel_archieve),
                ToolbarIcon("划线", Res.drawable.ic_bottom_panel_underline),
                ToolbarIcon("评论", Res.drawable.ic_bottom_panel_comment),
                ToolbarIcon("改标题", Res.drawable.ic_bottom_panel_comment),
                ToolbarIcon("分享", Res.drawable.ic_bottom_panel_share)
            ),
            listOf(
                ToolbarIcon("反馈", Res.drawable.ic_bottom_panel_feedback),
                ToolbarIcon("删除", Res.drawable.ic_bottom_panel_delete),
            )
        )
    }


    Box(modifier = Modifier.fillMaxSize().background(Color(0xFFFCFCFC))) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
        ) {
            // 页面内容从导航栏下方开始
            NavigatorBarSpacer()

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 20.dp)
                    .padding(bottom = 58.dp)
            ) {
                detail?.displayTitle?.let {
                    Text(
                        it,
                        style = TextStyle(
                            fontSize = 20.sp,
                            fontWeight = FontWeight.SemiBold,
                            lineHeight = 30.sp,
                            color = Color(0xFF0f1419)
                        )
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    detail?.createdAt?.let {
                        Text(
                            it,
                            style = TextStyle(fontSize = 14.sp, lineHeight = 20.sp, color = Color(0xFF999999))
                        )
                    }
                    Text(
                        "查看原网页",
                        modifier = Modifier.padding(start = 16.dp).clickable() {
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

                OverviewView(
                    modifier = Modifier.padding(top = 20.dp),
                    onExpand = { showOverviewDialog = true }
                )

                AdaptiveWebView(modifier = Modifier.fillMaxWidth().padding(top = 20.dp))
            }
        }

        // 底部悬浮操作栏
        FloatingActionBar(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 58.dp),
            onMoreClick = { showToolbar = true }
        )

        // 标签管理界面
        TagsManageBottomSheet(
            visible = showTagView,
            onDismissRequest = { showTagView = false },
            enableDrag = false,
            addedTags = currentTags,
            availableTags = allAvailableTags,
            onConfirm = { selectedTags ->
                currentTags = selectedTags
                coroutineScope.launch {
                    detailView.updateBookmarkTags(bookmarkId, selectedTags.map { it.id })
                }
            }
        )

        // Overview 弹窗
        OverviewDialog(
            visible = showOverviewDialog,
            onDismissRequest = { showOverviewDialog = false }
        )

        // 底部工具栏
        BottomToolbarSheet(
            visible = showToolbar,
            onDismissRequest = { showToolbar = false },
            pages = toolbarPages,
            onIconClick = { pageIndex, iconIndex ->
                println("点击了第 ${pageIndex + 1} 页的第 ${iconIndex + 1} 个图标")
                showToolbar = false
            }
        )

        NavigatorBar(
            navController = nav,
        )
    }
}

@Composable
private fun OverviewView(
    modifier: Modifier = Modifier,
    onExpand: () -> Unit = {}
) {
    Surface(
        modifier = modifier.then(Modifier.fillMaxWidth()),
        shape = RoundedCornerShape(8.dp),
        color = Color(0xFFF5F5F3)
    ) {
        Column() {
            Text("全文概要", modifier = Modifier.padding(12.dp), style = TextStyle())
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp)
                    .height(1.dp)
                    .background(Color(0x14333333))
            )

            Surface(
                onClick = onExpand,
                color = Color.Transparent,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(45.dp),
                shape = RoundedCornerShape(12.dp)
            ) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        horizontalArrangement =Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            "展开全部",
                            style = TextStyle(fontSize = 12.sp, lineHeight = 16.5.sp, color = Color(0xFF5490C2))
                        )
                        Icon(
                            painter = painterResource(Res.drawable.ic_xs_blue_down_arrow),
                            contentDescription = null,
                            tint = Color.Unspecified,
                            modifier = Modifier.size(8.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun FloatingActionBar(
    modifier: Modifier = Modifier,
    onMoreClick: () -> Unit = {}
) {
    Box(
        modifier = modifier
    ) {
        Box(
            modifier = Modifier
                .matchParentSize()
                .offset(y = 10.dp)
                .shadow(
                    elevation = 40.dp,
                    shape = RoundedCornerShape(25.dp),
                    ambientColor = Color.Black.copy(alpha = 1.0f),
                    spotColor = Color.Black.copy(alpha = 1.0f)
                )
        )

        Row(
            horizontalArrangement = Arrangement.spacedBy(0.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(25.dp))
                    .border(
                        width = 1.dp,
                        color = Color.White,
                        shape = RoundedCornerShape(25.dp)
                    )
                    .background(Color(0xFFF5F5F5))

            ) {
                Surface(
                    onClick = { /* 点击事件 */ },
                    modifier = Modifier
                        .size(50.dp),
                    color = Color.Transparent,
                ) {
                    Box(
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            painter = painterResource(Res.drawable.ic_floating_panel_star),
                            contentDescription = null,
                            tint = Color.Unspecified,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                Surface(
                    onClick = { /* 点击事件 */ },
                    modifier = Modifier
                        .size(50.dp),
                    color = Color.Transparent,
                ) {
                    Box(
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            painter = painterResource(Res.drawable.ic_floating_panel_archieve),
                            contentDescription = null,
                            tint = Color.Unspecified,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }

            Box(modifier = Modifier.width(12.dp))

            Surface(
                onClick = onMoreClick,
                modifier = Modifier
                    .size(50.dp),
                color = Color(0xFFF5F5F5),
                shape = RoundedCornerShape(25.dp),
                border = BorderStroke(1.dp, Color.White)
            ) {
                Box(
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        painter = painterResource(Res.drawable.ic_floating_panel_more),
                        contentDescription = null,
                        tint = Color.Unspecified,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}

// ================================================================================================
// UI组件 - 标签
// ================================================================================================

@Composable
private fun TagsView(
    tags: List<UserTag>,
    modifier: Modifier = Modifier,
    onAddTagClick: () -> Unit
) {
    FlowRow(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        tags.forEach { tag ->
            TagItem(
                tag = tag.tag_name,
                onClick = {  }
            )
        }

        Box(
            modifier = Modifier
                .size(21.dp) // 动态高度
                .clip(RoundedCornerShape(3.dp))
                .border(
                    width = 1.dp,
                    color = Color(0xFFE4D6BA),
                    shape = RoundedCornerShape(3.dp)
                ).then(
                    Modifier
                        .clickable(
                            onClick = onAddTagClick,
                            interactionSource = remember { MutableInteractionSource() }
                        )
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                painter = painterResource(Res.drawable.ic_xs_yellow_plus),
                contentDescription = null,
                tint = Color.Unspecified,
                modifier = Modifier.size(8.dp)
            )
        }
    }
}

@Composable
private fun TagItem(
    tag: String,
    onClick: () -> Unit,
    showDeleteButton: Boolean = false,
    onDelete: (() -> Unit)? = null,
    isLargeStyle: Boolean = false  // 是否使用大尺寸样式
) {
    // 根据样式选择不同的尺寸参数
    val height = if (isLargeStyle) 30.dp else 21.dp
    val fontSize = if (isLargeStyle) 15.sp else 12.sp
    val lineHeight = if (isLargeStyle) 21.sp else 15.sp
    val horizontalPadding = if (isLargeStyle) 6.dp else 4.dp
    val endPadding = if (isLargeStyle && showDeleteButton) 4.dp else if (showDeleteButton) 2.dp else horizontalPadding

    Box(
        modifier = Modifier
            .height(height) // 动态高度
            .clip(RoundedCornerShape(3.dp))
            .border(
                width = 1.dp,
                color = Color(0xFFE4D6BA),
                shape = RoundedCornerShape(3.dp)
            )
            .then(
                if (showDeleteButton) {
                    // 有删除按钮时不需要整体可点击
                    Modifier.padding(start = horizontalPadding, end = endPadding)
                } else {
                    // 没有删除按钮时整体可点击
                    Modifier
                        .clickable(
                            onClick = onClick,
                            interactionSource = remember { MutableInteractionSource() }
                        )
                        .padding(horizontal = horizontalPadding)
                }
            ),
        contentAlignment = if (showDeleteButton) Alignment.CenterStart else Alignment.Center
    ) {
        if (showDeleteButton && onDelete != null) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = tag,
                    style = TextStyle(
                        color = Color(0xFFA28D64),
                        fontSize = fontSize,
                        lineHeight = lineHeight
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Box(
                    modifier = Modifier
                        .width(1.dp)
                        .height(12.dp)
                        .background(Color(0x140F1419))
                )

                Box(
                    modifier = Modifier
                        .size(14.dp)
                        .clip(RoundedCornerShape(7.dp))
                        .clickable(
                            onClick = onDelete,
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "×",
                        style = TextStyle(
                            color = Color(0xFFA28D64),
                            fontSize = 14.sp,
                            lineHeight = 14.sp,
                            fontWeight = FontWeight.Normal
                        )
                    )
                }
            }
        } else {
            // 普通标签布局
            Text(
                text = tag,
                style = TextStyle(
                    color = Color(0xFFA28D64),
                    fontSize = fontSize,
                    lineHeight = lineHeight
                ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
fun AdaptiveWebView(modifier: Modifier = Modifier) {
    var webViewHeight by remember { mutableStateOf(500.dp) }
    val webViewState = rememberWebViewStateWithHTMLData(optimizedHtml)
    webViewState.webSettings.apply {
        isJavaScriptEnabled = true
        supportZoom = false
        allowFileAccessFromFileURLs = false
        allowUniversalAccessFromFileURLs = false
        logSeverity = KLogSeverity.Error

        androidWebSettings.apply {
            domStorageEnabled = true
            safeBrowsingEnabled = true
            allowFileAccess = false
            layerType = PlatformWebSettings.AndroidWebSettings.LayerType.HARDWARE
        }
    }

    // 监听导航状态
    val navigator = rememberWebViewNavigator()

    WebView(
        state = webViewState,
        modifier = modifier
            .fillMaxWidth()
            .height(webViewHeight),
        captureBackPresses = false,
        navigator = navigator,
        onCreated = { webView ->
            // WebView 创建时的配置
        },
        onDispose = { webView ->
            // 清理资源
        }
    )

    // 监听页面加载完成，获取高度
    LaunchedEffect(webViewState.loadingState) {
        if (webViewState.loadingState is LoadingState.Finished) {
            // 注入 JavaScript 获取页面高度
            val script = """
                (function() {
                    return Math.max(
                        document.body.scrollHeight,
                        document.body.offsetHeight,
                        document.documentElement.clientHeight,
                        document.documentElement.scrollHeight,
                        document.documentElement.offsetHeight
                    );
                })();
            """.trimIndent()

            navigator.evaluateJavaScript(script) { result ->
                result?.let {
                    try {
                        val height = it.toDoubleOrNull() ?: 500.0
                        webViewHeight = (height.dp + 10.dp)
                    } catch (e: Exception) {
                        println("获取高度失败: ${e.message}")
                    }
                }
            }
        }
    }
}


@Composable
fun TagsManageBottomSheet(
    visible: Boolean,
    onDismissRequest: () -> Unit,
    enableDrag: Boolean = false,
    addedTags: List<UserTag> = emptyList(),
    availableTags: List<UserTag> = emptyList(),
    onConfirm: (List<UserTag>) -> Unit = {}
) {
    val density = LocalDensity.current
    var offsetY by remember { mutableStateOf(0f) }
    var currentSelectedTags by remember { mutableStateOf(addedTags) }

    // 当visible变化时，重置选中的标签
    LaunchedEffect(visible) {
        if (visible) {
            currentSelectedTags = addedTags
        }
    }

    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(animationSpec = tween(300)),
        exit = fadeOut(animationSpec = tween(300))
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.5f))
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null
                ) {
                    onDismissRequest()
                }
        )
    }

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.BottomCenter
    ) {
        AnimatedVisibility(
            visible = visible,
            enter = slideInVertically(
                initialOffsetY = { it },
                animationSpec = tween(300)
            ),
            exit = slideOutVertically(
                targetOffsetY = { it },
                animationSpec = tween(300)
            )
        ) {
            Surface(
                shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp),
                color = Color.White,
                modifier = Modifier
                    .fillMaxWidth()
                    .offset { IntOffset(0, offsetY.roundToInt()) }
                    .then(
                        if (enableDrag) {
                            Modifier
                                .draggable(
                                    orientation = Orientation.Vertical,
                                    state = rememberDraggableState { delta ->
                                        offsetY = (offsetY + delta).coerceAtLeast(0f)
                                    },
                                    onDragStopped = {
                                        if (offsetY > with(density) { 100.dp.toPx() }) {
                                            onDismissRequest()
                                        } else {
                                            offsetY = 0f
                                        }
                                    }
                                )
                        } else {
                            Modifier
                                .clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = null
                                ) {}
                        }
                    ),
                shadowElevation = 8.dp
            ) {
                Column(
                    modifier = Modifier.padding(0.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(60.dp)
                            .padding(horizontal = 24.dp)
                    ) {
                        Text(
                            text = "取消",
                            modifier = Modifier
                                .align(Alignment.CenterStart)
                                .clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = null
                                ) {
                                    onDismissRequest()
                                },
                            style = TextStyle(
                                fontSize = 15.sp,
                                lineHeight = 20.sp,
                                color = Color(0xFF333333)
                            )
                        )

                        Text(
                            text = "标签",
                            modifier = Modifier.align(Alignment.Center),
                            style = TextStyle(
                                fontSize = 16.sp,
                                fontWeight = FontWeight.SemiBold,
                                lineHeight = 20.sp,
                                color = Color(0xFF0F1419)
                            )
                        )

                        Text(
                            text = "确定",
                            modifier = Modifier
                                .align(Alignment.CenterEnd)
                                .clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = null
                                ) {
                                        onConfirm(currentSelectedTags)
                                        onDismissRequest()
                                },
                            style = TextStyle(
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Medium,
                                lineHeight = 20.sp,
                                color = Color(0xFF16b998)
                            )
                        )
                    }

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 24.dp)
                            .height(0.5.dp)
                            .background(Color(0x14333333))
                    )

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 24.dp)
                            .verticalScroll(rememberScrollState())
                            .padding(bottom = 28.dp)
                    ) {
                        // 已添加标签区域
                        if (currentSelectedTags.isNotEmpty()) {
                            Text(
                                text = "已添加",
                                modifier = Modifier.padding(top = 20.dp, bottom = 12.dp),
                                style = TextStyle(
                                    fontSize = 14.sp,
                                    lineHeight = 20.sp,
                                    color = Color(0xFF999999)
                                )
                            )

                            FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                verticalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                currentSelectedTags.forEach { tag ->
                                    TagItem(
                                        tag = tag.tag_name,
                                        onClick = { /* 不需要 */ },
                                        showDeleteButton = true,
                                        onDelete = {
                                            // 点击删除按钮移除标签
                                            currentSelectedTags = currentSelectedTags - tag
                                        },
                                        isLargeStyle = true
                                    )
                                }
                            }
                        }

                        // 可添加标签区域
                        val unselectedTags = availableTags.filter { it !in currentSelectedTags }
                        if (unselectedTags.isNotEmpty()) {
                            Text(
                                text = "可添加",
                                modifier = Modifier.padding(
                                    top = if (currentSelectedTags.isNotEmpty()) 30.dp else 30.dp,
                                    bottom = 12.dp
                                ),
                                style = TextStyle(
                                    fontSize = 14.sp,
                                    lineHeight = 20.sp,
                                    color = Color(0xFF999999)
                                )
                            )

                            FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                verticalArrangement = Arrangement.spacedBy(16.dp),
                                modifier = Modifier.padding(bottom = 28.dp)
                            ) {
                                unselectedTags.forEach { tag ->
                                    TagItem(
                                        tag = tag.tag_name,
                                        onClick = {
                                            // 点击添加标签
                                            currentSelectedTags = currentSelectedTags + tag
                                        },
                                        isLargeStyle = true
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * Overview 弹窗组件
 */
@Composable
fun OverviewDialog(
    visible: Boolean,
    onDismissRequest: () -> Unit
) {
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(animationSpec = tween(300)),
        exit = fadeOut(animationSpec = tween(300))
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xCCF5F5F3))
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) {
                        onDismissRequest()
                    }
            )

            AnimatedVisibility(
                visible = visible,
                enter = scaleIn(
                    initialScale = 0.3f,
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioMediumBouncy,
                        stiffness = Spring.StiffnessLow
                    )
                ) + fadeIn(animationSpec = tween(200)),
                exit = scaleOut(
                    targetScale = 0.3f,
                    animationSpec = tween(200)
                ) + fadeOut(animationSpec = tween(200))
            ) {
                Surface(
                    modifier = Modifier
                        .padding(horizontal = 40.dp)
                        .fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    color = Color.White,
                    shadowElevation = 8.dp
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            "全文概要",
                            style = TextStyle(
                                fontSize = 18.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color(0xFF0F1419)
                            )
                        )

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 16.dp)
                                .height(1.dp)
                                .background(Color(0x14333333))
                        )

                        Text(
                            "这里是全文概要的内容...",
                            style = TextStyle(
                                fontSize = 14.sp,
                                lineHeight = 20.sp,
                                color = Color(0xFF666666)
                            )
                        )
                    }
                }
            }
        }
    }
}

// ================================================================================================
// UI组件 - 工具栏
// ================================================================================================

/**
 * 分页指示器组件
 * @param pageCount 总页数，只有一页时不显示
 * @param currentPage 当前页索引
 */
@Composable
private fun PageIndicator(
    pageCount: Int,
    currentPage: Int,
    modifier: Modifier = Modifier
) {
    if (pageCount <= 1) return

    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        repeat(pageCount) { index ->
            val isActive = index == currentPage
            Box(
                modifier = Modifier
                    .width(if (isActive) 12.dp else 6.dp)
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp))
                    .background(if (isActive) Color(0xFF333333) else Color(0xCC333333))
            )
        }
    }
}

/**
 * 单个图标按钮
 */
@Composable
private fun IconButton(
    icon: ToolbarIcon,
    onClick: () -> Unit
) {
    // 记录按下状态
    var isPressed by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()

    // 缩放动画：按下时缩小到0.85，松开时恢复到1.0
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.85f else 1.0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "scale"
    )

    // 透明度动画：按下时透明度降低到0.6，松开时恢复到1.0
    val alpha by animateFloatAsState(
        targetValue = if (isPressed) 0.6f else 1.0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "alpha"
    )

    Column(
        modifier = Modifier
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
                this.alpha = alpha
            }
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = {
                    // 点击时触发动画
                    isPressed = true
                    // 延迟恢复状态，让动画播放完整
                    coroutineScope.launch {
                        delay(150)
                        isPressed = false
                    }
//                    onClick()
                }
            ),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Box(
            modifier = Modifier
                .size(56.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(Color(0xCCFFFFFF)),
            contentAlignment = Alignment.Center
        ) {

            icon.iconRes?.let {
                Icon(
                    painter = painterResource(it),
                    contentDescription = null,
                    tint = Color.Unspecified,
                    modifier = Modifier.size(24.dp)
                )
            }

        }

        Text(
            text = icon.label,
            style = TextStyle(
                fontSize = 12.sp,
                lineHeight = 16.5.sp,
                color = Color(0xCC333333)
            )
        )
    }
}

/**
 * 工具栏单页布局组件
 * @param icons 图标列表（最多8个）
 * @param onIconClick 图标点击回调
 */
@Composable
private fun IconGridPage(
    icons: List<ToolbarIcon>,
    onIconClick: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.padding(horizontal = 24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // 上面一行4个icon
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            icons.take(4).forEachIndexed { index, icon ->
                IconButton(
                    icon = icon,
                    onClick = { onIconClick(index) }
                )
            }

            repeat(4 - icons.take(4).size) {
                Box(modifier = Modifier.width(56.dp))
            }
        }

        // 下面一行4个icon
        if (icons.size > 4) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                icons.drop(4).take(4).forEachIndexed { index, icon ->
                    IconButton(
                        icon = icon,
                        onClick = { onIconClick(index + 4) }
                    )
                }

                repeat(4 - icons.drop(4).take(4).size) {
                    Box(modifier = Modifier.width(56.dp))
                }
            }
        }
    }
}

/**
 * 支持左右滑动分页的工具栏容器
 */
@Composable
private fun PagerToolbar(
    pages: List<List<ToolbarIcon>>,
    onIconClick: (pageIndex: Int, iconIndex: Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val pagerState = rememberPagerState(pageCount = { pages.size })

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        // 页面内容
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxWidth().height(173.dp),
            verticalAlignment = Alignment.Top
        ) { page ->
            IconGridPage(
                icons = pages[page],
                onIconClick = { iconIndex -> onIconClick(page, iconIndex) }
            )
        }

        // 分页指示器
        PageIndicator(
            pageCount = pages.size,
            currentPage = pagerState.currentPage,
            modifier = Modifier.padding(bottom = 34.dp)
        )
    }
}

/**
 * 底部弹出工具栏
 * 带显示/隐藏动画，点击外部区域隐藏
 */
@Composable
fun BottomToolbarSheet(
    visible: Boolean,
    onDismissRequest: () -> Unit,
    pages: List<List<ToolbarIcon>>,
    onIconClick: (pageIndex: Int, iconIndex: Int) -> Unit
) {
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(animationSpec = tween(300)),
        exit = fadeOut(animationSpec = tween(300))
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.0f))
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null
                ) {
                    onDismissRequest()
                }
        )
    }

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.BottomCenter
    ) {
        AnimatedVisibility(
            visible = visible,
            enter = slideInVertically(
                initialOffsetY = { it },
                animationSpec = tween(300)
            ),
            exit = slideOutVertically(
                targetOffsetY = { it },
                animationSpec = tween(300)
            )
        ) {
            Surface(
                color = Color(0xF2F5F5F3),
                border = BorderStroke(0.5.dp, Color(0x140F1419)),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) { /* 阻止点击事件穿透 */ },
                shadowElevation = 8.dp
            ) {
                PagerToolbar(
                    pages = pages,
                    onIconClick = onIconClick,
                    modifier = Modifier.padding(top = 30.dp)
                )
            }
        }
    }
}