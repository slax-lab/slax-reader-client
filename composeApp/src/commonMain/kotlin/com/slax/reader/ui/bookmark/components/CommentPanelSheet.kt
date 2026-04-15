package com.slax.reader.ui.bookmark.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.github.panpf.sketch.rememberAsyncImagePainter
import com.github.panpf.sketch.request.ComposableImageRequest
import com.github.panpf.sketch.request.error
import com.github.panpf.sketch.request.placeholder
import com.slax.reader.utils.BridgeMarkCommentInfo
import com.slax.reader.utils.BridgeMarkItemInfo
import org.jetbrains.compose.resources.painterResource
import slax_reader_client.composeapp.generated.resources.Res
import slax_reader_client.composeapp.generated.resources.global_default_avatar
import slax_reader_client.composeapp.generated.resources.ic_comment_panel_close
import slax_reader_client.composeapp.generated.resources.ic_comment_panel_copy
import slax_reader_client.composeapp.generated.resources.ic_comment_panel_highlight
import slax_reader_client.composeapp.generated.resources.ic_comment_panel_highlighted

/** 评论面板操作按钮的标识 */
object CommentPanelActionId {
    const val COPY = "copy"
    const val HIGHLIGHT = "highlight"
    const val REMOVE_HIGHLIGHT = "remove_highlight"
    const val SHARE = "share"
}

/**
 * 回复目标信息
 *
 * 当用户点击某条评论的"回复"按钮时，记录被回复评论的关键信息。
 * 用于在输入框前显示"回复 XXX："前缀，并在后续提交时指定 parent_id。
 *
 * @param markId 被回复评论的 markId，后续提交回复时作为 parent_id 使用
 * @param username 被回复人的用户名，用于输入框前缀展示
 */
data class ReplyTarget(
    val markId: Long,
    val username: String,
)

/**
 * 划线/高亮文本的下划线样式
 *
 * 根据 mark 数据中 stroke 和 comments 的有无决定：
 * - [SOLID]：有 stroke 数据 → 实线下划线
 * - [DASHED]：无 stroke 但有 comment 数据 → 虚线下划线
 * - [NONE]：两者都没有 → 无下划线
 */
private enum class HighlightUnderlineStyle {
    NONE, SOLID, DASHED
}

/**
 * 划线评论面板底部弹窗
 *
 * 从底部弹出，顶部带20dp圆角，分为4个区域：
 * 1. header区域（关闭按钮）
 * 2. 划线内容显示区（文本 + 下划线 + 操作栏）
 * 3. 评论列表区域（填充剩余空间，可滚动）
 * 4. 发表评论区域（输入框，紧贴底部安全区域）
 *
 * @param highlightedText 当前划线选中的文本内容
 * @param visible 面板的显示状态
 * @param markItemInfo 当前选中的 mark 信息，内含 stroke 和 comments 数据
 * @param highlightLoading 划线/取消划线操作是否正在进行中，为 true 时第二个按钮显示转圈
 * @param userAvatarUrl 当前登录用户的头像 URL
 * @param onDismiss 关闭面板的回调
 * @param onActionClick 操作栏按钮点击回调，参数为 [CommentPanelActionId] 中定义的标识
 * @param commentListContent 评论列表区域的内容插槽，为空时使用默认评论列表
 * @param modifier 外部传入的Modifier
 */
