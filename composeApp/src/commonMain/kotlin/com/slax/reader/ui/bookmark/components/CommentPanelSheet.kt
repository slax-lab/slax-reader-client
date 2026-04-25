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
import androidx.compose.foundation.interaction.collectIsPressedAsState
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
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.Stable
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.style.Hyphens
import androidx.compose.ui.text.style.LineBreak
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.snapshotFlow
import com.slax.reader.utils.setPlainText
import com.slax.reader.utils.toDateTime
import com.slax.reader.utils.toISODateFormat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.layout.positionInParent
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupPositionProvider
import com.github.panpf.sketch.rememberAsyncImagePainter
import com.github.panpf.sketch.request.ComposableImageRequest
import com.github.panpf.sketch.request.error
import com.github.panpf.sketch.request.placeholder
import com.slax.reader.utils.BridgeMarkCommentInfo
import com.slax.reader.utils.BridgeMarkItemInfo
import com.slax.reader.utils.i18n
import com.slax.reader.utils.isIOS
import org.jetbrains.compose.resources.painterResource
import slax_reader_client.composeapp.generated.resources.Res
import slax_reader_client.composeapp.generated.resources.global_default_avatar
import slax_reader_client.composeapp.generated.resources.ic_comment_panel_close
import slax_reader_client.composeapp.generated.resources.ic_comment_panel_copy
import slax_reader_client.composeapp.generated.resources.ic_comment_panel_delete
import slax_reader_client.composeapp.generated.resources.ic_comment_panel_highlight
import slax_reader_client.composeapp.generated.resources.ic_comment_panel_highlighted
import slax_reader_client.composeapp.generated.resources.ic_menu_action_copy

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
 * @param highlightLoading 划线/删除划线操作是否正在进行中，为 true 时第二个按钮显示转圈
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
    panelComments: List<BridgeMarkCommentInfo> = emptyList(),
    highlightLoading: Boolean = false,
    autoFocusInput: Boolean = false,
    userAvatarUrl: String? = null,
    onDismiss: () -> Unit,
    onActionClick: (actionId: String) -> Unit,
    onSubmitComment: (comment: String, replyTarget: ReplyTarget?) -> Unit = { _, _ -> },
    onDeleteComment: (markId: Long) -> Unit = {},
    commentListContent: (@Composable () -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    val isStroked = markItemInfo?.stroke?.isNotEmpty() == true
    val underlineStyle = when {
        isStroked -> HighlightUnderlineStyle.SOLID
        panelComments.isNotEmpty() -> HighlightUnderlineStyle.DASHED
        else -> HighlightUnderlineStyle.NONE
    }

    val state = rememberCommentPanelState()
    state.comments = panelComments

    var minSheetHeightPx by remember { mutableIntStateOf(0) }

    LaunchedEffect(visible) {
        if (!visible) minSheetHeightPx = 0
    }
    // 评论数量变化时重置最小高度约束，使面板能根据内容自然收缩
    LaunchedEffect(panelComments.size) {
        minSheetHeightPx = 0
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
                    onClick = { state.tryDismiss(onDismiss) }
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
                color = Color(0xFFF5F5F3),
                shadowElevation = 8.dp,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(
                        min = with(LocalDensity.current) { minSheetHeightPx.toDp() },
                        max = maxSheetHeight
                    )
                    .onSizeChanged { size ->
                        if (size.height > minSheetHeightPx) {
                            minSheetHeightPx = size.height
                        }
                    }
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) { /* 阻止点击事件穿透 */ }
            ) {
                Box(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.fillMaxWidth().background(Color(0xFFF5F5F3)).let { if (isIOS()) it.imePadding() else it },) {

                    // 区域1：Header
                    CommentPanelHeader(onDismiss = { state.tryDismiss(onDismiss) })

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
                        scrollState = state.scrollState,
                        isProgrammaticScroll = { state.isProgrammaticScroll },
                        content = commentListContent ?: if (panelComments.isNotEmpty()) {
                            {
                                DefaultCommentList(
                                    comments = panelComments,
                                    onReplyClick = { comment ->
                                        state.replyTarget = ReplyTarget(
                                            markId = comment.markId,
                                            username = comment.username.ifBlank { "comment_panel_unknown_user".i18n() }
                                        )
                                    },
                                    onDeleteComment = onDeleteComment,
                                    onCellPositioned = { markId, topY ->
                                        state.recordCellPosition(markId, topY)
                                    }
                                )
                            }
                        } else {
                            null
                        },
                        modifier = Modifier.weight(1f, fill = false).background(Color.White)
                    )

                    // 区域4：发表评论区域
                    PostCommentArea(
                        userAvatarUrl = userAvatarUrl,
                        replyTarget = state.replyTarget,
                        autoFocusInput = autoFocusInput,
                        onClearReplyTarget = { state.replyTarget = null },
                        onHasContentChanged = { state.hasContent = it },
                        onSubmit = { comment ->
                            val target = state.replyTarget
                            state.onPostSubmitted(target?.markId)
                            onSubmitComment(comment, target)
                        }
                    )
                }
                }
            }
        }
    }

    // 放弃评论确认框
    if (state.showDiscardConfirm) {
        AlertDialog(
            onDismissRequest = { state.cancelDiscard() },
            title = { Text("comment_panel_discard_title".i18n()) },
            text = { Text("comment_panel_discard_message".i18n()) },
            containerColor = Color.White,
            confirmButton = {
                TextButton(onClick = { state.confirmDiscard(onDismiss) }) {
                    Text(
                        text = "comment_panel_discard_confirm".i18n(),
                        color = Color(0xFF999999)
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { state.cancelDiscard() }) {
                    Text(
                        text = "comment_panel_discard_cancel".i18n(),
                        color = Color(0xFF16B998)
                    )
                }
            }
        )
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
            .background(Color(0xFFFCFCFC)),
        contentAlignment = Alignment.CenterEnd
    ) {
        Icon(
            painter = painterResource(Res.drawable.ic_comment_panel_close),
            contentDescription = "comment_panel_close".i18n(),
            tint = Color.Unspecified,
            modifier = Modifier
                .padding(end = 20.dp)
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
 * @param text 划线选中的文本
 * @param underlineStyle 下划线样式
 * @param isStroked 是否已划线，决定第二个按钮显示"划线"还是"删除划线"
 * @param highlightLoading 划线/删除划线操作是否正在进行中
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
    Box(modifier = Modifier.fillMaxWidth().background(Color(0xFFFCFCFC))) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // 划线文本内容（带自定义下划线）
            HighlightedText(
                text = text.replace(Regex("\n{2,}"), "\n"),
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
                onActionClick = onActionClick
            )

            // 底部 24dp 内间距
            Spacer(modifier = Modifier.height(24.dp))
        }
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
            color = Color(0xFF333333),
            hyphens = Hyphens.Auto,
            lineBreak = LineBreak.Paragraph,
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
 * @param isStroked 当前选中文本是否已划线
 * @param highlightLoading 划线/删除划线操作是否正在进行中
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
            label = "comment_panel_copy".i18n(),
            contentDescription = "comment_panel_copy_highlight_desc".i18n(),
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
                    text = if (isStroked) "comment_panel_remove_highlight".i18n() else "comment_panel_highlight".i18n(),
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
                label = "comment_panel_remove_highlight".i18n(),
                contentDescription = "comment_panel_remove_highlight_desc".i18n(),
                onClick = { onActionClick(CommentPanelActionId.REMOVE_HIGHLIGHT) }
            )
        } else {
            HighlightedActionButton(
                iconRes = Res.drawable.ic_comment_panel_highlight,
                label = "comment_panel_highlight".i18n(),
                contentDescription = "comment_panel_add_highlight_desc".i18n(),
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
 * @param onDeleteComment 删除评论的回调，参数为被删除评论的 markId
 * @param onCellPositioned 评论单元格布局完成后的位置回调，参数为 markId 和该单元格的顶部 Y 位置
 */
@Composable
private fun DefaultCommentList(
    comments: List<BridgeMarkCommentInfo>,
    onReplyClick: (BridgeMarkCommentInfo) -> Unit,
    onDeleteComment: (Long) -> Unit = {},
    onCellPositioned: (markId: Long, topY: Int) -> Unit = { _, _ -> },
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        comments.forEach { comment ->
            CommentCell(
                comment = comment,
                onReplyClick = onReplyClick,
                onDeleteComment = onDeleteComment,
                onCellPositioned = { topY -> onCellPositioned(comment.markId, topY) }
            )
        }
    }
}

/**
 * 评论单元格
 *
 * @param comment 评论数据
 * @param onReplyClick 点击回复按钮的回调
 * @param onDeleteComment 删除评论的回调，参数为被删除评论的 markId
 * @param onCellPositioned 单元格布局完成后的位置回调，参数为顶部 Y 坐标
 */
@Composable
private fun CommentCell(
    comment: BridgeMarkCommentInfo,
    onReplyClick: (BridgeMarkCommentInfo) -> Unit,
    onDeleteComment: (Long) -> Unit = {},
    onCellPositioned: (topY: Int) -> Unit = {},
) {
    val clipboard = LocalClipboard.current
    val scope = rememberCoroutineScope()
    var isPressed by remember { mutableStateOf(false) }
    var isLongPressed by remember { mutableStateOf(false) }
    var showMenu by remember { mutableStateOf(false) }
    var longPressOffset by remember { mutableStateOf(Offset.Zero) }
    var showCopyToast by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .onGloballyPositioned { coords ->
                onCellPositioned(coords.positionInParent().y.toInt())
            }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(if (isPressed || isLongPressed) Color(0xFFF5F5F3) else Color.Transparent)
                .pointerInput(comment.isDeleted) {
                    detectTapGestures(
                        onPress = {
                            isPressed = true
                            try { awaitRelease() } finally { isPressed = false }
                        },
                        onTap = { if (!comment.isDeleted) onReplyClick(comment) },
                        onLongPress = { offset ->
                            if (!comment.isDeleted) {
                                longPressOffset = offset
                                isLongPressed = true
                                showMenu = true
                            }
                        }
                    )
                }
                .padding(start = 20.dp, top = 20.dp)
        ) {
            CommentItemHeader(comment = comment)
            Spacer(modifier = Modifier.height(8.dp))
            CommentItemBody(comment = comment)
            Spacer(modifier = Modifier.height(8.dp))

            if (comment.children.isNotEmpty()) {
                Column(modifier = Modifier.padding(start = 28.dp)) {
                    comment.children.forEach { child ->
                        ChildCommentCell(
                            comment = child,
                            onReplyClick = onReplyClick,
                            onDeleteComment = onDeleteComment
                        )
                    }
                }
            }
        }

        if (showMenu) {
            CommentContextMenu(
                pressOffset = longPressOffset,
                onCopyClick = {
                    showMenu = false
                    isLongPressed = false
                    scope.launch { clipboard.setPlainText(comment.comment) }
                    showCopyToast = true
                },
                onDeleteClick = {
                    showMenu = false
                    isLongPressed = false
                    onDeleteComment(comment.markId)
                },
                onDismiss = {
                    showMenu = false
                    isLongPressed = false
                }
            )
        }

        CopySuccessToast(
            visible = showCopyToast,
            onDismiss = { showCopyToast = false },
            modifier = Modifier.align(Alignment.TopCenter).padding(top = 8.dp)
        )
    }
}

