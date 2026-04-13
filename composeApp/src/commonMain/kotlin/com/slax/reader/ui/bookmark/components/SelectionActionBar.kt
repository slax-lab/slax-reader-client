package com.slax.reader.ui.bookmark.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.dropShadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.shadow.Shadow
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.slax.reader.utils.AppWebViewState
import com.slax.reader.utils.i18n
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.painterResource
import slax_reader_client.composeapp.generated.resources.Res
import slax_reader_client.composeapp.generated.resources.ic_menu_action_comment
import slax_reader_client.composeapp.generated.resources.ic_menu_action_copy
import slax_reader_client.composeapp.generated.resources.ic_menu_action_highlight

/** 文本选中时显示的操作菜单项定义 */
data class SelectionAction(
    val id: String,
    val label: String,
    val iconRes: DrawableResource
)

/** 选中菜单的操作标识 */
object SelectionActionId {
    const val COPY = "copy"
    const val HIGHLIGHT = "highlight"
    const val COMMENT = "comment"
}

/** 构建选中菜单的操作列表（复制、划线、评论） */
@Composable
fun rememberSelectionActions(): List<SelectionAction> {
    return remember {
        listOf(
            SelectionAction(SelectionActionId.COPY, "selection_action_copy".i18n(), Res.drawable.ic_menu_action_copy),
            SelectionAction(SelectionActionId.HIGHLIGHT, "selection_action_highlight".i18n(), Res.drawable.ic_menu_action_highlight),
            SelectionAction(SelectionActionId.COMMENT, "selection_action_comment".i18n(), Res.drawable.ic_menu_action_comment),
        )
    }
}

/**
 * 处理选中菜单的操作点击
 *
 * @param actionId 操作标识，见 [SelectionActionId]
 * @param webViewState WebView 状态，用于执行 JS 指令
 * @param onHighlightRequest 点击"划线"按钮时的回调，由调用方触发划线流程
 * @param onCommentRequest 点击"评论"按钮时的回调，由调用方显示评论面板
 */
fun handleSelectionAction(
    actionId: String,
    webViewState: AppWebViewState,
    onHighlightRequest: (() -> Unit)? = null,
    onCommentRequest: (() -> Unit)? = null,
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
    }
}

/**
 * 文本选中时在选中区域附近显示的横向操作菜单
 *
 * 深色背景圆角卡片样式，包含图标+文字的菜单项，
 * 支持复制、划线、评论三个操作。
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
                        radius = 10.dp,
                        spread = 0.dp,
                        color = Color(0x1F000000),
                        offset = DpOffset(x = 0.dp, y = 0.dp)
                    )
                )
                .background(
                    color = Color(0xFF333333),
                    shape = RoundedCornerShape(12.dp)
                )
                .padding(horizontal = 6.dp)
        ) {
            actions.forEach { action ->
                SelectionActionItem(
                    iconRes = action.iconRes,
                    label = action.label,
                    onClick = { onActionClick(action.id) }
                )
            }
        }
    }
}

/** 单个操作菜单项：左icon + 右文字 */
@Composable
private fun SelectionActionItem(
    iconRes: DrawableResource,
    label: String,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    Surface(
        onClick = onClick,
        interactionSource = interactionSource,
        color = if (isPressed) Color(0xFF0F1419) else Color.Transparent,
        shape = RoundedCornerShape(6.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(1.dp),
            modifier = Modifier
                .height(32.dp)
                .padding(start = 6.dp, end = 10.dp)
        ) {
            Image(
                painter = painterResource(iconRes),
                contentDescription = label,
                modifier = Modifier.size(20.dp),
                colorFilter = ColorFilter.tint(Color(0xFFF5F5F3))
            )
            Text(
                text = label,
                style = TextStyle(
                    fontSize = 15.sp,
                    lineHeight = 21.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color(0xFFF5F5F3)
                )
            )
        }
    }
}
