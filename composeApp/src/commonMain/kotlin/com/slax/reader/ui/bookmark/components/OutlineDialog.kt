package com.slax.reader.ui.bookmark.components

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.slax.reader.ui.bookmark.BookmarkDetailViewModel
import kotlinx.coroutines.delay
import org.jetbrains.compose.resources.painterResource
import slax_reader_client.composeapp.generated.resources.Res
import slax_reader_client.composeapp.generated.resources.ic_outline_banner_analyzed
import slax_reader_client.composeapp.generated.resources.ic_outline_banner_analyzing
import slax_reader_client.composeapp.generated.resources.ic_outline_banner_close
import slax_reader_client.composeapp.generated.resources.ic_outline_banner_expand
import slax_reader_client.composeapp.generated.resources.ic_outline_dialog_close
import slax_reader_client.composeapp.generated.resources.ic_outline_dialog_shrink

/**
 * 大纲弹窗的三种状态
 */
enum class OutlineDialogState {
    HIDDEN,      // 隐藏
    EXPANDED,    // 展开（全屏弹窗）
    COLLAPSED    // 收缩（小banner）
}

/**
 * 大纲弹窗组件
 * 支持三种状态：隐藏、全屏弹窗、收缩为小banner
 */
@Composable
fun OutlineDialog(
    detailViewModel: BookmarkDetailViewModel,
    initialState: OutlineDialogState = OutlineDialogState.HIDDEN,
    onDismissRequest: () -> Unit
) {
    println("[watch][UI] recomposition OutlineDialog")

    LaunchedEffect(detailViewModel._bookmarkId) {
        detailViewModel.loadOutline()
    }

    var currentState by remember { mutableStateOf(OutlineDialogState.HIDDEN) }
    var visible by remember { mutableStateOf(false) }

    // 计算弹窗需要向上移动的距离（dp）
    val density = LocalDensity.current
    val moveUpDistancePx = remember {
        with(density) { 28.dp.toPx() } // 36dp - 8dp = 28dp
    }

    // 初始化时延迟显示，确保动画生效
    LaunchedEffect(Unit) {
        if (initialState != OutlineDialogState.HIDDEN) {
            visible = true
            delay(50L) // 短暂延迟，确保动画触发
            currentState = initialState
        }
    }

    // 监听状态变化
    LaunchedEffect(currentState) {
        if (currentState == OutlineDialogState.HIDDEN) {
            delay(300L) // 等待动画完成
            visible = false
            onDismissRequest()
        }
    }

    if (!visible) {
        return
    }

    Box(modifier = Modifier.fillMaxSize()) {
        // 半透明背景（仅在EXPANDED状态显示）
        AnimatedVisibility(
            visible = currentState == OutlineDialogState.EXPANDED,
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
                        currentState = OutlineDialogState.HIDDEN
                    }
            )
        }

        // 全屏弹窗（底部对齐）
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.BottomCenter
        ) {
            AnimatedVisibility(
                visible = currentState == OutlineDialogState.EXPANDED,
                enter = slideInVertically(
                    initialOffsetY = { it },
                    animationSpec = tween(300)
                ),
                exit = slideOutVertically(
                    targetOffsetY = { it },
                    animationSpec = tween(300)
                )
            ) {
                ExpandedOutlineDialog(
                    detailViewModel = detailViewModel,
                    onCollapse = { currentState = OutlineDialogState.COLLAPSED },
                    onClose = { currentState = OutlineDialogState.HIDDEN }
                )
            }
        }

        // 小banner（顶部对齐）
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.TopCenter
        ) {
            AnimatedVisibility(
                visible = currentState == OutlineDialogState.COLLAPSED,
                enter = scaleIn(
                    initialScale = 0.3f,
                    animationSpec = tween(350, delayMillis = 100)
                ) + fadeIn(
                    animationSpec = tween(300, delayMillis = 100)
                ),
                exit = if (currentState == OutlineDialogState.EXPANDED) {
                    // 展开：向下放大消失（变成弹窗）
                    slideOutVertically(
                        targetOffsetY = { it },
                        animationSpec = tween(350)
                    ) + scaleOut(
                        targetScale = 3.0f,
                        animationSpec = tween(350)
                    ) + fadeOut(
                        animationSpec = tween(250)
                    )
                } else {
                    // 关闭：原地缩小淡出
                    scaleOut(
                        targetScale = 0.3f,
                        animationSpec = tween(300)
                    ) + fadeOut(
                        animationSpec = tween(250)
                    )
                }
            ) {
                CollapsedOutlineBanner(
                    detailViewModel = detailViewModel,
                    onExpand = { currentState = OutlineDialogState.EXPANDED },
                    onClose = { currentState = OutlineDialogState.HIDDEN }
                )
            }
        }
    }
}

