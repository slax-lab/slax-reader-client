package com.slax.reader.ui.bookmark.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.slax.reader.ui.bookmark.BookmarkDetailViewModel
import com.slax.reader.ui.bookmark.OverviewViewBounds
import com.slax.reader.utils.i18n
import org.jetbrains.compose.resources.painterResource
import slax_reader_client.composeapp.generated.resources.Res
import slax_reader_client.composeapp.generated.resources.ic_xs_blue_down_arrow

@Composable
fun OverviewView(
    detailView: BookmarkDetailViewModel,
    modifier: Modifier = Modifier,
    onExpand: () -> Unit = {},
    onBoundsChanged: (OverviewViewBounds) -> Unit = {}
) {
    println("[watch][UI] recomposition OverviewView")

    LaunchedEffect(detailView._bookmarkId) {
        detailView.loadOverview()
    }

    val overviewState by detailView.overviewState.collectAsState()
    val content = overviewState.overview
    if (content.isEmpty()) return

    val annotatedText = remember(content) {
        buildAnnotatedString {
            withStyle(style = SpanStyle(color = Color(0xFF999999))) {
                append("overview_prefix".i18n())
            }
            append(content)
        }
    }

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .onGloballyPositioned { coordinates ->
                val position = coordinates.positionInRoot()
                onBoundsChanged(
                    OverviewViewBounds(
                        x = position.x,
                        y = position.y,
                        width = coordinates.size.width.toFloat(),
                        height = coordinates.size.height.toFloat()
                    )
                )
            },
        shape = RoundedCornerShape(8.dp),
        color = Color(0xFFF5F5F3)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onExpand() }
                .padding(12.dp)
        ) {
            // 使用自定义Layout实现文本末尾附加箭头图标
            TextWithTrailingIcon(
                text = annotatedText,
                icon = {
                    Icon(
                        painter = painterResource(Res.drawable.ic_xs_blue_down_arrow),
                        contentDescription = null,
                        tint = Color.Unspecified,
                        modifier = Modifier.size(8.dp)
                    )
                }
            )
        }
    }
}

@Composable
private fun TextWithTrailingIcon(
    text: AnnotatedString,
    icon: @Composable () -> Unit,
    modifier: Modifier = Modifier
) {
    val textMeasurer = rememberTextMeasurer()
    val density = LocalDensity.current

    val iconSizeDp = 8.dp
    val iconSpacingDp = 4.dp

    var containerWidth by remember { mutableStateOf(0) }

    val displayText = remember(text, containerWidth) {
        if (containerWidth == 0) return@remember text

        val textStyle = TextStyle(
            fontSize = 14.sp,
            lineHeight = 20.sp,
            color = Color(0xFF333333)
        )

        // 使用全宽测量文本
        val fullWidthMeasure = textMeasurer.measure(
            text = text,
            style = textStyle,
            constraints = Constraints(maxWidth = containerWidth),
            maxLines = 3,
            overflow = TextOverflow.Ellipsis
        )

        // 检查是否需要省略
        val needsEllipsis = fullWidthMeasure.didOverflowHeight || fullWidthMeasure.hasVisualOverflow

        if (!needsEllipsis) {
            text
        } else {
            val maxLines = 3
            val actualLineCount = minOf(fullWidthMeasure.lineCount, maxLines)
            val lastLineIndex = actualLineCount - 1

            // 计算最后一行需要预留的空间
            with(density) {
                val iconWidthPx = iconSizeDp.roundToPx() + iconSpacingDp.roundToPx()
                val lastLineMaxWidth = containerWidth - iconWidthPx

                // 获取到最后一行开始的偏移量
                val lastLineStartOffset = if (lastLineIndex > 0) {
                    fullWidthMeasure.getLineEnd(lastLineIndex - 1, visibleEnd = true)
                } else {
                    0
                }

                // 计算最后一行在受限宽度下能显示多少字符
                val remainingText = text.subSequence(lastLineStartOffset, text.length)

                // 二分查找合适的截断位置
                var low = 0
                var high = remainingText.length
                var bestEnd = 0

                while (low <= high) {
                    val mid = (low + high) / 2
                    val testText = buildAnnotatedString {
                        append(remainingText.subSequence(0, mid))
                        append("...")
                    }

                    val testWidth = textMeasurer.measure(
                        text = testText,
                        style = textStyle
                    ).size.width

                    if (testWidth <= lastLineMaxWidth) {
                        bestEnd = mid
                        low = mid + 1
                    } else {
                        high = mid - 1
                    }
                }

                buildAnnotatedString {
                    append(text.subSequence(0, lastLineStartOffset))
                    append(remainingText.subSequence(0, bestEnd))
                    if (bestEnd < remainingText.length) {
                        append("...")
                    }
                }
            }
        }
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .onGloballyPositioned { coordinates ->
                containerWidth = coordinates.size.width
            }
    ) {
        var textLayoutResult by remember { mutableStateOf<TextLayoutResult?>(null) }

        Text(
            text = displayText,
            modifier = Modifier.fillMaxWidth(),
            style = TextStyle(
                fontSize = 14.sp,
                lineHeight = 20.sp,
                color = Color(0xFF333333)
            ),
            maxLines = 3,
            overflow = TextOverflow.Ellipsis,
            onTextLayout = { textLayoutResult = it }
        )

        textLayoutResult?.let { layout ->
            if (layout.lineCount > 0) {
                val lastLine = minOf(layout.lineCount - 1, 2)
                val lineTop = layout.getLineTop(lastLine)
                val lineBottom = layout.getLineBottom(lastLine)
                val lineRight = layout.getLineRight(lastLine)

                val lineHeight = lineBottom - lineTop

                Box(
                    modifier = Modifier
                        .offset {
                            IntOffset(
                                x = (lineRight + with(density) { iconSpacingDp.toPx() }).toInt(),
                                y = (lineTop + lineHeight / 2 - with(density) { iconSizeDp.toPx() } / 2).toInt()
                            )
                        }
                ) {
                    icon()
                }
            }
        }
    }
}
