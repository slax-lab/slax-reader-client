package com.slax.reader.ui.bookmark.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.StartOffset
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.unit.dp
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin


/**
 * 加载动画组件
 * 显示三个渐变色横条闪动效果
 */
@Composable
fun LoadingAnimation() {
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
 * dot 旋转加载环
 */
@Composable
fun DotLoadingRing(modifier: Modifier = Modifier) {
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
 * 圆点加载组件
 */
@Composable
fun DotLoadingAnimation() {
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