/**
 * 全屏展开状态的弹窗
 */
@Composable
private fun ExpandedOutlineDialog(
    detailViewModel: BookmarkDetailViewModel,
    onCollapse: () -> Unit,
    onClose: () -> Unit
) {
    // 订阅状态
    val outlineContent by detailViewModel.outlineContent.collectAsState()
    val outlineState by detailViewModel.outlineState.collectAsState()

    Surface(
        shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
        color = Color.White,
        shadowElevation = 8.dp,
        modifier = Modifier
            .fillMaxWidth()
            .fillMaxHeight()
            .statusBarsPadding() // 避开状态栏
            .padding(top = 36.dp) // 距离状态栏底部 36dp
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) { /* 阻止点击事件穿透 */ }
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 0.dp)
        ) {
            // 顶部控制栏
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 0.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 收缩按钮
                val collapseInteractionSource = remember { MutableInteractionSource() }
                val isCollapsePressed by collapseInteractionSource.collectIsPressedAsState()

                Box(
                    modifier = Modifier
                        .alpha(if (isCollapsePressed) 0.5f else 1f)
                        .clickable(
                            interactionSource = collapseInteractionSource,
                            indication = null,
                            onClick = onCollapse
                        )
                        .padding(vertical = 18.dp, horizontal = 20.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        painter = painterResource(Res.drawable.ic_outline_dialog_shrink),
                        contentDescription = "收缩",
                        tint = Color(0xFF666666),
                        modifier = Modifier.size(24.dp)
                    )
                }

                // 关闭按钮
                val closeInteractionSource = remember { MutableInteractionSource() }
                val isClosePressed by closeInteractionSource.collectIsPressedAsState()

                Box(
                    modifier = Modifier
                        .alpha(if (isClosePressed) 0.5f else 1f)
                        .clickable(
                            interactionSource = closeInteractionSource,
                            indication = null,
                            onClick = onClose
                        )
                        .padding(vertical = 18.dp, horizontal = 20.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        painter = painterResource(Res.drawable.ic_outline_dialog_close),
                        contentDescription = "关闭",
                        tint = Color(0xFF666666),
                        modifier = Modifier.size(24.dp)
                    )
                }
            }

            Box(
                modifier = Modifier
                    .padding(horizontal = 20.dp)
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                when {
                    outlineState.isLoading -> {
                        // 加载状态：显示动画
                        LoadingAnimation()
                    }
                    outlineState.error != null -> {
                        // 错误状态：显示错误信息
                        ErrorView(error = outlineState.error!!)
                    }
                    outlineContent.isEmpty() -> {
                        // 空状态：显示提示信息
                        EmptyView()
                    }
                    else -> {
                        // 已加载：显示 markdown 内容
                        MarkdownRenderer(content = outlineContent, onLinkClick = {
                            println("click link in outline: $it")
                        })
                    }
                }
            }
        }
    }
}

/**
 * 收缩状态的小banner
 * 根据 outline 加载状态显示不同的文本和图标
 */