@Composable
fun CommentPanelSheet(
    highlightedText: String,
    visible: Boolean,
    markItemInfo: BridgeMarkItemInfo? = null,
    highlightLoading: Boolean = false,
    commentLoading: Boolean = false,
    userAvatarUrl: String? = null,
    onDismiss: () -> Unit,
    onActionClick: (actionId: String) -> Unit,
    onSubmitComment: (comment: String, parentId: Long?) -> Unit = { _, _ -> },
    commentListContent: (@Composable () -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    // 从 markItemInfo 中提取评论列表和下划线样式
    val comments = markItemInfo?.comments ?: emptyList()
    val isStroked = markItemInfo?.stroke?.isNotEmpty() == true
    val underlineStyle = when {
        isStroked -> HighlightUnderlineStyle.SOLID
        markItemInfo?.comments?.isNotEmpty() == true -> HighlightUnderlineStyle.DASHED
        else -> HighlightUnderlineStyle.NONE
    }

    // 回复目标状态：点击评论的"回复"按钮后记录被回复人信息
    var replyTarget by remember { mutableStateOf<ReplyTarget?>(null) }

    // 面板关闭时自动清除回复状态
    LaunchedEffect(visible) {
        if (!visible) {
            replyTarget = null
        }
    }
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
    BoxWithConstraints(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.BottomCenter
    ) {
        // 弹窗最大高度 = 可用高度 - 80dp
        val maxSheetHeight = maxHeight - 80.dp

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
                    .heightIn(max = maxSheetHeight)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) { /* 阻止点击事件穿透 */ }
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {

                    // 区域1：Header
                    CommentPanelHeader(onDismiss = onDismiss)

                    // 区域2：划线内容显示区（文本 + 下划线 + 操作栏）
                    HighlightedContentArea(
                        text = highlightedText,
                        underlineStyle = underlineStyle,
                        isStroked = isStroked,
                        highlightLoading = highlightLoading,
                        onActionClick = onActionClick
                    )

                    // 区域3：评论列表显示区（填充剩余空间，可滚动）
                    CommentListArea(
                        content = commentListContent ?: if (comments.isNotEmpty()) {
                            {
                                DefaultCommentList(
                                    comments = comments,
                                    onReplyClick = { comment ->
                                        replyTarget = ReplyTarget(
                                            markId = comment.markId,
                                            username = comment.username.ifBlank { "未知用户" }
                                        )
                                    }
                                )
                            }
                        } else {
                            null
                        },
                        modifier = Modifier.weight(1f, fill = false)
                    )

                    // 区域4：发表评论区域
                    PostCommentArea(
                        userAvatarUrl = userAvatarUrl,
                        replyTarget = replyTarget,
                        commentLoading = commentLoading,
                        onClearReplyTarget = { replyTarget = null },
                        onSubmitComment = { comment ->
                            val parentId = replyTarget?.markId
                            replyTarget = null
                            onSubmitComment(comment, parentId)
                        }
                    )
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
            .padding(end = 20.dp)
            .background(Color(0xFFFCFCFC)),
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
 * 从上到下为：划线文本内容（带下划线）→ 操作栏（复制/划线或取消划线）→ 底部间距
 *
 * 文本最多显示2行，超出省略号。根据 [underlineStyle] 在每行文字下方绘制：
 * - SOLID：实线下划线，颜色 #CCB69AFF
 * - DASHED：虚线下划线，颜色 #CCB69AFF
 * - NONE：无下划线
 *
 * @param text 划线选中的文本
 * @param underlineStyle 下划线样式
 * @param isStroked 是否已划线，决定第二个按钮显示"划线"还是"取消划线"
 * @param highlightLoading 划线/取消划线操作是否正在进行中
 * @param onActionClick 操作栏点击回调
 */
@Composable
private fun HighlightedContentArea(
    text: String,
    underlineStyle: HighlightUnderlineStyle = HighlightUnderlineStyle.NONE,
    isStroked: Boolean = false,
    highlightLoading: Boolean = false,
    onActionClick: (actionId: String) -> Unit
) {
    // 复制成功提示状态
    var showCopyToast by remember { mutableStateOf(false) }

    Box(modifier = Modifier.fillMaxWidth().background(Color(0xFFFCFCFC))) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // 划线文本内容（带自定义下划线）
            HighlightedText(
                text = text,
                underlineStyle = underlineStyle,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
            )

            // 操作栏与文本的间距
            Spacer(modifier = Modifier.height(24.dp))

            // 划线内容操作栏
            HighlightedActionBar(
                isStroked = isStroked,
                highlightLoading = highlightLoading,
                onActionClick = { actionId ->
                    if (actionId == CommentPanelActionId.COPY) {
                        showCopyToast = true
                    }
                    onActionClick(actionId)
                }
            )

            // 底部 24dp 内间距
            Spacer(modifier = Modifier.height(24.dp))
        }

        // 复制成功 Toast 提示
        CopySuccessToast(
            visible = showCopyToast,
            onDismiss = { showCopyToast = false },
            modifier = Modifier.align(Alignment.TopCenter).padding(top = 8.dp)
        )
    }
}

/**
 * 带自定义下划线的高亮文本
 *
 * 最多显示2行，超出部分省略号。通过 [onTextLayout] 获取每行的位置信息，
 * 使用 [drawBehind] 在文字下方绘制实线或虚线下划线。
 *
 * @param text 文本内容
 * @param underlineStyle 下划线样式
 * @param modifier 外部修饰符
 */
@Composable
private fun HighlightedText(
    text: String,
    underlineStyle: HighlightUnderlineStyle,
    modifier: Modifier = Modifier
) {
    // 下划线颜色
    val underlineColor = Color(0xFFCCB69A)
    // 虚线路径效果：10px 实线 + 6px 间隔
    val dashPathEffect = remember { PathEffect.dashPathEffect(floatArrayOf(10f, 6f), 0f) }
    // 保存文本布局结果
    var textLayoutResult by remember { mutableStateOf<TextLayoutResult?>(null) }

    Text(
        text = text,
        style = TextStyle(
            fontSize = 15.sp,
            lineHeight = 24.sp,
            fontWeight = FontWeight.Normal,
            color = Color(0xFF333333)
        ),
        maxLines = 2,
        overflow = TextOverflow.Ellipsis,
        onTextLayout = { textLayoutResult = it },
        modifier = modifier.drawBehind {
            val layout = textLayoutResult ?: return@drawBehind
            if (underlineStyle == HighlightUnderlineStyle.NONE) return@drawBehind

            val strokeWidth = 1.dp.toPx()
            // 下划线绘制在每行文字底部偏下 2dp 的位置
            val offsetY = 2.dp.toPx()

            for (lineIndex in 0 until layout.lineCount) {
                val lineLeft = layout.getLineLeft(lineIndex)
                val lineRight = layout.getLineRight(lineIndex)
                val lineBottom = layout.getLineBottom(lineIndex) + offsetY

                drawLine(
                    color = underlineColor,
                    start = Offset(lineLeft, lineBottom),
                    end = Offset(lineRight, lineBottom),
                    strokeWidth = strokeWidth,
                    pathEffect = if (underlineStyle == HighlightUnderlineStyle.DASHED) dashPathEffect else null
                )
            }
        }
    )
}

/**
 * 划线内容操作栏
 *
 * 包含复制和划线/取消划线两个按钮，整体水平+垂直居中，按钮间距40dp。
 * 第二个按钮根据 [isStroked] 状态切换：
 * - 已划线时显示"取消划线"，点击触发 [CommentPanelActionId.REMOVE_HIGHLIGHT]
 * - 未划线时显示"划线"，点击触发 [CommentPanelActionId.HIGHLIGHT]
 *
 * 当 [highlightLoading] 为 true 时，第二个按钮替换为加载转圈，禁止点击。
 *
 * @param isStroked 当前选中文本是否已划线
 * @param highlightLoading 划线/取消划线操作是否正在进行中
 * @param onActionClick 按钮点击回调
 */
@Composable
private fun HighlightedActionBar(
    isStroked: Boolean,
    highlightLoading: Boolean = false,
    onActionClick: (actionId: String) -> Unit,
) {
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

        if (highlightLoading) {
            // 加载中：显示转圈指示器，尺寸与按钮图标一致
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(5.dp),
            ) {
                CircularProgressIndicator(
                    modifier = Modifier.size(18.dp),
                    color = Color(0xCC333333),
                    strokeWidth = 2.dp,
                )
                Text(
                    text = if (isStroked) "取消划线" else "划线",
                    style = TextStyle(
                        fontSize = 14.sp,
                        lineHeight = 20.sp,
                        color = Color(0x66333333),
                        fontWeight = FontWeight.Normal
                    )
                )
            }
        } else if (isStroked) {
            HighlightedActionButton(
                iconRes = Res.drawable.ic_comment_panel_highlighted,
                label = "取消划线",
                contentDescription = "删除已有划线",
                onClick = { onActionClick(CommentPanelActionId.REMOVE_HIGHLIGHT) }
            )
        } else {
            HighlightedActionButton(
                iconRes = Res.drawable.ic_comment_panel_highlight,
                label = "划线",
                contentDescription = "添加划线",
                onClick = { onActionClick(CommentPanelActionId.HIGHLIGHT) }
            )
        }
    }
}

