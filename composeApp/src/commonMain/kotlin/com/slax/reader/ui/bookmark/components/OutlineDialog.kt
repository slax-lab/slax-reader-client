package com.slax.reader.ui.bookmark.components

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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.lerp as lerpDp
import androidx.compose.ui.unit.sp
import com.slax.reader.ui.bookmark.BookmarkDetailViewModel
import com.slax.reader.ui.bookmark.states.OutlineDialogStatus
import com.slax.reader.ui.bookmark.states.OutlineDialogStatus.*
import com.slax.reader.utils.i18n
import org.jetbrains.compose.resources.painterResource
import org.koin.compose.viewmodel.koinViewModel
import slax_reader_client.composeapp.generated.resources.Res
import slax_reader_client.composeapp.generated.resources.ic_outline_collapsed
import slax_reader_client.composeapp.generated.resources.ic_outline_dialog_close
import slax_reader_client.composeapp.generated.resources.ic_outline_dialog_shrink
import androidx.compose.ui.draw.dropShadow
import androidx.compose.ui.graphics.shadow.Shadow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first

@Composable
fun OutlineDialog() {
    println("[watch][UI] recomposition OutlineDialog")
    val viewModel = koinViewModel<BookmarkDetailViewModel>()

    val status by viewModel.outlineDelegate.dialogStatus.collectAsState()

    if (status == NONE) return

    val transitionState = remember { MutableTransitionState(HIDDEN) }
    transitionState.targetState = status
    val transition = rememberTransition(transitionState, label = "outlineTransition")

    val anim = transition.outlineAnimations()
    val collapsedVisible = status == COLLAPSED || anim.collapsedAlpha > 0f

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val screenWidth = maxWidth
        val screenHeight = maxHeight
        val density = LocalDensity.current
        val screenHeightPx = with(density) { screenHeight.toPx() }
        val statusBarHeight = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
        val dialogHeight = screenHeight - statusBarHeight - 36.dp

        if (anim.bgAlpha > 0f) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = anim.bgAlpha))
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = { viewModel.outlineDelegate.hideDialog() }
                    )
            )
        }

        if (anim.morphAlpha > 0f) {
            MorphOverlay(
                morphAlpha = anim.morphAlpha,
                morphProgress = anim.morphProgress,
                screenWidth = screenWidth,
                dialogHeight = dialogHeight
            )
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    alpha = anim.expandedAlpha
                    translationY = if (anim.expandedAlpha > 0f) {
                        anim.expandedSlideOffset * screenHeightPx
                    } else {
                        screenHeightPx
                    }
                },
            contentAlignment = Alignment.BottomCenter
        ) {
            ExpandedOutlineDialog()
        }

        if (collapsedVisible) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(bottom = 58.dp)
                    .offset(x = (-87).dp),
                contentAlignment = Alignment.BottomCenter
            ) {
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

                    // 隔离阴影动画
                    Box(modifier = Modifier.graphicsLayer { alpha = anim.collapsedAlpha },
                        contentAlignment = Alignment.Center
                    ) {
                        CollapsedOutlineButton()
                    }
                }
            }
        }
    }
}

/**
 * 变形遮罩层：在 EXPANDED ↔ COLLAPSED 过渡期间，以白色形状模拟容器从矩形变形为圆形的效果。
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
                        SkeletonLoadingAnimation()
                    }

                    outlineState.error != null -> {
                        ErrorView(error = outlineState.error!!)
                    }

                    else -> {
                        val scrollState = rememberScrollState()

                        LaunchedEffect(Unit) {
                            // 恢复滚动位置
                            val savedPos = viewModel.outlineDelegate.savedScrollPosition
                            if (savedPos > 0) {
                                snapshotFlow { scrollState.maxValue }
                                    .first { it > 0 }
                                scrollState.scrollTo(savedPos.coerceAtMost(scrollState.maxValue))
                            }

                            @OptIn(kotlinx.coroutines.FlowPreview::class)
                            snapshotFlow { scrollState.value }
                                .distinctUntilChanged()
                                .debounce(500)
                                .collect { position ->
                                    viewModel.outlineDelegate.saveScrollPosition(position)
                                }
                        }

                        Box(modifier = Modifier.fillMaxSize()) {
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .verticalScroll(scrollState)
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
                                        DotsLineLoadingAnimation()
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
        DotsRingLoadingAnimation(modifier = Modifier.size(27.dp))
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

// Outline 展开收起最小化 过渡动画定义

private data class OutlineAnimations(
    val bgAlpha: Float,
    val expandedAlpha: Float,
    val expandedSlideOffset: Float,
    val collapsedAlpha: Float,
    val morphProgress: Float,
    val morphAlpha: Float,
)

/**
 * 根据过渡方向匹配动画规格，未匹配时回退 snap()
 */
