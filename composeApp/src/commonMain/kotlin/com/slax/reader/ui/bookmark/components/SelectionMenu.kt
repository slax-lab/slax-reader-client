package com.slax.reader.ui.bookmark.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.slax.reader.data.model.PositionInfo
import org.jetbrains.compose.resources.painterResource
import slax_reader_client.composeapp.generated.resources.*

/**
 * 文本选择菜单
 * 显示复制、划线、评论等操作
 */
@Composable
fun SelectionMenu(
    position: PositionInfo,
    visible: Boolean = true,
    onCopyClick: () -> Unit,
    onHighlightClick: () -> Unit,
    onCommentClick: () -> Unit,
    onDismiss: () -> Unit
) {
    val density = LocalDensity.current

    // 菜单尺寸
    val menuWidthDp = 280.dp
    val menuHeightDp = 70.dp
    val offsetYDp = 80.dp

    // 计算菜单位置（避免超出屏幕）
    val menuX = with(density) {
        val posX = position.x.dp
        val halfWidth = menuWidthDp / 2
        // 居中显示，但确保不超出屏幕边界
        (posX - halfWidth).coerceIn(8.dp, 400.dp - menuWidthDp - 8.dp)
    }

    val menuY = with(density) {
        val posY = position.y.dp
        // 在选中文本上方显示
        (posY - offsetYDp).coerceAtLeast(8.dp)
    }

    // 动画
    val alpha = remember { Animatable(if (visible) 1f else 0f) }
    val scale = remember { Animatable(if (visible) 1f else 0.8f) }

    LaunchedEffect(visible) {
        if (visible) {
            alpha.animateTo(1f, animationSpec = tween(200))
            scale.animateTo(1f, animationSpec = tween(200))
        } else {
            alpha.animateTo(0f, animationSpec = tween(150))
            scale.animateTo(0.8f, animationSpec = tween(150))
        }
    }

    if (visible || alpha.value > 0f) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Transparent)
                .clickable(
                    onClick = onDismiss,
                    indication = null,
                    interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }
                )
        ) {
            Surface(
                modifier = Modifier
                    .offset(x = menuX, y = menuY)
                    .width(menuWidthDp)
                    .height(menuHeightDp)
                    .graphicsLayer {
                        this.alpha = alpha.value
                        scaleX = scale.value
                        scaleY = scale.value
                    }
                    .shadow(8.dp, RoundedCornerShape(12.dp)),
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 4.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 8.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // 复制按钮
                    MenuActionButton(
                        iconRes = Res.drawable.ic_bottom_panel_share,  // 使用分享图标代替复制
                        label = "复制",
                        onClick = {
                            onCopyClick()
                            onDismiss()
                        }
                    )

                    Divider(
                        modifier = Modifier
                            .width(1.dp)
                            .height(40.dp),
                        color = MaterialTheme.colorScheme.outlineVariant
                    )

                    // 划线按钮
                    MenuActionButton(
                        iconRes = Res.drawable.ic_bottom_panel_underline,
                        label = "划线",
                        onClick = {
                            onHighlightClick()
                            onDismiss()
                        }
                    )

                    Divider(
                        modifier = Modifier
                            .width(1.dp)
                            .height(40.dp),
                        color = MaterialTheme.colorScheme.outlineVariant
                    )

                    // 评论按钮
                    MenuActionButton(
                        iconRes = Res.drawable.ic_bottom_panel_comment,
                        label = "评论",
                        onClick = {
                            onCommentClick()
                            onDismiss()
                        }
                    )
                }
            }
        }
    }
}

/**
 * 标记菜单
 * 点击已有标记时显示
 */
@Composable
fun MarkMenu(
    markId: String,
    position: PositionInfo,
    visible: Boolean = true,
    onViewClick: () -> Unit,
    onDeleteClick: () -> Unit,
    onDismiss: () -> Unit
) {
    val density = LocalDensity.current

    // 菜单尺寸
    val menuWidthDp = 200.dp
    val menuHeightDp = 70.dp
    val offsetYDp = 80.dp

    // 计算菜单位置
    val menuX = with(density) {
        val posX = position.x.dp
        val halfWidth = menuWidthDp / 2
        (posX - halfWidth).coerceIn(8.dp, 400.dp - menuWidthDp - 8.dp)
    }

    val menuY = with(density) {
        val posY = position.y.dp
        (posY - offsetYDp).coerceAtLeast(8.dp)
    }

    // 动画
    val alpha = remember { Animatable(if (visible) 1f else 0f) }
    val scale = remember { Animatable(if (visible) 1f else 0.8f) }

    LaunchedEffect(visible) {
        if (visible) {
            alpha.animateTo(1f, animationSpec = tween(200))
            scale.animateTo(1f, animationSpec = tween(200))
        } else {
            alpha.animateTo(0f, animationSpec = tween(150))
            scale.animateTo(0.8f, animationSpec = tween(150))
        }
    }

    if (visible || alpha.value > 0f) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Transparent)
                .clickable(
                    onClick = onDismiss,
                    indication = null,
                    interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }
                )
        ) {
            Surface(
                modifier = Modifier
                    .offset(x = menuX, y = menuY)
                    .width(menuWidthDp)
                    .height(menuHeightDp)
                    .graphicsLayer {
                        this.alpha = alpha.value
                        scaleX = scale.value
                        scaleY = scale.value
                    }
                    .shadow(8.dp, RoundedCornerShape(12.dp)),
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 4.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 8.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // 查看按钮
                    MenuActionButton(
                        iconRes = Res.drawable.ic_bottom_panel_comment,  // 使用评论图标代替查看
                        label = "查看",
                        onClick = {
                            onViewClick()
                            onDismiss()
                        }
                    )

                    Divider(
                        modifier = Modifier
                            .width(1.dp)
                            .height(40.dp),
                        color = MaterialTheme.colorScheme.outlineVariant
                    )

                    // 删除按钮
                    MenuActionButton(
                        iconRes = Res.drawable.ic_bottom_panel_delete,
                        label = "删除",
                        onClick = {
                            onDeleteClick()
                            onDismiss()
                        },
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }
        }
    }
}

/**
 * 菜单动作按钮
 */
@Composable
private fun MenuActionButton(
    iconRes: org.jetbrains.compose.resources.DrawableResource,
    label: String,
    onClick: () -> Unit,
    tint: Color = MaterialTheme.colorScheme.primary
) {
    Column(
        modifier = Modifier
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            painter = painterResource(iconRes),
            contentDescription = label,
            modifier = Modifier.size(24.dp),
            tint = tint
        )

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = label,
            fontSize = 12.sp,
            fontWeight = FontWeight.Normal,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}
