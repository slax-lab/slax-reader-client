package com.slax.reader.ui.bookmark.components

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.draw.drawBehind
import com.slax.reader.ui.bookmark.BookmarkDetailViewModel
import com.slax.reader.ui.bookmark.states.OutlineDialogStatus
import com.slax.reader.utils.i18n
import org.jetbrains.compose.resources.painterResource
import org.koin.compose.viewmodel.koinViewModel
import slax_reader_client.composeapp.generated.resources.Res
import slax_reader_client.composeapp.generated.resources.ic_outline_collapsed
import slax_reader_client.composeapp.generated.resources.ic_outline_dialog_close
import slax_reader_client.composeapp.generated.resources.ic_outline_dialog_shrink
import androidx.compose.ui.draw.dropShadow
import androidx.compose.ui.graphics.shadow.Shadow
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun OutlineDialog() {
    println("[watch][UI] recomposition OutlineDialog")
    val viewModel = koinViewModel<BookmarkDetailViewModel>()

    val bookmarkId by viewModel.bookmarkId.collectAsState()
    val status by viewModel.outlineDelegate.dialogStatus.collectAsState()

    LaunchedEffect(bookmarkId) {
        if (bookmarkId != null) {
            viewModel.loadOutline()
        }
    }

    if (status == OutlineDialogStatus.NONE) return

    // 使用 updateTransition 驱动协调动画
    val transition = updateTransition(targetState = status, label = "outlineTransition")

    Box(modifier = Modifier.fillMaxSize()) {
        // 1. 背景遮罩（仅 EXPANDED 时可见）
        val bgAlpha by transition.animateFloat(
            label = "bgAlpha",
            transitionSpec = { tween(300) }
        ) { state ->
            if (state == OutlineDialogStatus.EXPANDED) 0.5f else 0f
        }

        if (bgAlpha > 0f) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = bgAlpha))
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = { viewModel.outlineDelegate.hideDialog() }
                    )
            )
        }

        // 2. 展开态弹窗（从底部弹出 + 淡入，向下掉落 + 淡出）
        transition.AnimatedVisibility(
            visible = { it == OutlineDialogStatus.EXPANDED },
            enter = slideInVertically(tween(300, easing = FastOutSlowInEasing)) { it } + fadeIn(tween(250)),
            exit = slideOutVertically(tween(250, easing = FastOutSlowInEasing)) { it } + fadeOut(tween(200))
        ) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.BottomCenter
            ) {
                ExpandedOutlineDialog()
            }
        }

        // 3. 收缩态按钮（简单淡入淡出）
        // 偏移量：整体居中后，按钮中心在屏幕中心左偏 87dp
        // 计算依据：组合总宽 224dp = Button(50) + Gap(12) + FAB(162)
        // Button 中心 = -(224/2 - 50/2) = -87dp
        transition.AnimatedVisibility(
            visible = { it == OutlineDialogStatus.COLLAPSED },
            enter = fadeIn(tween(200)),
            exit = fadeOut(tween(200))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(bottom = 58.dp)
                    .offset(x = (-87).dp),
                contentAlignment = Alignment.BottomCenter
            ) {
                CollapsedOutlineButton()
            }
        }
    }
}
/**
 * 全屏展开状态的弹窗
 */
