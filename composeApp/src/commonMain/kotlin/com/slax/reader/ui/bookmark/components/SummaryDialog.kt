package com.slax.reader.ui.bookmark.components

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.slax.reader.ui.bookmark.BookmarkDetailViewModel
import kotlinx.coroutines.delay

/**
 * 总结弹窗的三种状态
 */
enum class SummaryDialogState {
    HIDDEN,      // 隐藏
    EXPANDED,    // 展开（全屏弹窗）ƒ
    COLLAPSED    // 收缩（小banner）
}

/**
 * 总结全文弹窗组件
 * 支持三种状态：隐藏、全屏弹窗、收缩为小banner
 */
@Composable
fun SummaryDialog(
    detailViewModel: BookmarkDetailViewModel,
    initialState: SummaryDialogState = SummaryDialogState.HIDDEN,
    onDismissRequest: () -> Unit
) {
    println("[watch][UI] recomposition SummaryDialog")

    var currentState by remember { mutableStateOf(SummaryDialogState.HIDDEN) }
    var visible by remember { mutableStateOf(false) }

    // 初始化时延迟显示，确保动画生效
    LaunchedEffect(Unit) {
        if (initialState != SummaryDialogState.HIDDEN) {
            visible = true
            delay(50L) // 短暂延迟，确保动画触发
            currentState = initialState
        }
    }

    // 监听状态变化
    LaunchedEffect(currentState) {
        if (currentState == SummaryDialogState.HIDDEN) {
            delay(300L) // 等待动画完成
            visible = false
            onDismissRequest()
        }
    }

    if (!visible) {
        return
    }

    Box(modifier = Modifier.fillMaxSize()) {
        // 半透明背景（仅在EXPANDED状态显示）
        AnimatedVisibility(
            visible = currentState == SummaryDialogState.EXPANDED,
            enter = fadeIn(animationSpec = tween(300)),
            exit = fadeOut(animationSpec = tween(300))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.5f))
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) {
                        currentState = SummaryDialogState.HIDDEN
                    }
            )
        }

        // 全屏弹窗（底部对齐）
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.BottomCenter
        ) {
            AnimatedVisibility(
                visible = currentState == SummaryDialogState.EXPANDED,
                enter = slideInVertically(
                    initialOffsetY = { it },
                    animationSpec = tween(300)
                ),
                exit = if (currentState == SummaryDialogState.COLLAPSED) {
                    // 收缩：向上缩小消失（变成banner）
                    slideOutVertically(
                        targetOffsetY = { -it },
                        animationSpec = tween(350)
                    ) + scaleOut(
                        targetScale = 0.3f,
                        animationSpec = tween(350)
                    ) + fadeOut(
                        animationSpec = tween(250)
                    )
                } else {
                    // 关闭：向下滑出
                    slideOutVertically(
                        targetOffsetY = { it },
                        animationSpec = tween(300)
                    )
                }
            ) {
                ExpandedSummaryDialog(
                    onCollapse = { currentState = SummaryDialogState.COLLAPSED },
                    onClose = { currentState = SummaryDialogState.HIDDEN }
                )
            }
        }

        // 小banner（顶部对齐）
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.TopCenter
        ) {
            AnimatedVisibility(
                visible = currentState == SummaryDialogState.COLLAPSED,
                enter = scaleIn(
                    initialScale = 0.3f,
                    animationSpec = tween(350, delayMillis = 100)
                ) + fadeIn(
                    animationSpec = tween(300, delayMillis = 100)
                ),
                exit = if (currentState == SummaryDialogState.EXPANDED) {
                    // 展开：向下放大消失（变成弹窗）
                    slideOutVertically(
                        targetOffsetY = { it },
                        animationSpec = tween(350)
                    ) + scaleOut(
                        targetScale = 3.0f,
                        animationSpec = tween(350)
                    ) + fadeOut(
                        animationSpec = tween(250)
                    )
                } else {
                    // 关闭：原地缩小淡出
                    scaleOut(
                        targetScale = 0.3f,
                        animationSpec = tween(300)
                    ) + fadeOut(
                        animationSpec = tween(250)
                    )
                }
            ) {
                CollapsedSummaryBanner(
                    onExpand = { currentState = SummaryDialogState.EXPANDED },
                    onClose = { currentState = SummaryDialogState.HIDDEN }
                )
            }
        }
    }
}

/**
 * 全屏展开状态的弹窗
 */
@Composable
private fun ExpandedSummaryDialog(
    onCollapse: () -> Unit,
    onClose: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp),
        color = Color.White,
        shadowElevation = 8.dp,
        modifier = Modifier
            .fillMaxWidth()
            .fillMaxHeight(0.65f)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) { /* 阻止点击事件穿透 */ }
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp)
        ) {
            // 顶部控制栏
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp, bottom = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 收缩按钮
                IconButton(onClick = onCollapse) {
                    Text(
                        text = "▼",
                        fontSize = 18.sp,
                        color = Color(0xFF666666)
                    )
                }

                // 关闭按钮
                IconButton(onClick = onClose) {
                    Text(
                        text = "✕",
                        fontSize = 18.sp,
                        color = Color(0xFF666666)
                    )
                }
            }

            // 标题
            Text(
                text = "总结全文",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF333333),
                modifier = Modifier.padding(bottom = 16.dp)
            )

            // 总结内容区域
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xFFF5F5F5), RoundedCornerShape(8.dp))
                    .padding(16.dp)
            ) {
                Text(
                    text = "正在生成文章总结...",
                    fontSize = 14.sp,
                    color = Color(0xFF666666)
                )
            }
        }
    }
}

/**
 * 收缩状态的小banner
 */
@Composable
private fun CollapsedSummaryBanner(
    onExpand: () -> Unit,
    onClose: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = Color.White,
        shadowElevation = 4.dp,
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding() // 添加状态栏间距
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .height(56.dp)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) { /* 阻止点击事件穿透 */ }
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 左边标题
            Text(
                text = "总结全文",
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium,
                color = Color(0xFF333333),
                modifier = Modifier.weight(1f)
            )

            // 右边按钮
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 展开按钮
                IconButton(onClick = onExpand) {
                    Text(
                        text = "▲",
                        fontSize = 16.sp,
                        color = Color(0xFF666666)
                    )
                }

                // 关闭按钮
                IconButton(onClick = onClose) {
                    Text(
                        text = "✕",
                        fontSize = 16.sp,
                        color = Color(0xFF666666)
                    )
                }
            }
        }
    }
}