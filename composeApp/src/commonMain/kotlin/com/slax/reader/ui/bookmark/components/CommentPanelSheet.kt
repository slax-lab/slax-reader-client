package com.slax.reader.ui.bookmark.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.slax.reader.utils.BridgeMarkCommentInfo
import org.jetbrains.compose.resources.painterResource
import slax_reader_client.composeapp.generated.resources.Res
import slax_reader_client.composeapp.generated.resources.ic_comment_panel_close
import slax_reader_client.composeapp.generated.resources.ic_comment_panel_copy
import slax_reader_client.composeapp.generated.resources.ic_comment_panel_highlighted
import slax_reader_client.composeapp.generated.resources.ic_comment_panel_share

/** 评论面板操作按钮的标识 */
object CommentPanelActionId {
    const val COPY = "copy"
    const val HIGHLIGHT = "highlight"
    const val SHARE = "share"
}

/**
 * 划线评论面板底部弹窗
 *
 * 从底部弹出，顶部带20dp圆角，分为4个区域：
 * 1. header区域（关闭按钮）
 * 2. 划线内容显示区（文本 + 操作栏）
 * 3. 评论列表区域（动态高度，最大200dp，超出可滚动）
 * 4. 发表评论区域（输入框，紧贴底部安全区域）
 *
 * @param highlightedText 当前划线选中的文本内容
 * @param visible 面板的显示状态
 * @param onDismiss 关闭面板的回调
 * @param onActionClick 操作栏按钮点击回调，参数为 [CommentPanelActionId] 中定义的标识
 * @param commentListContent 评论列表区域的内容插槽，为空时高度为0
 * @param modifier 外部传入的Modifier
 */
@Composable
fun CommentPanelSheet(
    highlightedText: String,
    visible: Boolean,
    comments: List<BridgeMarkCommentInfo> = emptyList(),
    onDismiss: () -> Unit,
    onActionClick: (actionId: String) -> Unit,
    commentListContent: (@Composable () -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    // 背景遮罩层，带淡入淡出动画
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(animationSpec = tween(300)),
        exit = fadeOut(animationSpec = tween(300))
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.5f))
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onDismiss
                )
        )
    }

    // 底部弹窗主体，从下方滑入
    Box(
        modifier = modifier.fillMaxSize(),
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
                shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
                color = Color.White,
                shadowElevation = 8.dp,
                modifier = Modifier
                    .fillMaxWidth()
                    .wrapContentHeight()
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) { /* 阻止点击事件穿透 */ }
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {

                    // 区域1：Header
                    CommentPanelHeader(onDismiss = onDismiss)

                    // 区域2：划线内容显示区（文本 + 操作栏）
                    HighlightedContentArea(
                        text = highlightedText,
                        onActionClick = onActionClick
                    )

                    // 区域3：评论列表显示区（无内容时高度为0，有内容时自动增高，最大200dp）
                    CommentListArea(
                        content = commentListContent ?: if (comments.isNotEmpty()) {
                            { DefaultCommentList(comments = comments) }
                        } else {
                            null
                        }
                    )

                    // 区域4：发表评论区域
                    PostCommentArea()
                }
            }
        }
    }
}

/**
 * Header区域
 *
 * 高度60dp，右侧带关闭按钮（距右边距20dp，垂直居中）
 */
@Composable
private fun CommentPanelHeader(onDismiss: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(60.dp)
            .padding(end = 20.dp),
        contentAlignment = Alignment.CenterEnd
    ) {
        Icon(
            painter = painterResource(Res.drawable.ic_comment_panel_close),
            contentDescription = "关闭评论面板",
            tint = Color.Unspecified,
            modifier = Modifier
                .size(24.dp)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onDismiss
                )
        )
    }
}

/**
 * 划线内容显示区
 *
 * 从上到下为：划线文本内容 → 操作栏（复制/划线/分享）
 * 文本左右边距20dp，字号15sp，颜色#FF333333，行高24dp，字重Regular
 * 操作栏与文本间距24dp，三个按钮水平居中，按钮间距40dp
 */
@Composable
private fun HighlightedContentArea(
    text: String,
    onActionClick: (actionId: String) -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        // 划线文本内容
        Text(
            text = text,
            style = TextStyle(
                fontSize = 15.sp,
                lineHeight = 24.sp,
                fontWeight = FontWeight.Normal,
                color = Color(0xFF333333)
            ),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
        )

        // 操作栏与文本的间距
        Spacer(modifier = Modifier.height(24.dp))

        // 划线内容操作栏
        HighlightedActionBar(onActionClick = onActionClick)
    }
}

/**
 * 划线内容操作栏
 *
 * 包含复制、划线、分享三个按钮，整体水平+垂直居中，按钮间距40dp
 * 每个按钮：左icon + 右文本，icon与文本间距5dp
 */
@Composable
private fun HighlightedActionBar(onActionClick: (actionId: String) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        HighlightedActionButton(
            iconRes = Res.drawable.ic_comment_panel_copy,
            label = "复制",
            contentDescription = "复制划线内容",
            onClick = { onActionClick(CommentPanelActionId.COPY) }
        )

        Spacer(modifier = Modifier.width(40.dp))

        HighlightedActionButton(
            iconRes = Res.drawable.ic_comment_panel_highlighted,
            label = "划线",
            contentDescription = "添加划线",
            onClick = { onActionClick(CommentPanelActionId.HIGHLIGHT) }
        )

        Spacer(modifier = Modifier.width(40.dp))

        HighlightedActionButton(
            iconRes = Res.drawable.ic_comment_panel_share,
            label = "分享",
            contentDescription = "分享划线内容",
            onClick = { onActionClick(CommentPanelActionId.SHARE) }
        )
    }
}

