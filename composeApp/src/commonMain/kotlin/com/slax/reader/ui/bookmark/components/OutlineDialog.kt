package com.slax.reader.ui.bookmark.components

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.slax.reader.ui.bookmark.BookmarkDetailViewModel
import com.slax.reader.utils.i18n
import org.jetbrains.compose.resources.painterResource
import slax_reader_client.composeapp.generated.resources.Res
import slax_reader_client.composeapp.generated.resources.ic_outline_banner_analyzed
import slax_reader_client.composeapp.generated.resources.ic_outline_banner_analyzing
import slax_reader_client.composeapp.generated.resources.ic_outline_banner_close
import slax_reader_client.composeapp.generated.resources.ic_outline_banner_expand
import slax_reader_client.composeapp.generated.resources.ic_outline_dialog_close
import slax_reader_client.composeapp.generated.resources.ic_outline_dialog_shrink

/**
 * Outline弹窗的三种状态
 */
enum class OutlineDialogState {
    HIDDEN,      // 隐藏
    EXPANDED,    // 展开弹窗
    COLLAPSED    // 收缩（小banner）
}


@Stable
class OutlineDialogStateHolder(
    initialState: OutlineDialogState = OutlineDialogState.HIDDEN
) {
    var currentState by mutableStateOf(initialState)
        private set

    val isVisible: Boolean get() = currentState != OutlineDialogState.HIDDEN
    val isExpanded: Boolean get() = currentState == OutlineDialogState.EXPANDED
    val isCollapsed: Boolean get() = currentState == OutlineDialogState.COLLAPSED

    fun show() {
        currentState = OutlineDialogState.EXPANDED
    }

    fun expand() {
        currentState = OutlineDialogState.EXPANDED
    }

    fun collapse() {
        currentState = OutlineDialogState.COLLAPSED
    }

    fun hide() {
        currentState = OutlineDialogState.HIDDEN
    }
}

@Composable
fun rememberOutlineDialogState(
    initialState: OutlineDialogState = OutlineDialogState.HIDDEN
): OutlineDialogStateHolder {
    return remember { OutlineDialogStateHolder(initialState) }
}

/**
 * Outline弹窗组件
 */
@Composable
fun OutlineDialog(
    detailViewModel: BookmarkDetailViewModel,
    state: OutlineDialogStateHolder,
    onScrollToAnchor: (String) -> Unit
) {
    println("[watch][UI] outline dialog recomposed, state = ${state.currentState}")
    LaunchedEffect(detailViewModel._bookmarkId) {
        detailViewModel.loadOutline()
    }

    if (!state.isVisible) return

    Box(modifier = Modifier.fillMaxSize()) {
        AnimatedVisibility(
            visible = state.isExpanded,
            enter = fadeIn(animationSpec = tween(300)),
            exit = fadeOut(animationSpec = tween(300))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.5f))
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = { state.hide() }
                    )
            )
        }

        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.BottomCenter
        ) {
            AnimatedVisibility(
                visible = state.isExpanded,
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
                    onCollapse = { state.collapse() },
                    onClose = { state.hide() },
                    onScrollToAnchor = onScrollToAnchor
                )
            }
        }

        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.TopCenter
        ) {
            AnimatedVisibility(
                visible = state.isCollapsed,
                enter = scaleIn(
                    initialScale = 0.3f,
                    animationSpec = tween(350, delayMillis = 100)
                ) + fadeIn(
                    animationSpec = tween(300, delayMillis = 100)
                ),
                exit = if (state.currentState == OutlineDialogState.EXPANDED) {
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
                    onExpand = { state.expand() },
                    onClose = { state.hide() }
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
    onClose: () -> Unit,
    onScrollToAnchor: (String) -> Unit
) {
    val outlineState by detailViewModel.outlineState.collectAsState()

    Surface(
        shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
        color = Color.White,
        shadowElevation = 8.dp,
        modifier = Modifier
            .fillMaxWidth()
            .fillMaxHeight()
            .statusBarsPadding()
            .padding(top = 36.dp)
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
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 0.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
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
                        contentDescription = "outline_collapse".i18n(),
                        tint = Color(0xFF666666),
                        modifier = Modifier.size(24.dp)
                    )
                }

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
                        contentDescription = "btn_close".i18n(),
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
                    outlineState.isLoading && outlineState.isPending -> {
                        LoadingAnimation()
                    }

                    outlineState.error != null -> {
                        ErrorView(error = outlineState.error!!)
                    }

                    else -> {
                        Box(modifier = Modifier.fillMaxSize()) {
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .verticalScroll(rememberScrollState())
                                    .padding(top = 4.dp)
                            ) {
                                MarkdownRenderer(
                                    detailViewModel = detailViewModel,
                                    onLinkClick = { url ->
                                        if (url.startsWith("#")) {
                                            val anchorText = url.removePrefix("#")
                                            onScrollToAnchor(anchorText)
                                            onClose()
                                        }
                                    }
                                )

                                if (outlineState.isLoading) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth().padding(vertical = 20.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.Center
                                    ) {
                                        DotLoadingAnimation()
                                    }
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
    val bookmarkDetail by detailViewModel.bookmarkDetail.collectAsState()

    // 获取 bookmark 标题
    val displayTitle = bookmarkDetail.firstOrNull()?.displayTitle ?: ""

    // 根据状态确定显示内容
    val isLoading = outlineState.isLoading
    val isCompleted = !isLoading && outlineState.outline.isNotEmpty() && outlineState.error == null

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
                    contentDescription = if (isCompleted) "outline_completed".i18n() else "outline_summarizing".i18n(),
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
                            append(if (isCompleted) "outline_completed_prefix".i18n() else "outline_summarizing_prefix".i18n())
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
                        contentDescription = "outline_expand".i18n(),
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
                        contentDescription = "btn_close".i18n(),
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
            text = "outline_empty".i18n(),
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
                text = "outline_error".i18n(),
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

/**
 * 圆点加载组件
 */
@Composable
private fun DotLoadingAnimation() {
    val infiniteTransition = rememberInfiniteTransition(label = "dotLoadingAnimation")

    val dotColors = listOf(
        Color(0xFF16B998),
        Color(0xFFFFC255),
        Color(0xFF56CAF2),
        Color(0xFFFB8F6C)
    )

    val delays = listOf(0, 250, 500, 750)

    Row(
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.height(8.dp) // 留出上下移动的空间
    ) {
        dotColors.forEachIndexed { index, color ->
            val offsetY by infiniteTransition.animateFloat(
                initialValue = 0f,
                targetValue = 0f,
                animationSpec = infiniteRepeatable(
                    animation = keyframes {
                        durationMillis = 1400
                        // 0%, 100%
                        0f at 0
                        // 25%
                        -2.4f at 350  // 1400 * 0.25
                        // 75%
                        2.4f at 1050  // 1400 * 0.75
                        // 100%
                        0f at 1400
                    },
                    repeatMode = RepeatMode.Restart,
                    initialStartOffset = StartOffset(delays[index])
                ),
                label = "offsetY$index"
            )

            Box(
                modifier = Modifier
                    .size(4.dp)
                    .graphicsLayer {
                        translationY = offsetY
                    }
                    .background(
                        color = color,
                        shape = RoundedCornerShape(50)
                    )
            )
        }
    }
}