@Composable
private fun ExpandedOutlineDialog() {
    val viewModel =  koinViewModel<BookmarkDetailViewModel>()
    val outlineState by viewModel.outlineDelegate.outlineState.collectAsState()

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
                .navigationBarsPadding(),
            ) {
            Box(
                modifier = Modifier
                    .padding(top = 32.dp)
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
                                    onLinkClick = { url ->
                                        if (url.startsWith("#")) {
                                            val anchorText = url.removePrefix("#")
                                            viewModel.requestScrollToAnchor(anchorText)
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

                                Spacer(modifier = Modifier.height(50.dp))
                            }
                        }
                    }
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(55.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                val collapseInteractionSource = remember { MutableInteractionSource() }
                val isCollapsePressed by collapseInteractionSource.collectIsPressedAsState()

                Row(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .alpha(if (isCollapsePressed) 0.5f else 1f)
                        .clickable(
                            interactionSource = collapseInteractionSource,
                            indication = null,
                            onClick = { viewModel.outlineDelegate.collapseDialog() }
                        ),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterHorizontally)
                ) {
                    Icon(
                        painter = painterResource(Res.drawable.ic_outline_dialog_shrink),
                        contentDescription = "outline_collapse".i18n(),
                        tint = Color(0xFF666666),
                        modifier = Modifier.size(24.dp)
                    )
                    Text(
                        text = "outline_minimize".i18n(),
                        fontSize = 14.sp,
                        lineHeight = 20.sp,
                        color = Color(0xCC333333)
                    )
                }

                Box(
                    modifier = Modifier
                        .width(1.dp)
                        .height(16.dp)
                        .background(Color(0x14333333))
                )

                val closeInteractionSource = remember { MutableInteractionSource() }
                val isClosePressed by closeInteractionSource.collectIsPressedAsState()

                Row(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .alpha(if (isClosePressed) 0.5f else 1f)
                        .clickable(
                            interactionSource = closeInteractionSource,
                            indication = null,
                            onClick = { viewModel.outlineDelegate.hideDialog() }
                        ),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterHorizontally)
                ) {
                    Icon(
                        painter = painterResource(Res.drawable.ic_outline_dialog_close),
                        contentDescription = "btn_close".i18n(),
                        tint = Color(0xFF666666),
                        modifier = Modifier.size(24.dp)
                    )
                    Text(
                        text = "btn_close".i18n(),
                        fontSize = 14.sp,
                        lineHeight = 20.sp,
                        color = Color(0xCC333333)
                    )
                }
            }
        }
    }
}

/**
 * 收缩态圆形按钮
 */
@Composable
private fun CollapsedOutlineButton() {
    val viewModel = koinViewModel<BookmarkDetailViewModel>()
    val outlineState by viewModel.outlineDelegate.outlineState.collectAsState()
    val isLoading = outlineState.isLoading

    Box(contentAlignment = Alignment.Center) {
        Box(
            modifier = Modifier
                .size(50.dp)
                .offset(y = 10.dp)
                .dropShadow(
                    shape = RoundedCornerShape(25.dp),
                    shadow = Shadow(
                        radius = 40.dp,
                        spread = 0.dp,
                        color = Color.Black.copy(alpha = 0.16f),
                        offset = DpOffset(x = 0.dp, y = 10.dp)
                    )
                )
        )

        Surface(
            onClick = { viewModel.outlineDelegate.expandDialog() },
            modifier = Modifier.size(50.dp),
            color = Color(0xFFFFFFFF),
            shape = RoundedCornerShape(25.dp),
            border = BorderStroke(1.dp, Color.White)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    painter = painterResource(Res.drawable.ic_outline_collapsed),
                    contentDescription = "outline_expand".i18n(),
                    tint = Color.Unspecified,
                    modifier = Modifier.size(20.dp)
                )
            }
        }

        if (isLoading) {
            DotLoadingRing(modifier = Modifier.size(27.dp))
        }
    }
}

/**
 * dot 旋转加载环
 */
@Composable
private fun DotLoadingRing(modifier: Modifier = Modifier) {
    val infiniteTransition = rememberInfiniteTransition(label = "dotRingRotation")
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1500, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "rotation"
    )

    val startColor = Color(0xFF16B998)
    val endColor = Color(0xFFECF9F6)
    val dotCount = 20

    val dotColors = remember {
        List(dotCount) { i -> lerp(startColor, endColor, i.toFloat() / dotCount) }
    }

    Box(
        modifier = modifier.graphicsLayer { rotationZ = rotation }.drawBehind {
            val radius = size.minDimension / 2f
            val dotRadius = 1.5.dp.toPx() / 2f
            val center = androidx.compose.ui.geometry.Offset(size.width / 2f, size.height / 2f)

            for (i in 0 until dotCount) {
                val angle = (2.0 * PI * i / dotCount - PI / 2).toFloat()
                val dotCenter = androidx.compose.ui.geometry.Offset(
                    x = center.x + radius * cos(angle),
                    y = center.y + radius * sin(angle)
                )
                drawCircle(
                    color = dotColors[i],
                    radius = dotRadius,
                    center = dotCenter
                )
            }
        }
    )
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
        modifier = Modifier.height(8.dp)
    ) {
        dotColors.forEachIndexed { index, color ->
            val offsetY by infiniteTransition.animateFloat(
                initialValue = 0f,
                targetValue = 0f,
                animationSpec = infiniteRepeatable(
                    animation = keyframes {
                        durationMillis = 1400
                        0f at 0
                        -2.4f at 350
                        2.4f at 1050
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