/**
 * 子评论单元格
 *
 * @param comment 子评论数据
 * @param onReplyClick 点击回复按钮的回调
 * @param onDeleteComment 删除评论的回调，参数为被删除评论的 markId
 */
@Composable
private fun ChildCommentCell(
    comment: BridgeMarkCommentInfo,
    onReplyClick: (BridgeMarkCommentInfo) -> Unit,
    onDeleteComment: (Long) -> Unit = {},
) {
    val clipboard = LocalClipboard.current
    val scope = rememberCoroutineScope()
    var isPressed by remember { mutableStateOf(false) }
    var isLongPressed by remember { mutableStateOf(false) }
    var showMenu by remember { mutableStateOf(false) }
    var longPressOffset by remember { mutableStateOf(Offset.Zero) }
    var showCopyToast by remember { mutableStateOf(false) }

    Box(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(if (isPressed || isLongPressed) Color(0xFFF5F5F3) else Color.Transparent)
                .pointerInput(comment.isDeleted) {
                    detectTapGestures(
                        onPress = {
                            isPressed = true
                            try { awaitRelease() } finally { isPressed = false }
                        },
                        onTap = { if (!comment.isDeleted) onReplyClick(comment) },
                        onLongPress = { offset ->
                            if (!comment.isDeleted) {
                                longPressOffset = offset
                                isLongPressed = true
                                showMenu = true
                            }
                        }
                    )
                }
                .padding(top = 8.dp, start = 8.dp, bottom = 8.dp)
        ) {
            CommentItemHeader(comment = comment)
            Spacer(modifier = Modifier.height(8.dp))
            CommentItemBody(comment = comment)
        }

        if (showMenu) {
            CommentContextMenu(
                pressOffset = longPressOffset,
                onCopyClick = {
                    showMenu = false
                    isLongPressed = false
                    scope.launch { clipboard.setPlainText(comment.comment) }
                    showCopyToast = true
                },
                onDeleteClick = {
                    showMenu = false
                    isLongPressed = false
                    onDeleteComment(comment.markId)
                },
                onDismiss = {
                    showMenu = false
                    isLongPressed = false
                }
            )
        }

        CopySuccessToast(
            visible = showCopyToast,
            onDismiss = { showCopyToast = false },
            modifier = Modifier.align(Alignment.TopCenter).padding(top = 8.dp)
        )
    }
}

