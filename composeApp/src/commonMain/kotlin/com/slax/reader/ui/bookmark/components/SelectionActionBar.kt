package com.slax.reader.ui.bookmark.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.dropShadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.shadow.Shadow
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.slax.reader.ui.bookmark.LocalSelectionMenuVisible
import com.slax.reader.utils.AppWebViewState

/** 文本选中时显示的操作菜单项定义 */
data class SelectionAction(
    val id: String,
    val label: String
)

/** 选中菜单的操作标识 */
object SelectionActionId {
    const val COPY = "copy"
    const val HIGHLIGHT = "highlight"
    const val COMMENT = "comment"
    const val CHAT = "chat"
}

/** 构建选中菜单的操作列表 */
@Composable
fun rememberSelectionActions(): List<SelectionAction> {
    return remember {
        listOf(
            SelectionAction(SelectionActionId.COPY, "复制"),
            SelectionAction(SelectionActionId.HIGHLIGHT, "划线"),
            SelectionAction(SelectionActionId.COMMENT, "评论"),
            SelectionAction(SelectionActionId.CHAT, "Chat")
        )
    }
}

/** 处理选中菜单的操作点击 */
fun handleSelectionAction(actionId: String, webViewState: AppWebViewState) {
    when (actionId) {
        SelectionActionId.COPY -> {
            // 通过 JS 执行复制操作，浏览器的 Selection 仍然处于选中状态
            webViewState.evaluateJs("document.execCommand('copy')")
        }
        SelectionActionId.HIGHLIGHT -> {
            // TODO: 划线功能，后续实现
        }
        SelectionActionId.COMMENT -> {
            // TODO: 评论功能，后续实现
        }
        SelectionActionId.CHAT -> {
            // TODO: Chat 功能，后续实现
        }
    }
}

/**
 * 文本选中时在右下角显示的竖条操作菜单
 *
 * 从右侧滑入，包含"复制"、"划线"、"评论"、"Chat"四个操作项，
 * 各项之间用分割线分隔，整体为圆角卡片样式。
 */
@Composable
fun SelectionActionBar(
    visible: Boolean,
    actions: List<SelectionAction>,
    onActionClick: (actionId: String) -> Unit,
    modifier: Modifier = Modifier
) {
    AnimatedVisibility(
        visible = visible,
        enter = slideInHorizontally(
            initialOffsetX = { it },
            animationSpec = tween(250)
        ) + fadeIn(animationSpec = tween(250)),
        exit = slideOutHorizontally(
            targetOffsetX = { it },
            animationSpec = tween(200)
        ) + fadeOut(animationSpec = tween(200)),
        modifier = modifier
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .width(IntrinsicSize.Min)
                .dropShadow(
                    shape = RoundedCornerShape(12.dp),
                    shadow = Shadow(
                        radius = 20.dp,
                        spread = 0.dp,
                        color = Color.Black.copy(alpha = 0.12f),
                        offset = DpOffset(x = 0.dp, y = 4.dp)
                    )
                )
                .background(
                    color = Color.White,
                    shape = RoundedCornerShape(12.dp)
                )
        ) {
            actions.forEachIndexed { index, action ->
                SelectionActionItem(
                    label = action.label,
                    onClick = { onActionClick(action.id) }
                )

                // 除最后一项外，添加分割线
                if (index < actions.size - 1) {
                    HorizontalDivider(
                        color = Color(0xFFE6E6E6),
                        thickness = 0.5.dp,
                        modifier = Modifier.padding(horizontal = 6.dp)
                    )
                }
            }
        }
    }
}

/** 单个操作菜单项 */
@Composable
private fun SelectionActionItem(
    label: String,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        color = Color.Transparent,
        shape = RoundedCornerShape(12.dp)
    ) {
        Text(
            text = label,
            style = TextStyle(
                fontSize = 13.sp,
                lineHeight = 18.sp,
                fontWeight = FontWeight.Medium,
                color = Color(0xFF333333)
            ),
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)
        )
    }
}
