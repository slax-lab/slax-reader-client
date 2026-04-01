package com.slax.reader.const.component

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import kotlinx.coroutines.delay

/**
 * 带消失动画的容器：先变暗，再收缩淡出。
 *
 * @param isDismissed 触发消失动画
 * @param durationMs  收缩+淡出阶段时长（毫秒）
 * @param onDismissed 动画完全结束后的回调
 * @param content     被包裹的内容
 */
@Composable
fun DismissableItem(
    isDismissed: Boolean,
    durationMs: Int = 600,
    onDismissed: () -> Unit = {},
    content: @Composable () -> Unit,
) {
    var itemVisible by remember { mutableStateOf(true) }
    val overlayAlpha = remember { Animatable(0f) }

    LaunchedEffect(isDismissed) {
        if (isDismissed && itemVisible) {
            delay(50)
            overlayAlpha.animateTo(0.08f, tween(durationMs / 2))
            delay(100)
            itemVisible = false
        }
    }

    AnimatedVisibility(
        visible = itemVisible,
        exit = shrinkVertically(
            animationSpec = tween(durationMs + 100, easing = FastOutSlowInEasing)
        ) + fadeOut(animationSpec = tween(durationMs)),
    ) {
        Box {
            content()
            if (overlayAlpha.value > 0f) {
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .background(Color.Black.copy(alpha = overlayAlpha.value))
                )
            }
        }
    }

    if (!itemVisible) {
        LaunchedEffect(Unit) {
            delay(durationMs + 150L)
            onDismissed()
        }
    }
}