/**
 * 评论头部模块
 *
 * @param comment 评论数据
 */
@Composable
private fun CommentItemHeader(
    comment: BridgeMarkCommentInfo,
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(end = 20.dp),
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
            contentDescription = "comment_panel_user_avatar".i18n(),
            modifier = Modifier
                .size(20.dp)
                .clip(CircleShape),
            contentScale = ContentScale.Crop
        )

        Spacer(modifier = Modifier.width(8.dp))

        Text(
            text = comment.username.ifBlank { "comment_panel_unknown_user".i18n() },
            style = commentMetaTextStyle,
            maxLines = 1
        )

        Spacer(modifier = Modifier.width(6.dp))

        Box(
            modifier = Modifier
                .height(9.dp)
                .width(0.5.dp)
                .background(Color(0xFFD8D8D8))
        )

        Spacer(modifier = Modifier.width(6.dp))

        Text(
            text = formatCommentDate(comment.createdAt),
            style = commentMetaTextStyle,
            maxLines = 1
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
        text = if (comment.isDeleted) "comment_panel_deleted".i18n() else comment.comment,
        style = commentBodyTextStyle,
        modifier = Modifier.padding(start = 28.dp, end = 20.dp)
    )
}

/**
 * 评论操作菜单
 *
 * @param pressOffset 长按发生时的局部坐标，用于定位菜单水平位置
 * @param onCopyClick 点击复制菜单项的回调
 * @param onDeleteClick 点击删除菜单项的回调
 * @param onDismiss 菜单关闭回调
 */
