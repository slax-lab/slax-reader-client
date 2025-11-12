package com.slax.reader.ui.inbox.compenents

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.jetbrains.compose.resources.painterResource
import slax_reader_client.composeapp.generated.resources.*

/**
 * 继续阅读组件
 * 悬浮在列表底部，带显示/隐藏动画
 *
 * @param visible 是否显示
 * @param title 显示的标题文字
 * @param onDismiss 点击关闭图标时的回调（在退出动画完成后调用）
 * @param onClick 点击组件时的回调（可选）
 */
@Composable
fun ContinueReading(
    visible: Boolean,
    title: String,
    onDismiss: () -> Unit,
    onClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    var internalVisible by remember { mutableStateOf(true) }

    LaunchedEffect(visible) {
        if (visible) {
            kotlinx.coroutines.delay(300)
        }

        internalVisible = visible
    }

    LaunchedEffect(internalVisible) {
        if (!internalVisible) {
            kotlinx.coroutines.delay(300)
            onDismiss()
        }
    }

    AnimatedVisibility(
        visible = internalVisible,
        enter = fadeIn(animationSpec = tween(300)) +
                slideInVertically(
                    initialOffsetY = { it / 2 },
                    animationSpec = tween(300)
                ),
        exit = fadeOut(animationSpec = tween(300)) +
                slideOutVertically(
                    targetOffsetY = { it / 2 },
                    animationSpec = tween(300)
                ),
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp)
            .padding(bottom = 8.dp)
            .navigationBarsPadding()
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .shadow(
                    elevation = 40.dp,
                    shape = RoundedCornerShape(16.dp),
                    ambientColor = Color(0x14000000), // rgba(0,0,0,0.08)
                    spotColor = Color(0x14000000)
                )
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            Color(0xFFFCF0E9),
                            Color(0xFFF6F2EF)
                        )
                    ),
                    shape = RoundedCornerShape(16.dp)
                )
                .border(
                    width = 1.dp,
                    color = Color(0x140F1419),
                    shape = RoundedCornerShape(16.dp)
                )
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    enabled = onClick != null
                ) {
                    onClick?.invoke()
                }
                .padding(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Start,
                modifier = Modifier.fillMaxWidth()
            ) {
                Image(
                    painter = painterResource(Res.drawable.ic_continue_reading_icon),
                    contentDescription = "Continue Reading",
                    modifier = Modifier.size(14.dp),
                    contentScale = ContentScale.Fit
                )

                Spacer(modifier = Modifier.width(6.dp))

                Text(
                    text = title,
                    style = TextStyle(
                        fontSize = 15.sp,
                        color = Color(0xFF4d4d4d),
                        lineHeight = 21.sp
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )

                Spacer(modifier = Modifier.width(8.dp))

                Box(
                    modifier = Modifier
                        .width(1.dp)
                        .height(16.dp)
                        .background(Color(0x3DCAA68E))
                )

                Spacer(modifier = Modifier.width(12.dp))

                Image(
                    painter = painterResource(Res.drawable.ic_continue_reading_close),
                    contentDescription = "Close",
                    modifier = Modifier
                        .size(12.dp)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) {
                            internalVisible = false
                        },
                    contentScale = ContentScale.Fit
                )
            }
        }
    }
}