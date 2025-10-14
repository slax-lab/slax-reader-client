package com.slax.reader.ui.bookmark

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.Spring
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.ui.draw.blur
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.multiplatform.webview.setting.PlatformWebSettings
import com.multiplatform.webview.util.KLogSeverity
import com.multiplatform.webview.web.WebView
import com.multiplatform.webview.web.rememberWebViewStateWithHTMLData
import com.multiplatform.webview.web.*
import kotlin.math.roundToInt
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun DetailScreen(nav: NavController, bookmarkId: String) {
    var webViewHeight by remember { mutableStateOf(0) }
    val webState = rememberWebViewStateWithHTMLData(optimizedHtml)
    var showTagView by remember { mutableStateOf(false) }
    var showOverviewDialog by remember { mutableStateOf(false) }
    var showToolbar by remember { mutableStateOf(false) }

    // 示例工具栏数据 - 3页，每页8个图标
    val toolbarPages = remember {
        listOf(
            // 第1页
            listOf(
                ToolbarIcon("复制"),
                ToolbarIcon("分享"),
                ToolbarIcon("收藏"),
                ToolbarIcon("删除"),
                ToolbarIcon("编辑"),
                ToolbarIcon("导出"),
                ToolbarIcon("打印"),
                ToolbarIcon("更多")
            ),
            // 第2页
            listOf(
                ToolbarIcon("标签"),
                ToolbarIcon("笔记"),
                ToolbarIcon("归档"),
                ToolbarIcon("移动"),
                ToolbarIcon("重命名"),
                ToolbarIcon("属性"),
                ToolbarIcon("历史"),
                ToolbarIcon("帮助")
            ),
            // 第3页
            listOf(
                ToolbarIcon("设置"),
                ToolbarIcon("主题"),
                ToolbarIcon("字体"),
                ToolbarIcon("缩放")
            )
        )
    }

    webState.webSettings.apply {
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

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(bottom = 58.dp) // 底部留出58dp间距
                .background(Color(0xFFFCFCFC))
        ) {
            NavigationBar()
            Column(
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("对话面壁智能首席科学家刘知远：大模型将有新的「摩尔定律」，AGI 时代的智能终端未必是手机", style = TextStyle(fontSize = 20.sp, fontWeight = FontWeight.SemiBold, lineHeight = 30.sp, color = Color(0xFF0f1419)))
                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text("2024-07-13 11:29", style = TextStyle(fontSize = 14.sp, lineHeight = 20.sp, color = Color(0xFF999999)))
                    Text("查看原网页",
                        modifier = Modifier.padding(start = 16.dp).clickable() {
                            println("被点击了")
                        },
                        style = TextStyle(color = Color(0xFF5490C2), fontSize = 14.sp, lineHeight = 20.sp)
                    )
                }

                TagsView(
                    modifier = Modifier.padding(top = 16.dp),
                    tags = listOf("AI", "大模型", "ChatGPT", "智能手机", "AGI", "刘知远", "科技", "访谈", "前沿技术"),
                    onTagClick = { showTagView = true }
                )

                OverviewView(
                    modifier = Modifier.padding(top = 20.dp),
                    onExpand = { showOverviewDialog = true }
                )

                AdaptiveWebView(modifier = Modifier.fillMaxWidth().padding(top = 20.dp))
            }
        }

        // 悬浮操作栏
        FloatingActionBar(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 58.dp),
            onMoreClick = { showToolbar = true }
        )

        // BottomSheet 放在最外层，确保浮在所有内容之上
        TagsManageBottomSheet(
            visible = showTagView,
            onDismissRequest = { showTagView = false },
            enableDrag = false
        )

        // 居中白色弹窗
        CenterWhiteDialog(
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
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                    ) {
                        Text("展开全部", style = TextStyle(fontSize = 12.sp, lineHeight = 16.5.sp, color = Color(0xFF5490C2)))
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
        // 阴影层
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

        // 内容层
        Row(
            horizontalArrangement = Arrangement.spacedBy(0.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
        // 左边两个按钮（连在一起）
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
            // 按钮1
            Surface(
                onClick = { /* 按钮1点击事件 */ },
                modifier = Modifier
                    .size(50.dp),
                color = Color.Transparent,
                shape = RoundedCornerShape(25.dp)
            ) {
                Box(
                    contentAlignment = Alignment.Center
                ) {
                    Text("1", style = TextStyle(fontSize = 16.sp, color = Color(0xFF0F1419)))
                }
            }

            // 按钮2
            Surface(
                onClick = { /* 按钮2点击事件 */ },
                modifier = Modifier
                    .size(50.dp),
                color = Color.Transparent,
                shape = RoundedCornerShape(25.dp)
            ) {
                Box(
                    contentAlignment = Alignment.Center
                ) {
                    Text("2", style = TextStyle(fontSize = 16.sp, color = Color(0xFF0F1419)))
                }
            }
        }

        // 12dp 间距
        Box(modifier = Modifier.width(12.dp))

        // 第三个按钮 - 更多操作
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
                Text("3", style = TextStyle(fontSize = 16.sp, color = Color(0xFF0F1419)))
            }
        }
    }
    }
}