/**
 * 单个操作按钮：左icon + 右文本，icon与文本间距5dp
 * 点击时通过降低透明度实现高亮反馈
 */
@Composable
private fun HighlightedActionButton(
    iconRes: org.jetbrains.compose.resources.DrawableResource,
    label: String,
    contentDescription: String,
    onClick: () -> Unit
) {
    var isPressed by remember { mutableStateOf(false) }
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(5.dp),
        modifier = Modifier
            .alpha(if (isPressed) 0.5f else 1f)
            .pointerInput(Unit) {
                detectTapGestures(
                    onPress = {
                        isPressed = true
                        try {
                            awaitRelease()
                        } finally {
                            isPressed = false
                        }
                    },
                    onTap = { onClick() }
                )
            }
    ) {
        Icon(
            painter = painterResource(iconRes),
            contentDescription = contentDescription,
            tint = Color.Unspecified,
            modifier = Modifier.size(20.dp)
        )
        Text(
            text = label,
            style = TextStyle(
                fontSize = 14.sp,
                lineHeight = 20.sp,
                color = Color(0xCC333333),
                fontWeight = FontWeight.Normal
            )
        )
    }
}

/** 评论列表中通用的元信息文字样式：#FF999999，12sp，行高16.5dp */
private val commentMetaTextStyle = TextStyle(
    fontSize = 12.sp,
    color = Color(0xFF999999),
    fontWeight = FontWeight.Normal,
    lineHeight = 16.5.sp
)

