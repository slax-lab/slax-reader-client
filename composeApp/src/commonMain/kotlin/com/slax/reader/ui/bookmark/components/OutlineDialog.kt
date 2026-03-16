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
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.lerp as lerpDp
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

    val transitionState = remember { MutableTransitionState(OutlineDialogStatus.HIDDEN) }
    transitionState.targetState = status
    val transition = rememberTransition(transitionState, label = "outlineTransition")

    // === 背景遮罩透明度 ===
    val bgAlpha by transition.animateFloat(
        label = "bgAlpha",
        transitionSpec = { tween(300) }
    ) { state ->
        if (state == OutlineDialogStatus.EXPANDED) 0.5f else 0f
    }

    // === 展开弹窗透明度（方向感知：EXPANDED↔HIDDEN 标准淡入淡出，EXPANDED↔COLLAPSED 快速配合变形）===
    val expandedAlpha by transition.animateFloat(
        label = "expandedAlpha",
        transitionSpec = {
            when {
                OutlineDialogStatus.HIDDEN isTransitioningTo OutlineDialogStatus.EXPANDED ->
                    tween(250)
                OutlineDialogStatus.COLLAPSED isTransitioningTo OutlineDialogStatus.EXPANDED ->
                    tween(180, delayMillis = 380)
                OutlineDialogStatus.EXPANDED isTransitioningTo OutlineDialogStatus.COLLAPSED ->
                    tween(120)
                OutlineDialogStatus.EXPANDED isTransitioningTo OutlineDialogStatus.HIDDEN ->
                    tween(200)
                else -> snap()
            }
        }
    ) { state -> if (state == OutlineDialogStatus.EXPANDED) 1f else 0f }

    // === 展开弹窗滑动偏移（仅 HIDDEN↔EXPANDED 方向有效，其他方向 snap 到 0）===
    val expandedSlideOffset by transition.animateFloat(
        label = "expandedSlideOffset",
        transitionSpec = {
            when {
                OutlineDialogStatus.HIDDEN isTransitioningTo OutlineDialogStatus.EXPANDED ->
                    tween(300, easing = FastOutSlowInEasing)
                OutlineDialogStatus.EXPANDED isTransitioningTo OutlineDialogStatus.HIDDEN ->
                    tween(250, easing = FastOutSlowInEasing)
                else -> snap()
            }
        }
    ) { state ->
        when (state) {
            OutlineDialogStatus.EXPANDED -> 0f
            OutlineDialogStatus.HIDDEN -> 1f
            else -> 0f
        }
    }

    // === 收缩按钮透明度（方向感知：EXPANDED↔COLLAPSED 时配合变形时序，其他方向标准淡入淡出）===
    val collapsedAlpha by transition.animateFloat(
        label = "collapsedAlpha",
        transitionSpec = {
            when {
                OutlineDialogStatus.EXPANDED isTransitioningTo OutlineDialogStatus.COLLAPSED ->
                    tween(150, delayMillis = 430)
                OutlineDialogStatus.COLLAPSED isTransitioningTo OutlineDialogStatus.EXPANDED ->
                    tween(100)
                OutlineDialogStatus.HIDDEN isTransitioningTo OutlineDialogStatus.COLLAPSED ->
                    tween(200)
                OutlineDialogStatus.COLLAPSED isTransitioningTo OutlineDialogStatus.HIDDEN ->
                    tween(200)
                else -> snap()
            }
        }
    ) { state -> if (state == OutlineDialogStatus.COLLAPSED) 1f else 0f }

    // === 变形进度：0f = 收缩圆形位置，1f = 展开矩形位置（仅 EXPANDED↔COLLAPSED 时动画）===
    val morphProgress by transition.animateFloat(
        label = "morphProgress",
        transitionSpec = {
            when {
                OutlineDialogStatus.EXPANDED isTransitioningTo OutlineDialogStatus.COLLAPSED ->
                    tween(380, delayMillis = 100, easing = FastOutSlowInEasing)
                OutlineDialogStatus.COLLAPSED isTransitioningTo OutlineDialogStatus.EXPANDED ->
                    tween(380, delayMillis = 80, easing = FastOutSlowInEasing)
                else -> snap()
            }
        }
    ) { state -> if (state == OutlineDialogStatus.EXPANDED) 1f else 0f }

    // === 变形遮罩透明度（keyframes 精确控制：仅在 EXPANDED↔COLLAPSED 过渡期间可见）===
    val morphAlpha by transition.animateFloat(
        label = "morphAlpha",
        transitionSpec = {
            when {
                OutlineDialogStatus.EXPANDED isTransitioningTo OutlineDialogStatus.COLLAPSED ->
                    keyframes {
                        durationMillis = 580
                        0f at 0 using LinearEasing
                        1f at 0 using LinearEasing
                        1f at 480 using LinearEasing
                        0f at 580
                    }
                OutlineDialogStatus.COLLAPSED isTransitioningTo OutlineDialogStatus.EXPANDED ->
                    keyframes {
                        durationMillis = 560
                        0f at 0 using LinearEasing
                        1f at 60 using LinearEasing
                        1f at 460 using LinearEasing
                        0f at 560
                    }
                else -> snap()
            }
        }
    ) { 0f }

    // 基于实际 alpha 值控制组合树的存在，alpha=0 时立即移除，
    // 避免 Surface(shadowElevation) 的阴影在 graphicsLayer { alpha=0 } 下仍被平台渲染器绘制
    val expandedVisible = expandedAlpha > 0f
    val collapsedVisible = status == OutlineDialogStatus.COLLAPSED || collapsedAlpha > 0f

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val screenWidth = maxWidth
        val screenHeight = maxHeight
        val density = LocalDensity.current
        val screenHeightPx = with(density) { screenHeight.toPx() }
        val statusBarHeight = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
        val dialogHeight = screenHeight - statusBarHeight - 36.dp

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

        if (morphAlpha > 0f) {
            MorphOverlay(
                morphAlpha = morphAlpha,
                morphProgress = morphProgress,
                screenWidth = screenWidth,
                dialogHeight = dialogHeight
            )
        }

        if (expandedVisible) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        alpha = expandedAlpha
                        translationY = expandedSlideOffset * screenHeightPx
                    },
                contentAlignment = Alignment.BottomCenter
            ) {
                ExpandedOutlineDialog()
            }
        }

        if (collapsedVisible) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(bottom = 58.dp)
                    .offset(x = (-87).dp),
                contentAlignment = Alignment.BottomCenter
            ) {
                CollapsedOutlineButton(collapsedAlpha)
            }
        }
    }
}