private fun Transition.Segment<OutlineDialogStatus>.specFor(
    vararg pairs: Pair<Pair<OutlineDialogStatus, OutlineDialogStatus>, FiniteAnimationSpec<Float>>
): FiniteAnimationSpec<Float> {
    for ((transition, spec) in pairs) {
        if (transition.first isTransitioningTo transition.second) return spec
    }
    return snap()
}

@Composable
private fun Transition<OutlineDialogStatus>.outlineAnimations(): OutlineAnimations {
    val bgAlpha by animateFloat(
        label = "bgAlpha",
        transitionSpec = { tween(300) }
    ) { if (it == EXPANDED) 0.5f else 0f }

    val expandedAlpha by animateFloat(
        label = "expandedAlpha",
        transitionSpec = {
            specFor(
                (HIDDEN to EXPANDED) to tween(250),
                (COLLAPSED to EXPANDED) to tween(180, delayMillis = 380),
                (EXPANDED to COLLAPSED) to tween(120),
                (EXPANDED to HIDDEN) to tween(200),
            )
        }
    ) { if (it == EXPANDED) 1f else 0f }

    val expandedSlideOffset by animateFloat(
        label = "expandedSlideOffset",
        transitionSpec = {
            specFor(
                (HIDDEN to EXPANDED) to tween(300, easing = FastOutSlowInEasing),
                (EXPANDED to HIDDEN) to tween(250, easing = FastOutSlowInEasing),
            )
        }
    ) { if (it == EXPANDED) 0f else if (it == HIDDEN) 1f else 0f }

    val collapsedAlpha by animateFloat(
        label = "collapsedAlpha",
        transitionSpec = {
            specFor(
                (EXPANDED to COLLAPSED) to tween(150, delayMillis = 430),
                (COLLAPSED to EXPANDED) to tween(100),
                (HIDDEN to COLLAPSED) to tween(200),
                (COLLAPSED to HIDDEN) to tween(200),
            )
        }
    ) { if (it == COLLAPSED) 1f else 0f }

    val morphProgress by animateFloat(
        label = "morphProgress",
        transitionSpec = {
            specFor(
                (EXPANDED to COLLAPSED) to tween(380, delayMillis = 100, easing = FastOutSlowInEasing),
                (COLLAPSED to EXPANDED) to tween(380, delayMillis = 80, easing = FastOutSlowInEasing),
            )
        }
    ) { if (it == EXPANDED) 1f else 0f }

    val morphAlpha by animateFloat(
        label = "morphAlpha",
        transitionSpec = {
            specFor(
                (EXPANDED to COLLAPSED) to keyframes {
                    durationMillis = 580
                    0f at 0 using LinearEasing
                    1f at 0 using LinearEasing
                    1f at 480 using LinearEasing
                    0f at 580
                },
                (COLLAPSED to EXPANDED) to keyframes {
                    durationMillis = 560
                    0f at 0 using LinearEasing
                    1f at 60 using LinearEasing
                    1f at 460 using LinearEasing
                    0f at 560
                },
            )
        }
    ) { 0f }

    return OutlineAnimations(
        bgAlpha = bgAlpha,
        expandedAlpha = expandedAlpha,
        expandedSlideOffset = expandedSlideOffset,
        collapsedAlpha = collapsedAlpha,
        morphProgress = morphProgress,
        morphAlpha = morphAlpha,
    )
}