/** 评论内容文字样式：#FF0F1419，15sp，行高24dp */
private val commentBodyTextStyle = TextStyle(
    fontSize = 15.sp,
    color = Color(0xFF0F1419),
    fontWeight = FontWeight.Normal,
    lineHeight = 24.sp
)

/**
 * 评论列表默认实现
 *
 * 承载父级评论单元格的纵向列表
 *
 * @param comments 评论数据列表
 * @param onReplyClick 点击回复按钮的回调，参数为被回复的评论
 */
@Composable
private fun DefaultCommentList(
    comments: List<BridgeMarkCommentInfo>,
    onReplyClick: (BridgeMarkCommentInfo) -> Unit,
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        comments.forEach { comment ->
            CommentCell(comment = comment, onReplyClick = onReplyClick)
        }
    }
}

/**
 * 评论单元格
 *
 * 左右各 20dp 内间距，上方 20dp 间距（无下方间距）。
 * 从上到下由：评论头部、评论内容、子评论列表 三部分组成。
 *
 * @param comment 评论数据
 * @param onReplyClick 点击回复按钮的回调
 */
@Composable
private fun CommentCell(
    comment: BridgeMarkCommentInfo,
    onReplyClick: (BridgeMarkCommentInfo) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 20.dp, top = 20.dp, end = 20.dp)
    ) {
        // 评论头部模块
        CommentItemHeader(comment = comment, onReplyClick = { onReplyClick(comment) })

        // 评论内容模块（距头部 8dp，左侧 28dp 内间距）
        Spacer(modifier = Modifier.height(8.dp))
        CommentItemBody(comment = comment)

        // 子评论列表模块（左侧 20dp 内间距，每个子评论上方 16dp 间距）
        if (comment.children.isNotEmpty()) {
            Column(modifier = Modifier.padding(start = 20.dp)) {
                comment.children.forEach { child ->
                    ChildCommentCell(
                        comment = child,
                        parentComment = comment,
                        onReplyClick = onReplyClick
                    )
                }
            }
        }
    }
}

/**
 * 子评论单元格
 *
 * 上方 16dp 间距。与父评论共享相同的头部布局，内容前增加"回复 XXX："前缀标识被回复人。
 *
 * @param comment 子评论数据
 * @param parentComment 父评论数据，当 reply 信息缺失时用于获取被回复人名称
 * @param onReplyClick 点击回复按钮的回调
 */
