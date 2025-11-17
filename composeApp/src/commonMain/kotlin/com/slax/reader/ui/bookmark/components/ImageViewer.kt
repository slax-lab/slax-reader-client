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
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.github.panpf.sketch.AsyncImage
import com.github.panpf.sketch.rememberAsyncImageState
import kotlinx.coroutines.delay

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

    var internalVisible by remember { mutableStateOf(false) }

    LaunchedEffect(visible) {
        internalVisible = visible
    }

    LaunchedEffect(internalVisible) {
        if (!internalVisible) {
            delay(300)
            onDismiss()
        }
    }


    AnimatedVisibility(
        visible = internalVisible,
        enter = fadeIn(animationSpec = tween(300)),
        exit = fadeOut(animationSpec = tween(300))
    ) {
        ImageViewerContent(
            imageUrls = imageUrls,
            initialPage = initialPage,
            onDismiss = {
                internalVisible = false
            },
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

    // 记录每个页面的缩放状态
    val scaleStates = remember(imageUrls.size) {
        List(imageUrls.size) { mutableStateOf(1f) }
    }

    // 始终允许 Pager 滑动，通过手势事件消费机制精确控制
    val userScrollEnabled = true

    // 监听 Pager 是否正在滚动
    val isPagerScrolling = pagerState.isScrollInProgress

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) {
                onDismiss()
            }
    ) {
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxSize(),
            userScrollEnabled = userScrollEnabled,
            verticalAlignment = Alignment.CenterVertically,
            beyondViewportPageCount = 2
        ) { page ->
            ZoomableImagePage(
                imageUrl = imageUrls[page],
                scaleState = scaleStates.getOrNull(page) ?: remember { mutableStateOf(1f) },
                currentPage = page,
                pageCount = imageUrls.size,
                isPagerScrolling = isPagerScrolling,
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
    // 只在滚动完全停止后才重置，避免翻页动画过程中突变
    LaunchedEffect(pagerState.currentPage, pagerState.isScrollInProgress) {
        if (!pagerState.isScrollInProgress) {
            scaleStates.forEachIndexed { index, state ->
                if (index != pagerState.currentPage && state.value != 1f) {
                    state.value = 1f
                }
            }
        }
    }
}

/**
 * 可缩放的图片页面组件
 */
@Composable
private fun ZoomableImagePage(
    imageUrl: String,
    scaleState: MutableState<Float>,
    currentPage: Int,
    pageCount: Int,
    isPagerScrolling: Boolean,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    println("[watch][UI] recomposition ZoomableImagePage: $imageUrl")

    var scale by scaleState
    var offsetX by remember { mutableFloatStateOf(0f) }
    var offsetY by remember { mutableFloatStateOf(0f) }

    var containerSize by remember { mutableStateOf(IntSize.Zero) }
    var imageSize by remember { mutableStateOf(Size.Zero) }

    val asyncImageState = rememberAsyncImageState()

    var isDoubleTapAnimating by remember { mutableStateOf(false) }
    var animationTargetScale by remember { mutableFloatStateOf(1f) }
    var animationTargetOffsetX by remember { mutableFloatStateOf(0f) }
    var animationTargetOffsetY by remember { mutableFloatStateOf(0f) }

    // 记录是否正在将手势传递给 Pager（用于保持连续的拖拽体验）
    var isPassingToPager by remember { mutableStateOf(false) }

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

    LaunchedEffect(asyncImageState.painter) {
        val painter = asyncImageState.painter
        if (painter != null) {
            val intrinsicSize = painter.intrinsicSize
            if (intrinsicSize.width > 0f && intrinsicSize.height > 0f) {
                imageSize = intrinsicSize
            }
        }
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
                        onDismiss()
                    }
                )
            }
            .pointerInput("transform_gestures") {
                awaitEachGesture {
                    awaitFirstDown(requireUnconsumed = false)

                    // 手势开始时重置状态
                    isPassingToPager = false

                    do {
                        val event = awaitPointerEvent()

                        val pointerCount = event.changes.count { it.pressed }

                        // 如果正在传递给 Pager，继续传递直到手指抬起
                        if (isPassingToPager) {
                            // 不消费事件，让 Pager 持续处理
                            continue
                        }

                        // 如果 Pager 正在滚动且不是用户主动拖拽，忽略图片手势
                        if (isPagerScrolling && pointerCount == 0) {
                            continue
                        }

                        if (pointerCount >= 2 || scale > 1f) {
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

                            // 只有在实际进行变换时才处理
                            if (hasZoom || hasPan) {
                                isDoubleTapAnimating = false

                                // 直接应用缩放
                                val newScale = (scale * zoom).coerceIn(1f, 5f)
                                scale = newScale

                                if (newScale > 1f) {
                                    // 计算拖拽边界
                                    val (maxOffsetX, maxOffsetY) = calculateDragBounds(newScale)

                                    // 检测是否到达边缘且可以触发翻页
                                    val canSwipeToPrevious = currentPage > 0
                                    val canSwipeToNext = currentPage < pageCount - 1

                                    // 边缘阈值（像素）
                                    val edgeThreshold = 20f

                                    // 检测是否在左边缘且尝试继续向右滑动
                                    val atLeftEdge = offsetX >= maxOffsetX - edgeThreshold
                                    val isPanningRight = pan.x > 5f

                                    // 检测是否在右边缘且尝试继续向左滑动
                                    val atRightEdge = offsetX <= -maxOffsetX + edgeThreshold
                                    val isPanningLeft = pan.x < -5f

                                    // 检测水平方向是否主导（防止垂直滑动误触发）
                                    val isHorizontalDominant = kotlin.math.abs(pan.x) > kotlin.math.abs(pan.y) * 1.5f

                                    // 判断是否应该将手势传递给 Pager
                                    val shouldPassToPager = when {
                                        // 在左边缘向右滑动，且可以翻到上一张
                                        atLeftEdge && isPanningRight && isHorizontalDominant && canSwipeToPrevious -> true
                                        // 在右边缘向左滑动，且可以翻到下一张
                                        atRightEdge && isPanningLeft && isHorizontalDominant && canSwipeToNext -> true
                                        else -> false
                                    }

                                    if (shouldPassToPager) {
                                        // 标记为正在传递给 Pager，后续事件都不消费
                                        isPassingToPager = true
                                        // 不调用 consume()，让 Pager 处理滑动
                                    } else {
                                        // 图片内部平移：应用偏移并消费所有事件
                                        offsetX += pan.x
                                        offsetY += pan.y
                                        offsetX = offsetX.coerceIn(-maxOffsetX, maxOffsetX)
                                        offsetY = offsetY.coerceIn(-maxOffsetY, maxOffsetY)

                                        // 消费事件，防止 Pager 响应
                                        event.changes.forEach { it.consume() }
                                    }
                                } else {
                                    // 缩放回 1.0 时重置偏移
                                    offsetX = 0f
                                    offsetY = 0f
                                }

                                // 双指缩放始终消费事件
                                if (pointerCount >= 2) {
                                    event.changes.forEach { it.consume() }
                                }
                            }
                        }
                    } while (event.changes.any { it.pressed })

                    // 手势结束时重置状态
                    isPassingToPager = false
                }
            },
        contentAlignment = Alignment.Center
    ) {
        AsyncImage(
            uri = imageUrl,
            contentDescription = "photo",
            state = asyncImageState,
            modifier = Modifier.fillMaxSize().graphicsLayer {
                scaleX = if (isDoubleTapAnimating) animatedScale else scale
                scaleY = if (isDoubleTapAnimating) animatedScale else scale
                translationX = if (isDoubleTapAnimating) animatedOffsetX else offsetX
                translationY = if (isDoubleTapAnimating) animatedOffsetY else offsetY
            }
        )
    }
}