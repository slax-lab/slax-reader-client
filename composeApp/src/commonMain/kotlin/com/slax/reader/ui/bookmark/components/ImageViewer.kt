package com.slax.reader.ui.bookmark.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp

/**
 * 图片浏览器组件
 */
@Composable
fun ImageViewer(
    imageUrls: List<String>,
    initialImageUrl: String,
    visible: Boolean,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val initialPage = remember(imageUrls, initialImageUrl) {
        imageUrls.indexOf(initialImageUrl).coerceAtLeast(0)
    }

    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(animationSpec = tween(300)),
        exit = fadeOut(animationSpec = tween(300))
    ) {
        ImageViewerContent(
            imageUrls = imageUrls,
            initialPage = initialPage,
            onDismiss = onDismiss,
            modifier = modifier
        )
    }
}

/**
 * 图片浏览器内容
 */
@Composable
private fun ImageViewerContent(
    imageUrls: List<String>,
    initialPage: Int,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val pagerState = rememberPagerState(
        initialPage = initialPage,
        pageCount = { imageUrls.size }
    )

    // 记录每个页面的缩放状态，用于控制 Pager 滑动
    val scaleStates = remember(imageUrls.size) {
        List(imageUrls.size) { mutableStateOf(1f) }
    }

    // 当前页面是否允许滑动（缩放状态下禁用滑动）
    val userScrollEnabled = remember {
        derivedStateOf {
            val currentScale = scaleStates.getOrNull(pagerState.currentPage)?.value ?: 1f
            val enabled = currentScale <= 1f
            enabled
        }
    }.value

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) {
                if (userScrollEnabled) {
                    onDismiss()
                }
            }
    ) {
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxSize(),
            userScrollEnabled = userScrollEnabled,
            verticalAlignment = Alignment.CenterVertically
        ) { page ->
            ZoomableImagePage(
                imageUrl = imageUrls[page],
                scaleState = scaleStates.getOrNull(page) ?: remember { mutableStateOf(1f) },
                onDismiss = onDismiss,
                modifier = Modifier.fillMaxSize()
            )
        }

        if (imageUrls.size > 1) {
            PageIndicator(
                pageCount = imageUrls.size,
                currentPage = pagerState.currentPage,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 40.dp)
            )
        }
    }

    // 监听页面变化，重置非当前页面的缩放状态
    LaunchedEffect(pagerState.currentPage) {
        scaleStates.forEachIndexed { index, state ->
            if (index != pagerState.currentPage && state.value != 1f) {
                state.value = 1f
            }
        }
    }
}

/**
 * 图片显示组件
 */