@Composable
private fun NavigationBar() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 16.dp)
    ) {
        Row(
            modifier = Modifier
                .width(70.dp)
                .align(Alignment.CenterStart),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
        }
    }
}

@Composable
private fun TagsView(
    tags: List<String>,
    modifier: Modifier = Modifier,
    onTagClick: () -> Unit
) {
    FlowRow(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        tags.forEach { tag ->
            TagItem(
                tag = tag,
                onClick = onTagClick
            )
        }
    }
}

@Composable
private fun TagItem(
    tag: String,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .height(21.dp) // 固定高度
            .clip(RoundedCornerShape(3.dp)) // 圆角裁剪，确保点击波纹也有圆角
            .border(
                width = 1.dp,
                color = Color(0xFFE4D6BA),
                shape = RoundedCornerShape(3.dp)
            )
            .clickable(
                onClick = onClick,
                interactionSource = remember { MutableInteractionSource() }
            )
            .padding(horizontal = 4.dp), // 只保留水平padding
        contentAlignment = Alignment.Center // 文字垂直居中
    ) {
        Text(
            text = tag,
            style = TextStyle(
                color = Color(0xFFA28D64),
                fontSize = 12.sp,
                lineHeight = 15.sp
            ),
            maxLines = 1, // 防止文字换行
            overflow = TextOverflow.Ellipsis // 文字过长显示省略号
        )
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
                        webViewHeight = height.dp
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
) {
    val density = LocalDensity.current
    var offsetY by remember { mutableStateOf(0f) }

    // 整个底部弹窗容器，包含背景和内容
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(animationSpec = tween(300)),
        exit = fadeOut(animationSpec = tween(300))
    ) {
        Box(
            modifier = Modifier.fillMaxSize()
        ) {
            // 半透明背景
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

            // 底部弹窗内容，带滑入动画
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .offset { IntOffset(0, offsetY.roundToInt()) },
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
                            modifier = Modifier.padding(0.dp),
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 24.dp).padding(top = 24.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                            ) {
                                Text(
                                    "标签",
                                    style = TextStyle(
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        lineHeight = 20.sp,
                                        color = Color(0xFF0F1419)
                                    )
                                )

                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(top = 20.dp)
                                        .height(1.dp)
                                        .background(Color(0x14333333))
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * 居中白色弹窗组件
 * 带有高斯模糊背景和从中间"鼓出来"的动画效果
 */
@Composable
fun CenterWhiteDialog(
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
            // 磨砂玻璃效果背景 - 多层叠加实现更明显的效果
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xCCF5F5F3)) // 更高透明度的白色背景
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) {
                        onDismissRequest()
                    }
            )

            // 居中的白色弹窗,带有从中间鼓出来的动画
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
 * 工具栏图标数据类
 */
data class ToolbarIcon(
    val label: String,
    val iconRes: String? = null
)

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
        )
    )

    Column(
        modifier = Modifier
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = {
                    isPressed = true
                    // 延迟恢复状态，让动画播放完整
                    coroutineScope.launch {
                        delay(150)
                        isPressed = false
                    }
                    onClick()
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
            Text(
                text = icon.label.first().toString(),
                style = TextStyle(
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color(0xFF0F1419)
                )
            )
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
 * 工具栏单页布局组件 - 显示8个icon按钮（上4下4）
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
    // 背景层 - 淡入淡出
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

    // 底部内容层 - 从下往上滑入
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.BottomCenter
    ) {
        AnimatedVisibility(
            visible = visible,
            enter = slideInVertically(
                initialOffsetY = { it }, // 从底部开始（完整高度的偏移）
                animationSpec = tween(300)
            ),
            exit = slideOutVertically(
                targetOffsetY = { it }, // 滑出到底部（完整高度的偏移）
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