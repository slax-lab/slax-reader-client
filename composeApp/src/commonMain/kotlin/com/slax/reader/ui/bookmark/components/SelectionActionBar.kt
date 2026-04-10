package com.slax.reader.ui.bookmark.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider
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

/**
 * 处理选中菜单的操作点击
 *
 * @param actionId 操作标识，见 [SelectionActionId]
 * @param webViewState WebView 状态，用于执行 JS 指令
 * @param onCommentRequest 点击"评论"按钮时的回调，由调用方负责显示评论面板
 * @param onHighlightRequest 点击"划线"按钮时的回调，由调用方触发划线流程
 */
fun handleSelectionAction(
    actionId: String,
    webViewState: AppWebViewState,
    onCommentRequest: (() -> Unit)? = null,
    onHighlightRequest: (() -> Unit)? = null,
) {
    when (actionId) {
        SelectionActionId.COPY -> {
            // 通过 JS 执行复制操作，浏览器的 Selection 仍然处于选中状态
            webViewState.evaluateJs("document.execCommand('copy')")
        }
        SelectionActionId.HIGHLIGHT -> {
            onHighlightRequest?.invoke()
        }
        SelectionActionId.COMMENT -> {
            onCommentRequest?.invoke()
        }
        SelectionActionId.CHAT -> {
            // TODO: Chat 功能，后续实现
        }
    }
}

/**
 * 文本选中时在选中区域附近显示的横向操作菜单
 *
 * 水平居中于界面，垂直方向根据选中位置动态定位。
 * 包含"复制"、"划线"、"评论"、"Chat"四个操作项，
 * 各项之间用竖分割线分隔，整体为圆角卡片样式。
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
        enter = slideInVertically(
            initialOffsetY = { it / 2 },
            animationSpec = tween(200)
        ) + fadeIn(animationSpec = tween(200)),
        exit = slideOutVertically(
            targetOffsetY = { it / 2 },
            animationSpec = tween(150)
        ) + fadeOut(animationSpec = tween(150)),
        modifier = modifier
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
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

                // 除最后一项外，添加竖分割线
                if (index < actions.size - 1) {
                    VerticalDivider(
                        color = Color(0xFFE6E6E6),
                        thickness = 0.5.dp,
                        modifier = Modifier
                            .height(16.dp)
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
