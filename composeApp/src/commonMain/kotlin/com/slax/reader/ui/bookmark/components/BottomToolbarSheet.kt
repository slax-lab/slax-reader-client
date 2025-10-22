package com.slax.reader.ui.bookmark.components

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.slax.reader.ui.bookmark.ToolbarIcon


/**
 * 底部弹出工具栏
 * 带显示/隐藏动画，点击外部区域隐藏
 */
@Composable
fun BottomToolbarSheet(
    visible: Boolean,
    onDismissRequest: () -> Unit,
    pages: List<List<ToolbarIcon>>,
    onIconClick: (pageIndex: Int, iconIndex: Int) -> Unit
) {
    // println("[watch][UI] recomposition BottomToolbarSheet")

    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(animationSpec = tween(300)),
        exit = fadeOut(animationSpec = tween(300))
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.0f))
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null
                ) {
                    onDismissRequest()
                }
        )
    }

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.BottomCenter
    ) {
        AnimatedVisibility(
            visible = visible,
            enter = slideInVertically(
                initialOffsetY = { it },
                animationSpec = tween(300)
            ),
            exit = slideOutVertically(
                targetOffsetY = { it },
                animationSpec = tween(300)
            )
        ) {
            Surface(
                color = Color(0xF2F5F5F3),
                border = BorderStroke(0.5.dp, Color(0x140F1419)),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) { /* 阻止点击事件穿透 */ },
                shadowElevation = 8.dp
            ) {
                PagerToolbar(
                    pages = pages,
                    onIconClick = onIconClick,
                    modifier = Modifier.padding(top = 30.dp)
                )
            }
        }
    }
}