@Composable
private fun CommentContextMenu(
    pressOffset: Offset,
    onCopyClick: () -> Unit,
    onDeleteClick: () -> Unit,
    onDismiss: () -> Unit,
) {
    val density = LocalDensity.current
    var confirmingDelete by remember { mutableStateOf(false) }

    // 缓存菜单右边缘和 Y 坐标，动画期间保持右边缘固定，左侧自然收缩
    val cachedRightEdge = remember { intArrayOf(-1) }
    val cachedY = remember { intArrayOf(-1) }

    val positionProvider = remember(pressOffset, density) {
        cachedRightEdge[0] = -1
        cachedY[0] = -1
        object : PopupPositionProvider {
            override fun calculatePosition(
                anchorBounds: IntRect,
                windowSize: IntSize,
                layoutDirection: LayoutDirection,
                popupContentSize: IntSize,
            ): IntOffset {
                if (cachedRightEdge[0] >= 0) {
                    // 右边缘固定，根据当前宽度倒推 x
                    val x = (cachedRightEdge[0] - popupContentSize.width)
                        .coerceIn(0, windowSize.width - popupContentSize.width)
                    return IntOffset(x, cachedY[0])
                }
                val pressScreenX = anchorBounds.left + pressOffset.x.toInt()
                val x = (pressScreenX - popupContentSize.width / 2)
                    .coerceIn(0, windowSize.width - popupContentSize.width)
                val gapPx = with(density) { 8.dp.roundToPx() }
                val y = (anchorBounds.top - popupContentSize.height - gapPx)
                    .coerceAtLeast(0)
                cachedRightEdge[0] = x + popupContentSize.width
                cachedY[0] = y
                return IntOffset(x, y)
            }
        }
    }

    Popup(
        popupPositionProvider = positionProvider,
        onDismissRequest = onDismiss,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .background(
                    color = Color(0xFF333333),
                    shape = RoundedCornerShape(12.dp)
                )
                .clip(RoundedCornerShape(12.dp))
                .padding(horizontal = 6.dp)
        ) {
            if (!confirmingDelete) {
                ContextMenuItem(
                    iconRes = Res.drawable.ic_menu_action_copy,
                    label = "comment_panel_copy".i18n(),
                    onClick = onCopyClick
                )
            }
            ContextMenuItem(
                iconRes = Res.drawable.ic_comment_panel_delete,
                label = if (confirmingDelete) "comment_panel_confirm_delete".i18n() else "comment_panel_delete".i18n(),
                onClick = {
                    if (confirmingDelete) {
                        onDeleteClick()
                    } else {
                        confirmingDelete = true
                    }
                },
                applyTint = false
            )
        }
    }
}