/**
 * 变形遮罩层：在 EXPANDED ↔ COLLAPSED 过渡期间，以白色形状模拟容器从矩形变形为圆形的效果。
 *
 * morphProgress：0f = 收缩态（50dp 圆形，位于按钮位置），1f = 展开态（全宽矩形，位于弹窗位置）
 * morphAlpha：由 keyframes 控制，仅在过渡期间可见，静止时始终为 0f。
 */
@Composable
private fun MorphOverlay(
    morphAlpha: Float,
    morphProgress: Float,
    screenWidth: androidx.compose.ui.unit.Dp,
    dialogHeight: androidx.compose.ui.unit.Dp
) {
    val width = lerpDp(50.dp, screenWidth, morphProgress)
    val height = lerpDp(50.dp, dialogHeight, morphProgress)
    val topCorner = lerpDp(25.dp, 20.dp, morphProgress)
    val bottomCorner = lerpDp(25.dp, 0.dp, morphProgress)
    val offsetX = lerpDp((-87).dp, 0.dp, morphProgress)
    val bottomPadding = lerpDp(58.dp, 0.dp, morphProgress)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(bottom = bottomPadding),
        contentAlignment = Alignment.BottomCenter
    ) {
        Surface(
            modifier = Modifier
                .offset(x = offsetX)
                .size(width, height)
                .graphicsLayer { alpha = morphAlpha },
            shape = RoundedCornerShape(
                topStart = topCorner,
                topEnd = topCorner,
                bottomStart = bottomCorner,
                bottomEnd = bottomCorner
            ),
            color = Color.White
        ) {}
    }
}

/**
 * 全屏展开状态的弹窗
 */
@Composable
private fun ExpandedOutlineDialog() {
    val viewModel = koinViewModel<BookmarkDetailViewModel>()
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
private fun CollapsedOutlineButton(animateAlpha: Float = 1f) {
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
            modifier = Modifier.size(50.dp)
                .graphicsLayer { alpha = animateAlpha },
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