@Composable
private fun ChildCommentCell(
    comment: BridgeMarkCommentInfo,
    parentComment: BridgeMarkCommentInfo,
    onReplyClick: (BridgeMarkCommentInfo) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 16.dp, start = 8.dp)
    ) {
        // 子评论头部
        CommentItemHeader(comment = comment, onReplyClick = { onReplyClick(comment) })

        // 子评论内容（距头部 8dp，左侧 28dp 内间距），带"回复 XXX："前缀
        Spacer(modifier = Modifier.height(8.dp))
        ChildCommentItemBody(comment = comment, parentComment = parentComment)
    }
}

/**
 * 评论头部模块
 *
 * 左侧依次为：圆形头像(20dp) → 间距8dp → 名字 → 间距6dp → 竖分割线(9x0.5dp) → 间距6dp → 日期
 * 右侧为回复按钮。所有内容垂直居中。
 *
 * @param comment 评论数据
 * @param onReplyClick 点击回复按钮的回调
 */
@Composable
private fun CommentItemHeader(
    comment: BridgeMarkCommentInfo,
    onReplyClick: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // 圆形头像 20dp
        val avatarPainter = rememberAsyncImagePainter(
            request = ComposableImageRequest(comment.avatar.ifBlank { null }) {
                placeholder(Res.drawable.global_default_avatar)
                error(Res.drawable.global_default_avatar)
            }
        )
        Image(
            painter = avatarPainter,
            contentDescription = "用户头像",
            modifier = Modifier
                .size(20.dp)
                .clip(CircleShape),
            contentScale = ContentScale.Crop
        )

        // 头像与名字间距 8dp
        Spacer(modifier = Modifier.width(8.dp))

        // 名字
        Text(
            text = comment.username.ifBlank { "未知用户" },
            style = commentMetaTextStyle,
            maxLines = 1
        )

        // 名字与分割线间距 6dp
        Spacer(modifier = Modifier.width(6.dp))

        // 竖分割线：高9dp，宽0.5dp，背景 #FFD8D8D8
        Box(
            modifier = Modifier
                .height(9.dp)
                .width(0.5.dp)
                .background(Color(0xFFD8D8D8))
        )

        // 分割线与日期间距 6dp
        Spacer(modifier = Modifier.width(6.dp))

        // 日期（格式 YY-MM-DD HH:mm）
        Text(
            text = formatCommentDate(comment.createdAt),
            style = commentMetaTextStyle,
            maxLines = 1
        )

        // 占满剩余空间，将回复按钮推到右侧
        Spacer(modifier = Modifier.weight(1f))

        // 回复按钮（按下时降低透明度实现高亮）
        var isReplyPressed by remember { mutableStateOf(false) }
        Text(
            text = "回复",
            style = commentMetaTextStyle,
            maxLines = 1,
            modifier = Modifier
                .alpha(if (isReplyPressed) 0.5f else 1f)
                .pointerInput(Unit) {
                    detectTapGestures(
                        onPress = {
                            isReplyPressed = true
                            try {
                                awaitRelease()
                            } finally {
                                isReplyPressed = false
                            }
                        },
                        onTap = { onReplyClick() }
                    )
                }
        )
    }
}

/**
 * 评论内容模块
 *
 * 左侧 28dp 内间距（与头像对齐），字号15sp，行高24dp，颜色 #FF0F1419
 */
@Composable
private fun CommentItemBody(comment: BridgeMarkCommentInfo) {
    Text(
        text = if (comment.isDeleted) "该评论已删除" else comment.comment,
        style = commentBodyTextStyle,
        modifier = Modifier.padding(start = 28.dp)
    )
}

/**
 * 子评论内容模块
 *
 * 在评论文本前增加"回复 XXX："前缀，其中 XXX 为被回复人用户名。
 * "回复 "和"："使用 #FF999999 灰色，XXX 使用 #FF999999 灰色，评论正文使用 #FF0F1419。
 * 被回复人优先取 reply.username，缺失时取 parentComment.username。
 *
 * @param comment 子评论数据
 * @param parentComment 父评论数据，当 reply 信息缺失时用于获取被回复人名称
 */