/**
 * 长按菜单的单个操作项
 *
 */
@Composable
private fun ContextMenuItem(
    iconRes: org.jetbrains.compose.resources.DrawableResource,
    label: String,
    onClick: () -> Unit,
    applyTint: Boolean = true,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    Surface(
        onClick = onClick,
        interactionSource = interactionSource,
        color = if (isPressed) Color(0xFF424242) else Color.Transparent,
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
                colorFilter = if (applyTint) ColorFilter.tint(Color(0xFFF5F5F3)) else null
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

/**
 * 将评论时间字符串格式化为 YYYY-MM-DD HH:mm（本地时区）
 *
 * 复用 Time.kt 的 toDateTime() 进行 UTC → 本地时区转换，无法解析时原样返回。
 */
private fun formatCommentDate(raw: String): String {
    if (raw.isBlank()) return ""
    return try {
        raw.toDateTime().toISODateFormat().take(16)
    } catch (_: Exception) {
        raw
    }
}

/**
 * 评论列表显示区域
 *
 * @param scrollState 滚动状态，由外部提供以支持滚动控制
 * @param content 评论列表内容插槽
 * @param modifier 外部传入的 Modifier，用于控制高度（如 weight）
 */
@Composable
private fun CommentListArea(
    scrollState: ScrollState,
    content: (@Composable () -> Unit)?,
    isProgrammaticScroll: () -> Boolean = { false },
    modifier: Modifier = Modifier
) {
    if (content == null) return

    val focusManager = LocalFocusManager.current

    LaunchedEffect(scrollState) {
        snapshotFlow { scrollState.isScrollInProgress }
            .collect { isScrolling ->
                if (isScrolling && !isProgrammaticScroll()) focusManager.clearFocus()
            }
    }

    Column(
        modifier = modifier.fillMaxWidth()
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(0.5.dp)
                .background(Color(0x14333333))
        )

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f, fill = false)
                .verticalScroll(scrollState)
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                content()
                Spacer(modifier = Modifier.height(52.dp))
            }
        }
    }
}