@Composable
private fun ZoomableImagePage(
    imageUrl: String,
    scaleState: MutableState<Float>,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    println("[watch][UI] recomposition ZoomableImagePage: $imageUrl")

    var scale by scaleState
    var offsetX by remember { mutableFloatStateOf(0f) }
    var offsetY by remember { mutableFloatStateOf(0f) }

    var containerSize by remember { mutableStateOf(IntSize.Zero) }
    var imageSize by remember { mutableStateOf(Size.Zero) }

    var isDoubleTapAnimating by remember { mutableStateOf(false) }
    var animationTargetScale by remember { mutableFloatStateOf(1f) }
    var animationTargetOffsetX by remember { mutableFloatStateOf(0f) }
    var animationTargetOffsetY by remember { mutableFloatStateOf(0f) }

    // 双击时的动画值
    val animatedScale by animateFloatAsState(
        targetValue = if (isDoubleTapAnimating) animationTargetScale else scale,
        animationSpec = tween(300),
        label = "scale",
        finishedListener = { isDoubleTapAnimating = false }
    )

    val animatedOffsetX by animateFloatAsState(
        targetValue = if (isDoubleTapAnimating) animationTargetOffsetX else offsetX,
        animationSpec = tween(300),
        label = "offsetX"
    )

    val animatedOffsetY by animateFloatAsState(
        targetValue = if (isDoubleTapAnimating) animationTargetOffsetY else offsetY,
        animationSpec = tween(300),
        label = "offsetY"
    )

    /**
     * 计算 ContentScale.Fit 模式下图片的实际显示尺寸
     * Fit 模式会保持图片比例，使其完整显示在容器内
     */
    fun calculateFitSize(imageSize: Size, containerSize: IntSize): Size {
        if (imageSize.width == 0f || imageSize.height == 0f ||
            containerSize.width == 0 || containerSize.height == 0
        ) {
            return Size.Zero
        }

        val imageRatio = imageSize.width / imageSize.height
        val containerRatio = containerSize.width.toFloat() / containerSize.height.toFloat()

        return if (imageRatio > containerRatio) {
            // 图片更宽，宽度撑满容器
            val width = containerSize.width.toFloat()
            val height = width / imageRatio
            Size(width, height)
        } else {
            // 图片更高，高度撑满容器
            val height = containerSize.height.toFloat()
            val width = height * imageRatio
            Size(width, height)
        }
    }

    /**
     * 计算精确的拖拽边界
     */
    fun calculateDragBounds(scale: Float): Pair<Float, Float> {
        if (containerSize == IntSize.Zero || imageSize == Size.Zero) {
            return Pair(0f, 0f)
        }

        // 计算图片在 Fit 模式下的实际显示尺寸
        val displaySize = calculateFitSize(imageSize, containerSize)

        // 缩放后的图片尺寸
        val scaledWidth = displaySize.width * scale
        val scaledHeight = displaySize.height * scale

        // 计算可拖拽的最大偏移量
        // 如果缩放后的图片比容器大，可以拖拽；否则限制在容器内
        val maxOffsetX = ((scaledWidth - displaySize.width) / 2f).coerceAtLeast(0f)
        val maxOffsetY = ((scaledHeight - displaySize.height) / 2f).coerceAtLeast(0f)

        return Pair(maxOffsetX, maxOffsetY)
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .onSizeChanged { size ->
                containerSize = size
            }
            .pointerInput("tap_gestures") {
                detectTapGestures(
                    onDoubleTap = {
                        if (scale > 1f) {
                            isDoubleTapAnimating = true
                            animationTargetScale = 1f
                            animationTargetOffsetX = 0f
                            animationTargetOffsetY = 0f

                            scale = 1f
                            offsetX = 0f
                            offsetY = 0f
                        } else {
                            isDoubleTapAnimating = true
                            animationTargetScale = 2.5f
                            animationTargetOffsetX = 0f
                            animationTargetOffsetY = 0f

                            scale = 2.5f
                            offsetX = 0f
                            offsetY = 0f
                        }
                    },
                    onTap = {
                        // 单击且未缩放时关闭
                        if (scale <= 1f) {
                            onDismiss()
                        }
                    }
                )
            }
            .pointerInput("transform_gestures") {
                awaitEachGesture {
                    awaitFirstDown(requireUnconsumed = false)

                    do {
                        val event = awaitPointerEvent()

                        val pointerCount = event.changes.count { it.pressed }
                        if (pointerCount >= 2 || scale > 1f) {
                            // 消费事件并处理缩放
                            var zoom = 1f
                            var pan = Offset.Zero

                            // 计算缩放和平移
                            if (pointerCount >= 2) {
                                zoom = event.calculateZoom()
                                pan = event.calculatePan()
                            } else if (scale > 1f && pointerCount == 1) {
                                // 已缩放且单指：处理拖拽
                                pan = event.calculatePan()
                            }

                            // 检测是否真正发生了缩放或拖拽
                            val hasZoom = zoom != 1f
                            val hasPan = pan != Offset.Zero

                            // 只有在实际进行变换时才停止动画和消费事件
                            if (hasZoom || hasPan) {
                                isDoubleTapAnimating = false

                                // 直接应用变换（无动画延迟，实时响应）
                                val newScale = (scale * zoom).coerceIn(1f, 5f)
                                scale = newScale

                                if (newScale > 1f) {
                                    offsetX += pan.x
                                    offsetY += pan.y

                                    val (maxOffsetX, maxOffsetY) = calculateDragBounds(newScale)
                                    offsetX = offsetX.coerceIn(-maxOffsetX, maxOffsetX)
                                    offsetY = offsetY.coerceIn(-maxOffsetY, maxOffsetY)
                                } else {
                                    offsetX = 0f
                                    offsetY = 0f
                                }

                                if (pointerCount >= 2 || scale > 1f) {
                                    event.changes.forEach { it.consume() }
                                }
                            }
                        }
                    } while (event.changes.any { it.pressed })
                }
            },
        contentAlignment = Alignment.Center
    ) {
        PlatformImage(
            url = imageUrl,
            contentDescription = null,
            contentScale = ContentScale.Fit,
            onImageSize = { size ->
                if (size.width > 0f && size.height > 0f) {
                    imageSize = size
                }
            },
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    scaleX = if (isDoubleTapAnimating) animatedScale else scale
                    scaleY = if (isDoubleTapAnimating) animatedScale else scale
                    translationX = if (isDoubleTapAnimating) animatedOffsetX else offsetX
                    translationY = if (isDoubleTapAnimating) animatedOffsetY else offsetY
                }
        )
    }
}