@Composable
private fun ChildCommentItemBody(
    comment: BridgeMarkCommentInfo,
    parentComment: BridgeMarkCommentInfo,
) {
    if (comment.isDeleted) {
        Text(
            text = "该评论已删除",
            style = commentBodyTextStyle,
            modifier = Modifier.padding(start = 28.dp)
        )
        return
    }

    val replyUsername = comment.reply?.username?.ifBlank { null }
        ?: parentComment.username.ifBlank { "未知用户" }

    Text(
        text = buildAnnotatedString {
            append("回复 ")
            withStyle(SpanStyle(color = Color(0xFF999999))) {
                append(replyUsername)
            }
            append("：")
            append(comment.comment)
        },
        style = commentBodyTextStyle,
        modifier = Modifier.padding(start = 28.dp)
    )
}

/**
 * 将评论时间字符串格式化为 YY-MM-DD HH:mm
 *
 * 支持 ISO 8601 等常见格式，无法解析时原样返回。
 */
private fun formatCommentDate(raw: String): String {
    if (raw.isBlank()) return ""
    return try {
        // 常见格式：2024-03-15T10:30:00Z 或 2024-03-15 10:30:00
        val cleaned = raw.replace("T", " ").replace("Z", "").trim()
        // 至少需要 "YYYY-MM-DD HH:mm" 共 16 个字符
        if (cleaned.length >= 16) {
            val year = cleaned.substring(2, 4)   // YY
            val month = cleaned.substring(5, 7)  // MM
            val day = cleaned.substring(8, 10)    // DD
            val hour = cleaned.substring(11, 13)  // HH
            val minute = cleaned.substring(14, 16) // mm
            "$year-$month-$day $hour:$minute"
        } else if (cleaned.length >= 10) {
            // 只有日期部分
            val year = cleaned.substring(2, 4)
            val month = cleaned.substring(5, 7)
            val day = cleaned.substring(8, 10)
            "$year-$month-$day"
        } else {
            raw
        }
    } catch (_: Exception) {
        raw
    }
}

/**
 * 评论列表显示区域
 *
 * 顶部带 0.5dp 分割线（颜色 #14333333），底部 52dp 内间距。
 * 无内容时高度为0，有内容时自动撑高，受外部 modifier（weight）约束最大高度，超出后可滚动查看。
 *
 * @param content 评论列表内容插槽
 * @param modifier 外部传入的 Modifier，用于控制高度（如 weight）
 */
@Composable
private fun CommentListArea(
    content: (@Composable () -> Unit)?,
    modifier: Modifier = Modifier
) {
    if (content == null) return

    val scrollState = rememberScrollState()
    Column(
        modifier = modifier.fillMaxWidth()
    ) {
        // 顶部分割线：高 0.5dp，颜色 #14333333（8%透明度的深灰）
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(0.5.dp)
                .background(Color(0x14333333))
        )

        // 可滚动评论内容
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f, fill = false)
                .verticalScroll(scrollState)
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                content()

                // 底部 52dp 内间距
                Spacer(modifier = Modifier.height(52.dp))
            }
        }
    }
}

/**
 * 发表评论区域
 *
 * 背景色 #FFF5F5F3，内部白色容器：上左右间距8dp，下方为 bottomInset + 8dp。
 * 键盘弹出时底部额外增加16dp间距，避免输入框紧贴键盘。
 *
 * @param userAvatarUrl 当前登录用户的头像 URL
 * @param replyTarget 当前回复目标，为 null 时表示普通评论模式
 * @param commentLoading 评论提交是否正在进行中
 * @param onClearReplyTarget 清除回复目标的回调（退格删除前缀时触发）
 * @param onSubmitComment 提交评论的回调，参数为清理后的评论文本
 */