@Composable
private fun CollapsedOutlineBanner(
    detailViewModel: BookmarkDetailViewModel,
    onExpand: () -> Unit,
    onClose: () -> Unit
) {
    // 订阅状态
    val outlineState by detailViewModel.outlineState.collectAsState()
    val outlineContent by detailViewModel.outlineContent.collectAsState()
    val bookmarkDetail by detailViewModel.bookmarkDetail.collectAsState()

    // 获取 bookmark 标题
    val displayTitle = bookmarkDetail.firstOrNull()?.displayTitle ?: ""

    // 根据状态确定显示内容
    val isLoading = outlineState.isLoading
    val isCompleted = !isLoading && outlineContent.isNotEmpty() && outlineState.error == null

    Surface(
        shape = RoundedCornerShape(16.dp),
        color = Color.White,
        shadowElevation = 4.dp,
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding() // 添加状态栏间距
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) { /* 阻止点击事件穿透 */ }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 17.dp, horizontal = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 左边：图标 + 文本
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // 状态图标
                Icon(
                    painter = painterResource(
                        if (isCompleted) {
                            Res.drawable.ic_outline_banner_analyzed
                        } else {
                            Res.drawable.ic_outline_banner_analyzing
                        }
                    ),
                    contentDescription = if (isCompleted) "已完成" else "总结中",
                    tint = Color.Unspecified, // 使用原始颜色
                    modifier = Modifier.size(20.dp)
                )

                // 状态文本（使用 AnnotatedString 实现不同颜色）
                Text(
                    modifier = Modifier.padding(start = 8.dp),
                    text = buildAnnotatedString {
                        withStyle(
                            style = SpanStyle(
                                color = if (isCompleted) Color(0xFF16B998) else Color(0xFFA28D64),
                                fontSize = 15.sp
                            )
                        ) {
                            append(if (isCompleted) "已完成：" else "总结中：")
                        }
                        withStyle(
                            style = SpanStyle(
                                color = Color(0xFF333333),
                                fontSize = 15.sp
                            )
                        ) {
                            append(displayTitle)
                        }
                    },
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            // 分割线
            Box(
                modifier = Modifier
                    .padding(start = 16.dp, end = 12.dp)
                    .width(1.dp)
                    .height(16.dp)
                    .background(Color(0x3DCAA68E))
            )

            if (isCompleted) {
                // 展开按钮
                val expandInteractionSource = remember { MutableInteractionSource() }
                val isExpandPressed by expandInteractionSource.collectIsPressedAsState()

                Box(
                    modifier = Modifier
                        .alpha(if (isExpandPressed) 0.5f else 1f)
                        .clickable(
                            interactionSource = expandInteractionSource,
                            indication = null,
                            onClick = onExpand
                        )
                        .padding(4.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        painter = painterResource(Res.drawable.ic_outline_banner_expand),
                        contentDescription = "展开",
                        modifier = Modifier.size(12.dp)
                    )
                }
            } else {
                // 关闭按钮
                val closeBannerInteractionSource = remember { MutableInteractionSource() }
                val isCloseBannerPressed by closeBannerInteractionSource.collectIsPressedAsState()

                Box(
                    modifier = Modifier
                        .alpha(if (isCloseBannerPressed) 0.5f else 1f)
                        .clickable(
                            interactionSource = closeBannerInteractionSource,
                            indication = null,
                            onClick = onClose
                        )
                        .padding(4.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        painter = painterResource(Res.drawable.ic_outline_banner_close),
                        contentDescription = "关闭",
                        modifier = Modifier.size(12.dp)
                    )
                }
            }
        }
    }
}

/**
 * 加载动画组件
 * 显示三个渐变色横条闪动效果
 */
@Composable
private fun LoadingAnimation() {
    val infiniteTransition = rememberInfiniteTransition(label = "loadingAnimation")
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "alphaAnimation"
    )

    Column(
        modifier = Modifier
            .fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        repeat(3) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(18.dp)
                    .alpha(alpha)
                    .background(
                        brush = Brush.horizontalGradient(
                            colors = listOf(
                                Color(0xFFF5F5F3),
                                Color(0x99F5F5F3),
                            )
                        ),
                        shape = RoundedCornerShape(4.dp)
                    )
            )
        }
    }
}

/**
 * 空状态视图
 */
@Composable
private fun EmptyView() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "暂无内容",
            fontSize = 14.sp,
            color = Color(0xFF999999)
        )
    }
}

/**
 * 错误状态视图
 */
@Composable
private fun ErrorView(error: String) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "加载失败",
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium,
                color = Color(0xFFFF5252)
            )
            Text(
                text = error,
                fontSize = 12.sp,
                color = Color(0xFF999999)
            )
        }
    }
}