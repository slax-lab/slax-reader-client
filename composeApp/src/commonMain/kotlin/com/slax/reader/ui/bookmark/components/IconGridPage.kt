package com.slax.reader.ui.bookmark.components

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.painterResource


/**
 * 单个图标按钮
 */
@Composable
fun IconButton(
    icon: ToolbarIcon,
    onClick: () -> Unit
) {
    println("[watch][UI] recomposition IconButton")

    // 记录按下状态
    var isPressed by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()

    // 缩放动画：按下时缩小到0.85，松开时恢复到1.0
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.85f else 1.0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "scale"
    )

    // 透明度动画：按下时透明度降低到0.6，松开时恢复到1.0
    val alpha by animateFloatAsState(
        targetValue = if (isPressed) 0.6f else 1.0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "alpha"
    )

    Column(
        modifier = Modifier
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
                this.alpha = alpha
            }
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = {
                    // 点击时触发动画
                    isPressed = true
                    // 延迟恢复状态，让动画播放完整
                    coroutineScope.launch {
                        delay(150)
                        isPressed = false
                    }

                    onClick()
                }
            ),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Box(
            modifier = Modifier
                .size(56.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(Color(0xCCFFFFFF)),
            contentAlignment = Alignment.Center
        ) {

            icon.iconRes?.let {
                Icon(
                    painter = painterResource(it),
                    contentDescription = null,
                    tint = Color.Unspecified,
                    modifier = Modifier.size(24.dp)
                )
            }

        }

        Text(
            text = icon.label,
            style = TextStyle(
                fontSize = 12.sp,
                lineHeight = 16.5.sp,
                color = Color(0xCC333333)
            )
        )
    }
}