@Composable
private fun PostCommentArea(
    userAvatarUrl: String? = null,
    replyTarget: ReplyTarget? = null,
    commentLoading: Boolean = false,
    onClearReplyTarget: () -> Unit = {},
    onSubmitComment: (comment: String) -> Unit = {},
) {
    val bottomInset = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    val imeBottom = WindowInsets.ime.asPaddingValues().calculateBottomPadding()
    // 键盘弹出时额外增加 16dp 间距，避免输入框紧贴键盘顶部
    val keyboardExtraPadding = if (imeBottom > 0.dp) 16.dp else 0.dp

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFFF5F5F3))
            .padding(start = 8.dp, top = 8.dp, end = 8.dp, bottom = bottomInset + 8.dp + keyboardExtraPadding)
    ) {
        PostCommentInputContainer(
            userAvatarUrl = userAvatarUrl,
            replyTarget = replyTarget,
            commentLoading = commentLoading,
            onClearReplyTarget = onClearReplyTarget,
            onSubmitComment = onSubmitComment
        )
    }
}

/**
 * 发表评论输入容器
 *
 * 白色背景，圆角8dp，最小高度38dp。
 * 左侧为当前用户圆形头像(24dp)，距顶部固定7dp（不垂直居中）。
 * 右侧输入框自适应高度，最大100dp。
 *
 * @param userAvatarUrl 当前登录用户的头像 URL
 * @param replyTarget 当前回复目标
 * @param commentLoading 评论提交是否正在进行中
 * @param onClearReplyTarget 清除回复目标的回调
 * @param onSubmitComment 提交评论的回调
 */
@Composable
private fun PostCommentInputContainer(
    userAvatarUrl: String? = null,
    replyTarget: ReplyTarget? = null,
    commentLoading: Boolean = false,
    onClearReplyTarget: () -> Unit = {},
    onSubmitComment: (comment: String) -> Unit = {},
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 38.dp)
            .background(color = Color.White, shape = RoundedCornerShape(8.dp))
            .padding(horizontal = 8.dp)
    ) {
        // 左侧当前用户头像（距顶部固定 7dp，不垂直居中）
        val avatarPainter = rememberAsyncImagePainter(
            request = ComposableImageRequest(userAvatarUrl) {
                placeholder(Res.drawable.global_default_avatar)
                error(Res.drawable.global_default_avatar)
            }
        )
        Image(
            painter = avatarPainter,
            contentDescription = "当前用户头像",
            modifier = Modifier
                .padding(top = 7.dp)
                .size(24.dp)
                .clip(CircleShape),
            contentScale = ContentScale.Crop
        )

        Spacer(modifier = Modifier.width(8.dp))

        // 右侧输入框：自适应高度，最大100dp，超出可滚动
        PostCommentTextField(
            replyTarget = replyTarget,
            commentLoading = commentLoading,
            onClearReplyTarget = onClearReplyTarget,
            onSubmitComment = onSubmitComment,
            modifier = Modifier
                .weight(1f)
                .heightIn(min = 38.dp, max = 100.dp)
        )
    }
}

/**
 * 回复模式下嵌入输入框的零宽空格哨兵字符
 *
 * 用途：当输入框处于回复模式时，始终在文本开头保持一个不可见的零宽空格。
 * 这样即使用户看到的输入框是"空的"，实际文本值为 "\u200B" 而非 ""，
 * 按退格键时会删除该哨兵字符，触发 onValueChange，从而被检测到并清除回复状态。
 *
 * 注意：后续提交评论时，需用 [String.replace] 剥离此字符以获取用户实际输入内容。
 */
private const val REPLY_SENTINEL = "\u200B"

/**
 * 评论输入框
 *
 * 默认展示占位文字"发表评论"，输入内容后高度自动撑高。
 * 键盘右下角显示发送键（ImeAction.Send），按下后提交评论。
 *
 * 回复模式下通过 [VisualTransformation] 在输入文字前内联渲染"@XXX "前缀：
 * - 前缀嵌在文字流中，视觉上与用户输入的文字在同一行自然衔接
 * - "@XXX " 使用 #FF999999 灰色
 * - 字号与输入框一致（14sp）
 * - 前缀不可选中、不可编辑，读取 textFieldValue.text 时不包含前缀
 * - 输入框为空时按退格键，通过 [REPLY_SENTINEL] 哨兵字符检测到退格操作并清除前缀
 *
 * @param replyTarget 当前回复目标，为 null 时显示普通评论模式
 * @param commentLoading 评论提交是否正在进行中
 * @param onClearReplyTarget 清除回复目标的回调
 * @param onSubmitComment 提交评论的回调，参数为清理后的评论文本
 * @param modifier 外部修饰符
 */