/**
 * 单个操作按钮：左icon + 右文本，icon与文本间距5dp
 */
@Composable
private fun HighlightedActionButton(
    iconRes: org.jetbrains.compose.resources.DrawableResource,
    label: String,
    contentDescription: String,
    onClick: () -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(5.dp),
        modifier = Modifier.clickable(
            interactionSource = remember { MutableInteractionSource() },
            indication = null,
            onClick = onClick
        )
    ) {
        Icon(
            painter = painterResource(iconRes),
            contentDescription = contentDescription,
            tint = Color.Unspecified
        )
        Text(
            text = label,
            style = TextStyle(
                fontSize = 13.sp,
                color = Color(0xFF333333),
                fontWeight = FontWeight.Normal
            )
        )
    }
}

@Composable
private fun DefaultCommentList(comments: List<BridgeMarkCommentInfo>) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        comments.forEach { comment ->
            CommentItem(comment = comment, depth = 0)
        }
    }
}

@Composable
private fun CommentItem(comment: BridgeMarkCommentInfo, depth: Int) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = (depth * 12).dp)
    ) {
        Text(
            text = comment.username.ifBlank { "未知用户" },
            style = TextStyle(
                fontSize = 13.sp,
                color = Color(0xFF111111),
                fontWeight = FontWeight.Medium,
                lineHeight = 18.sp
            )
        )

        Spacer(modifier = Modifier.height(4.dp))

        val replyPrefix = comment.reply?.username?.takeIf { it.isNotBlank() }?.let { "回复 @$it：" } ?: ""
        Text(
            text = if (comment.isDeleted) "该评论已删除" else replyPrefix + comment.comment,
            style = TextStyle(
                fontSize = 14.sp,
                color = Color(0xFF333333),
                fontWeight = FontWeight.Normal,
                lineHeight = 20.sp
            )
        )

        if (comment.children.isNotEmpty()) {
            Spacer(modifier = Modifier.height(8.dp))
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                comment.children.forEach { child ->
                    CommentItem(comment = child, depth = depth + 1)
                }
            }
        }
    }
}

/**
 * 评论列表显示区域
 *
 * 无内容时高度为0，有内容时自动撑高，最大高度200dp，超出后可滚动查看
 */
@Composable
private fun CommentListArea(content: (@Composable () -> Unit)?) {
    if (content == null) return

    // 最大高度200dp，超出后可通过滚动查看
    val scrollState = rememberScrollState()
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(max = 200.dp)
            .verticalScroll(scrollState)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            content()
        }
    }
}

/**
 * 发表评论区域
 *
 * 背景色 #F2F5F5F3，内部白色容器：上左右间距8dp，下方为底部安全区域
 * 白色容器：默认高度38dp，圆角8dp，左侧圆形头像（24dp），右侧高度自适应输入框
 * 输入框过高后可滚动查看，最大高度100dp
 */
@Composable
private fun PostCommentArea() {
    // 获取设备底部安全区域
    val bottomInset = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFFF5F5F3))
            .padding(start = 8.dp, top = 8.dp, end = 8.dp, bottom = bottomInset)
    ) {
        PostCommentInputContainer()
    }
}

/**
 * 发表评论输入容器
 *
 * 白色背景，圆角8dp，默认高度38dp（内容不足时）
 * 左侧圆形头像24dp，右侧输入框自适应高度，最大100dp
 */
@Composable
private fun PostCommentInputContainer() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 38.dp)
            .background(color = Color.White, shape = RoundedCornerShape(8.dp))
            .padding(horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // 左侧圆形头像占位
        Box(
            modifier = Modifier
                .size(24.dp)
                .clip(CircleShape)
                .background(Color(0xFFE0E0E0))
        )

        Spacer(modifier = Modifier.width(8.dp))

        // 右侧输入框：自适应高度，最大100dp，超出可滚动
        PostCommentTextField(
            modifier = Modifier
                .weight(1f)
                .heightIn(min = 38.dp, max = 100.dp)
        )
    }
}

/**
 * 评论输入框
 *
 * 默认展示占位文字，输入内容后高度自动撑高，超过最大高度后可滚动查看已输入内容
 */
@Composable
private fun PostCommentTextField(modifier: Modifier = Modifier) {
    // 输入内容状态
    val textState = androidx.compose.runtime.remember {
        androidx.compose.runtime.mutableStateOf("")
    }
    val scrollState = rememberScrollState()

    androidx.compose.foundation.text.BasicTextField(
        value = textState.value,
        onValueChange = { textState.value = it },
        textStyle = TextStyle(
            fontSize = 14.sp,
            color = Color(0xFF333333),
            lineHeight = 20.sp,
            fontWeight = FontWeight.Normal
        ),
        modifier = modifier
            .verticalScroll(scrollState),
        decorationBox = { innerTextField ->
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 9.dp),
                contentAlignment = Alignment.CenterStart
            ) {
                // 占位提示文字
                if (textState.value.isEmpty()) {
                    Text(
                        text = "说点什么...",
                        style = TextStyle(
                            fontSize = 14.sp,
                            color = Color(0xFFBBBBBB),
                            lineHeight = 20.sp,
                            fontWeight = FontWeight.Normal
                        )
                    )
                }
                innerTextField()
            }
        }
    )
}
