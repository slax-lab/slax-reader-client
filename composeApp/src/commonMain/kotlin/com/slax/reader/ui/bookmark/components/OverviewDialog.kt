package com.slax.reader.ui.bookmark.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.slax.reader.ui.bookmark.OverviewViewBounds
import kotlin.math.roundToInt

/**
 * Overview 弹窗组件
 */
@Composable
fun OverviewDialog(
    visible: Boolean,
    onDismissRequest: () -> Unit,
    sourceBounds: OverviewViewBounds
) {
    val density = LocalDensity.current

    var screenWidth by remember { mutableStateOf(0f) }
    var screenHeight by remember { mutableStateOf(0f) }

    val animationProgress by animateFloatAsState(
        targetValue = if (visible) 1f else 0f,
        animationSpec = tween(durationMillis = 400, easing = FastOutSlowInEasing),
        label = "dialogAnimation"
    )

    val alpha by animateFloatAsState(
        targetValue = if (visible) 1f else 0f,
        animationSpec = tween(durationMillis = 300),
        label = "alphaAnimation"
    )

    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(animationSpec = tween(300)),
        exit = fadeOut(animationSpec = tween(300))
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .onGloballyPositioned { coordinates ->
                    screenWidth = coordinates.size.width.toFloat()
                    screenHeight = coordinates.size.height.toFloat()
                }
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xCCF5F5F3))
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) {
                        onDismissRequest()
                    }
            )

            val targetWidth = with(density) { (screenWidth - 80.dp.toPx()) }
            val targetHeight = 300f

            val currentWidth = sourceBounds.width + (targetWidth - sourceBounds.width) * animationProgress
            val currentHeight = sourceBounds.height + (targetHeight - sourceBounds.height) * animationProgress

            val targetX = (screenWidth - targetWidth) / 2f
            val targetY = (screenHeight - targetHeight) / 2f

            val currentX = sourceBounds.x + (targetX - sourceBounds.x) * animationProgress
            val currentY = sourceBounds.y + (targetY - sourceBounds.y) * animationProgress

            Box(
                modifier = Modifier.fillMaxSize()
            ) {
                Surface(
                    modifier = Modifier
                        .width(with(density) { currentWidth.toDp() })
                        .height(with(density) { currentHeight.toDp() })
                        .absoluteOffset {
                            IntOffset(
                                x = currentX.roundToInt(),
                                y = currentY.roundToInt()
                            )
                        }
                        .graphicsLayer {
                            this.alpha = alpha
                        },
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