@Composable
private fun PostCommentArea(
    userAvatarUrl: String? = null,
    replyTarget: ReplyTarget? = null,
    autoFocusInput: Boolean = false,
    onClearReplyTarget: () -> Unit = {},
    onHasContentChanged: (Boolean) -> Unit = {},
    onSubmit: (comment: String) -> Unit = {},
) {
    val bottomInset = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    val imeBottom = WindowInsets.ime.asPaddingValues().calculateBottomPadding()

    // iOS 上键盘显示时，导航栏（home indicator）在键盘后面，不需要为其预留空间
    val keyboardSpacing = if (imeBottom > 0.dp && !isIOS()) 8.dp else 0.dp
    val effectiveBottomInset = if (isIOS() && imeBottom > 0.dp) 0.dp else bottomInset

    // 纯净的用户输入文本（不含前缀），是提交给接口的真实内容
    var realInputText by remember { mutableStateOf("") }

    // 回复目标切换时清空用户已输入的内容
    LaunchedEffect(replyTarget) {
        realInputText = ""
    }

    val hasContent by remember {
        derivedStateOf { realInputText.trim().isNotBlank() }
    }

    // 将 hasContent 变化通知给父级，用于拦截关闭操作
    LaunchedEffect(hasContent) {
        onHasContentChanged(hasContent)
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFFF5F5F3))
            .padding(start = 8.dp, top = 8.dp, end = 8.dp, bottom = effectiveBottomInset + 8.dp + keyboardSpacing),
        verticalAlignment = Alignment.Top
    ) {
        PostCommentInputContainer(
            userAvatarUrl = userAvatarUrl,
            replyTarget = replyTarget,
            autoFocusInput = autoFocusInput,
            realInputText = realInputText,
            onRealInputTextChange = { realInputText = it },
            onClearReplyTarget = onClearReplyTarget,
            modifier = Modifier.weight(1f)
        )

        Spacer(modifier = Modifier.width(8.dp))

        val sendInteractionSource = remember { MutableInteractionSource() }
        val isSendPressed by sendInteractionSource.collectIsPressedAsState()
        val backgroundColor = if (hasContent) Color(0xFF16B998) else Color(0x29999999)
        val textColor = if (hasContent) Color.White else Color(0xFF999999)
        val sendButtonAlpha = if (hasContent && isSendPressed) 0.7f else 1f

        Box(
            modifier = Modifier
                .height(38.dp)
                .alpha(sendButtonAlpha)
                .background(backgroundColor, RoundedCornerShape(8.dp))
                .clickable(
                    enabled = hasContent,
                    interactionSource = sendInteractionSource,
                    indication = null
                ) {
                    val trimmed = realInputText.trim()
                    if (trimmed.isNotBlank()) {
                        onSubmit(trimmed)
                        realInputText = ""
                    }
                }
                .padding(horizontal = 11.5.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "comment_panel_send".i18n(),
                style = TextStyle(
                    fontSize = 16.sp,
                    lineHeight = 26.sp,
                    color = textColor
                )
            )
        }
    }
}

@Composable
private fun PostCommentInputContainer(
    userAvatarUrl: String? = null,
    replyTarget: ReplyTarget? = null,
    autoFocusInput: Boolean = false,
    realInputText: String,
    onRealInputTextChange: (String) -> Unit,
    onClearReplyTarget: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .heightIn(min = 38.dp)
            .background(color = Color.White, shape = RoundedCornerShape(8.dp))
            .padding(horizontal = 8.dp)
    ) {
        val avatarPainter = rememberAsyncImagePainter(
            request = ComposableImageRequest(userAvatarUrl) {
                placeholder(Res.drawable.global_default_avatar)
                error(Res.drawable.global_default_avatar)
            }
        )
        Image(
            painter = avatarPainter,
            contentDescription = "comment_panel_current_user_avatar".i18n(),
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
            autoFocusInput = autoFocusInput,
            realInputText = realInputText,
            onRealInputTextChange = onRealInputTextChange,
            onClearReplyTarget = onClearReplyTarget,
            modifier = Modifier
                .weight(1f)
                .heightIn(min = 38.dp, max = 100.dp)
        )
    }
}