@Composable
private fun PostCommentTextField(
    replyTarget: ReplyTarget? = null,
    commentLoading: Boolean = false,
    onClearReplyTarget: () -> Unit = {},
    onSubmitComment: (comment: String) -> Unit = {},
    modifier: Modifier = Modifier,
) {
    var textFieldValue by remember { mutableStateOf(TextFieldValue()) }
    val scrollState = rememberScrollState()

    // 回复目标切换时重置输入内容
    // 回复模式下插入零宽空格哨兵字符，使输入框"非空"，确保退格键能触发 onValueChange
    LaunchedEffect(replyTarget) {
        textFieldValue = if (replyTarget != null) {
            TextFieldValue(REPLY_SENTINEL, selection = TextRange(REPLY_SENTINEL.length))
        } else {
            TextFieldValue()
        }
    }

    // 仅当光标在文本末尾时，自动滚动到底部以跟随最新输入
    LaunchedEffect(textFieldValue) {
        if (textFieldValue.selection.collapsed &&
            textFieldValue.selection.end == textFieldValue.text.length
        ) {
            scrollState.animateScrollTo(scrollState.maxValue)
        }
    }

    val inputTextStyle = TextStyle(
        fontSize = 14.sp,
        color = Color(0xFF333333),
        lineHeight = 20.sp,
        fontWeight = FontWeight.Normal
    )

    // 回复模式下构造 VisualTransformation，在文字前方内联插入带颜色的前缀
    val visualTransformation = remember(replyTarget) {
        if (replyTarget != null) {
            ReplyPrefixVisualTransformation(replyTarget.username)
        } else {
            VisualTransformation.None
        }
    }

    // 提交评论的处理函数：剥离哨兵字符 → 验证非空 → 回调 → 清空输入框
    val handleSubmit = {
        val cleanText = textFieldValue.text.replace(REPLY_SENTINEL, "").trim()
        if (cleanText.isNotEmpty() && !commentLoading) {
            onSubmitComment(cleanText)
            textFieldValue = TextFieldValue()
        }
    }

    androidx.compose.foundation.text.BasicTextField(
        value = textFieldValue,
        onValueChange = { newValue ->
            if (replyTarget != null && !newValue.text.contains(REPLY_SENTINEL)) {
                // 哨兵字符被删除 → 用户在"空"输入框按了退格键 → 清除回复前缀
                onClearReplyTarget()
            } else {
                textFieldValue = newValue
            }
        },
        textStyle = inputTextStyle,
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
        keyboardActions = KeyboardActions(onSend = { handleSubmit() }),
        visualTransformation = visualTransformation,
        modifier = modifier.verticalScroll(scrollState),
        decorationBox = { innerTextField ->
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 9.dp),
                contentAlignment = Alignment.CenterStart
            ) {
                // 占位文字：无回复目标且文本为空时显示"发表评论"
                if (textFieldValue.text.isEmpty() && replyTarget == null) {
                    Text(
                        text = "发表评论",
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

/**
 * 回复前缀视觉变换
 *
 * 通过 [VisualTransformation] 在输入文字前方内联插入"@XXX "前缀。
 * 前缀仅存在于视觉渲染层，不影响实际文本值，光标无法移入前缀区域。
 *
 * 颜色方案：
 * - "@XXX "：#FF999999（灰色）
 *
 * @param username 被回复人的用户名
 */
private class ReplyPrefixVisualTransformation(
    private val username: String,
) : VisualTransformation {

    private val prefix = "@$username "
    private val prefixLength = prefix.length

    override fun filter(text: AnnotatedString): TransformedText {
        val transformed = buildAnnotatedString {
            // "@XXX " 灰色
            withStyle(SpanStyle(color = Color(0xFF999999))) {
                append(prefix)
            }
            // 用户实际输入的文字
            append(text)
        }

        val offsetMapping = object : OffsetMapping {
            override fun originalToTransformed(offset: Int): Int = offset + prefixLength
            override fun transformedToOriginal(offset: Int): Int = (offset - prefixLength).coerceAtLeast(0)
        }

        return TransformedText(transformed, offsetMapping)
    }
}