/**
 * 回复评论输入框
 *
 * @param replyTarget 当前回复的目标（null 表示普通发评论模式）
 * @param autoFocusInput 是否在首次组合时自动请求输入焦点
 * @param realInputText 用户输入的纯净文本（不含前缀），由父级持有
 * @param onRealInputTextChange 纯净文本变化回调
 * @param onClearReplyTarget 用户通过退格主动清除回复状态时的回调
 */
@Composable
private fun PostCommentTextField(
    replyTarget: ReplyTarget? = null,
    autoFocusInput: Boolean = false,
    realInputText: String,
    onRealInputTextChange: (String) -> Unit,
    onClearReplyTarget: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val scrollState = rememberScrollState()
    val focusRequester = remember { androidx.compose.ui.focus.FocusRequester() }

    // 前缀字符串，仅在 replyTarget 变化时重新计算
    val prefix = remember(replyTarget) {
        replyTarget?.let { "${"comment_panel_reply_prefix".i18n()}${it.username}：" } ?: ""
    }

    // 输入框底层的 TextFieldValue，前缀与用户输入拼接为完整字符串
    var textFieldValue by remember {
        mutableStateOf(TextFieldValue(text = prefix + realInputText))
    }

    // 同步外部状态到内部（应对父级主动清空 realInputText、切换 replyTarget 等场景）
    LaunchedEffect(prefix, realInputText) {
        val expected = prefix + realInputText
        if (textFieldValue.text != expected) {
            textFieldValue = TextFieldValue(
                text = expected,
                selection = TextRange(expected.length)
            )
        }
    }

    LaunchedEffect(autoFocusInput) {
        if (autoFocusInput) {
            // 延迟等待面板动画和布局完成后再请求焦点，避免键盘弹起时面板尚未渲染导致的遮挡问题
            delay(300)
            focusRequester.requestFocus()
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

    // 视觉变换：对已内嵌在真实字符串中的前缀部分做差异化着色
    val visualTransformation = remember(prefix) {
        if (prefix.isNotEmpty()) ReplyPrefixVisualTransformation(prefix) else VisualTransformation.None
    }

    BasicTextField(
        value = textFieldValue,
        onValueChange = { newValue ->
            if (prefix.isNotEmpty()) {
                when {
                    // 全选删除等操作导致文本完全清空 → 清除回复状态
                    newValue.text.isEmpty() -> {
                        onClearReplyTarget()
                        onRealInputTextChange("")
                        return@BasicTextField
                    }
                    // 前缀被破坏（用户在内容为空时继续退格）→ 清除回复状态，保留已输入内容
                    !newValue.text.startsWith(prefix) -> {
                        onClearReplyTarget()
                        // 剔除前缀残留，尽力保留用户已输入的部分
                        val residual = newValue.text.removePrefix(
                            prefix.commonPrefixWith(newValue.text)
                        )
                        textFieldValue = TextFieldValue(
                            text = residual,
                            selection = TextRange(residual.length)
                        )
                        onRealInputTextChange(residual)
                        return@BasicTextField
                    }
                    // 正常输入：强制将选区限制在前缀末尾之后，防止光标侵入前缀区域
                    else -> {
                        val safeSelection = TextRange(
                            start = newValue.selection.start.coerceAtLeast(prefix.length),
                            end = newValue.selection.end.coerceAtLeast(prefix.length)
                        )
                        textFieldValue = newValue.copy(selection = safeSelection)
                        val newRealText = newValue.text.substring(prefix.length)
                        if (newRealText != realInputText) {
                            onRealInputTextChange(newRealText)
                        }
                    }
                }
            } else {
                // 普通模式：直接透传
                textFieldValue = newValue
                if (newValue.text != realInputText) {
                    onRealInputTextChange(newValue.text)
                }
            }
        },
        textStyle = inputTextStyle,
        visualTransformation = visualTransformation,
        modifier = modifier
            .focusRequester(focusRequester)
            .verticalScroll(scrollState),
        decorationBox = { innerTextField ->
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 9.dp),
                contentAlignment = Alignment.CenterStart
            ) {
                // 占位文字：无回复目标且用户尚未输入内容时显示
                if (realInputText.isEmpty() && replyTarget == null) {
                    Text(
                        text = "comment_panel_placeholder".i18n(),
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
 * 将已内嵌在真实字符串头部的前缀做差异化着色：
 * - "回复 " 部分渲染为灰色
 * - "XXX：" 部分渲染为深色
 *
 * 前缀作为真实字符串的一部分存在，[OffsetMapping] 使用 Identity 映射，
 * 光标位置由 [PostCommentTextField] 中的选区拦截逻辑保证不进入前缀区域。
 *
 * @param prefix 完整前缀字符串，格式为"回复 XXX："
 */
private class ReplyPrefixVisualTransformation(
    private val prefix: String,
) : VisualTransformation {

    // "回复 " 固定为 3 个字符（含空格），其余为用户名和"："
    private val replyWordLength = "comment_panel_reply_prefix".i18n().length

    override fun filter(text: AnnotatedString): TransformedText {
        val transformed = buildAnnotatedString {
            if (text.text.startsWith(prefix)) {
                // "回复 " 灰色
                withStyle(SpanStyle(color = Color(0xFF999999))) {
                    append(text.text.substring(0, replyWordLength))
                }
                // "XXX：" 深色
                withStyle(SpanStyle(color = Color(0xFF333333))) {
                    append(text.text.substring(replyWordLength, prefix.length))
                }
                // 用户实际输入的文字，保持原始 AnnotatedString spans
                append(text.subSequence(prefix.length, text.length))
            } else {
                append(text)
            }
        }
        // 真实字符串与显示字符串长度一致，使用 Identity 映射
        return TransformedText(transformed, OffsetMapping.Identity)
    }
}

@Stable
class CommentPanelState internal constructor(
    val scrollState: ScrollState,
    private val scope: CoroutineScope,
) {
    var hasContent by mutableStateOf(false)
    var showDiscardConfirm by mutableStateOf(false)
        private set
    var replyTarget by mutableStateOf<ReplyTarget?>(null)

    internal var comments: List<BridgeMarkCommentInfo> = emptyList()
    private val cellTops = mutableMapOf<Long, Int>()
    private var scrollJob: Job? = null
    internal var isProgrammaticScroll = false

    fun tryDismiss(onDismiss: () -> Unit) {
        if (hasContent) showDiscardConfirm = true else performDismiss(onDismiss)
    }

    fun confirmDiscard(onDismiss: () -> Unit) {
        showDiscardConfirm = false
        performDismiss(onDismiss)
    }

    fun cancelDiscard() {
        showDiscardConfirm = false
    }

    fun recordCellPosition(markId: Long, topY: Int) {
        cellTops[markId] = topY
    }

    fun onPostSubmitted(targetMarkId: Long?) {
        replyTarget = null
        scrollJob?.cancel()
        scrollJob = scope.launch {
            delay(200)
            isProgrammaticScroll = true
            try {
                val pos = when (targetMarkId) {
                    null -> scrollState.maxValue
                    else -> resolveScrollPosition(targetMarkId) ?: scrollState.maxValue
                }
                scrollState.animateScrollTo(pos)
            } finally {
                isProgrammaticScroll = false
            }
        }
    }

    private fun resolveScrollPosition(targetMarkId: Long): Int? {
        cellTops[targetMarkId]?.let { return it }
        for (comment in comments) {
            if (comment.children.any { it.markId == targetMarkId }) {
                return cellTops[comment.markId]
            }
        }
        return null
    }

    private fun performDismiss(onDismiss: () -> Unit) {
        scrollJob?.cancel()
        replyTarget = null
        hasContent = false
        showDiscardConfirm = false
        cellTops.clear()
        onDismiss()
    }
}

@Composable
private fun rememberCommentPanelState(): CommentPanelState {
    val scrollState = rememberScrollState()
    val scope = rememberCoroutineScope()
    return remember { CommentPanelState(scrollState, scope) }
}
