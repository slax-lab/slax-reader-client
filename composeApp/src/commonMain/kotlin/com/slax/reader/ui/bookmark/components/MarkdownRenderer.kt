package com.slax.reader.ui.bookmark.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.ui.layout.Layout
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.intellij.markdown.ast.ASTNode
import org.intellij.markdown.ast.getTextInNode
import org.intellij.markdown.flavours.commonmark.CommonMarkFlavourDescriptor
import org.intellij.markdown.parser.MarkdownParser

/**
 * Markdown 内容渲染组件
 * 使用 JetBrains Markdown 解析器，完全自定义渲染
 * @param content Markdown 文本内容
 * @param onLinkClick 链接点击回调，参数为链接 URL
 */
@Composable
fun MarkdownRenderer(
    content: String,
    onLinkClick: (String) -> Unit = {}
) {
    // 解析 markdown
    val flavour = CommonMarkFlavourDescriptor()
    val parsedTree = MarkdownParser(flavour).buildMarkdownTreeFromString(content)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(top = 4.dp)
    ) {
        RenderMarkdownNode(
            content = content,
            node = parsedTree,
            isRoot = true,
            onLinkClick = onLinkClick
        )
    }
}

/**
 * 递归渲染 Markdown 节点
 */
@Composable
private fun RenderMarkdownNode(
    content: String,
    node: ASTNode,
    isRoot: Boolean = false,
    onLinkClick: (String) -> Unit = {}
) {
    val nodeType = node.type.toString()

    when {
        // 跳过换行符等空白节点
        nodeType == "Markdown:EOL" || nodeType.contains("WHITE_SPACE") -> {
            // 不渲染
        }
        // 标题（支持内联元素如链接）
        nodeType == "Markdown:ATX_1" -> {
            RenderHeading(
                content = content,
                node = node,
                fontSize = 20.sp,
                lineHeight = 28.sp,
                bottomPadding = 8.dp,
                onLinkClick = onLinkClick
            )
        }
        nodeType == "Markdown:ATX_2" -> {
            RenderHeading(
                content = content,
                node = node,
                fontSize = 17.sp,
                lineHeight = 22.sp,
                bottomPadding = 6.dp,
                onLinkClick = onLinkClick
            )
        }
        nodeType == "Markdown:ATX_3" || nodeType == "Markdown:ATX_4" ||
        nodeType == "Markdown:ATX_5" || nodeType == "Markdown:ATX_6" -> {
            RenderHeading(
                content = content,
                node = node,
                fontSize = 12.sp,
                lineHeight = 16.sp,
                bottomPadding = 4.dp,
                onLinkClick = onLinkClick
            )
        }
        // 无序列表
        nodeType == "Markdown:UNORDERED_LIST" -> {
            Column(modifier = Modifier.padding(vertical = 4.dp)) {
                node.children.forEach { child ->
                    if (child.type.toString() == "Markdown:LIST_ITEM") {
                        CustomListItem(content, child, onLinkClick, isOrdered = false)
                    }
                }
            }
        }
        // 有序列表
        nodeType == "Markdown:ORDERED_LIST" -> {
            Column(modifier = Modifier.padding(vertical = 4.dp)) {
                node.children.forEachIndexed { index, child ->
                    if (child.type.toString() == "Markdown:LIST_ITEM") {
                        CustomListItem(content, child, onLinkClick, isOrdered = true, orderNumber = index + 1)
                    }
                }
            }
        }
        // 代码块
        nodeType == "Markdown:CODE_FENCE" || nodeType == "Markdown:CODE_BLOCK" -> {
            CodeBlock(content, node)
        }
        // 引用
        nodeType == "Markdown:BLOCK_QUOTE" -> {
            BlockQuote(content, node, onLinkClick)
        }
        // 水平线
        nodeType == "Markdown:HORIZONTAL_RULE" -> {
            HorizontalDivider(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                thickness = 1.dp,
                color = Color(0xFFE0E0E0)
            )
        }
        // 段落
        nodeType == "Markdown:PARAGRAPH" -> {
            RenderParagraph(content, node, onLinkClick = onLinkClick)
        }
        // 根节点，递归渲染子节点
        isRoot -> {
            node.children.forEach { child ->
                RenderMarkdownNode(content, child, onLinkClick = onLinkClick)
            }
        }
        // 未处理的节点类型，递归渲染子节点
        else -> {
            if (node.children.isNotEmpty()) {
                node.children.forEach { child ->
                    RenderMarkdownNode(content, child, onLinkClick = onLinkClick)
                }
            }
        }
    }
}

/**
 * 渲染自定义列表项
 */
@Composable
private fun CustomListItem(
    content: String,
    node: ASTNode,
    onLinkClick: (String) -> Unit = {},
    isOrdered: Boolean = false,
    orderNumber: Int = 1
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 6.dp),
        verticalAlignment = Alignment.Top
    ) {
        if (isOrdered) {
            // 有序列表：显示数字
            Text(
                text = "$orderNumber.",
                style = TextStyle(
                    fontSize = 15.sp,
                    lineHeight = 20.sp,
                    color = Color(0xFFA8B1CD),
                    fontWeight = FontWeight.Medium
                ),
                modifier = Modifier
                    .padding(top = 0.dp)
                    .width(24.dp)
            )
        } else {
            Box(
                modifier = Modifier
                    .padding(top = 8.5.dp)
                    .width(16.dp),
                contentAlignment = Alignment.Center
            ) {
                Canvas(modifier = Modifier.size(5.dp)) {
                    drawCircle(
                        color = Color(0xFFA8B1CD),
                        radius = size.minDimension / 2
                    )
                }
            }
        }

        // 8dp 间距
        Spacer(modifier = Modifier.width(8.dp))

        // 列表项内容（支持嵌套列表）
        Column(modifier = Modifier.weight(1f)) {
            node.children.forEach { child ->
                val childType = child.type.toString()
                when {
                    // 段落
                    childType == "Markdown:PARAGRAPH" -> {
                        RenderParagraph(content, child, isInList = true, onLinkClick = onLinkClick)
                    }
                    // 嵌套的无序列表
                    childType == "Markdown:UNORDERED_LIST" -> {
                        Column(modifier = Modifier.padding(top = 4.dp)) {
                            child.children.forEach { nestedChild ->
                                if (nestedChild.type.toString() == "Markdown:LIST_ITEM") {
                                    CustomListItem(content, nestedChild, onLinkClick, isOrdered = false)
                                }
                            }
                        }
                    }
                    // 嵌套的有序列表
                    childType == "Markdown:ORDERED_LIST" -> {
                        Column(modifier = Modifier.padding(top = 4.dp)) {
                            child.children.forEachIndexed { index, nestedChild ->
                                if (nestedChild.type.toString() == "Markdown:LIST_ITEM") {
                                    CustomListItem(content, nestedChild, onLinkClick, isOrdered = true, orderNumber = index + 1)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * 渲染标题（支持内联元素如链接）
 */
@Composable
private fun RenderHeading(
    content: String,
    node: ASTNode,
    fontSize: androidx.compose.ui.unit.TextUnit,
    lineHeight: androidx.compose.ui.unit.TextUnit,
    bottomPadding: androidx.compose.ui.unit.Dp,
    onLinkClick: (String) -> Unit = {}
) {
    // 查找 ATX_CONTENT 节点，或者直接使用子节点
    val contentNode = node.children.firstOrNull { it.type.toString() == "Markdown:ATX_CONTENT" }
    val contentNodes = if (contentNode != null) {
        contentNode.children
    } else {
        node.children.filter {
            val type = it.type.toString()
            !type.contains("WHITE_SPACE") && type != "Markdown:EOL"
        }
    }

    // 如果有内容节点，处理内联元素
    if (contentNodes.isNotEmpty()) {
        FlowLayout(
            modifier = Modifier.padding(bottom = bottomPadding)
        ) {
            contentNodes.forEach { child ->
                val childType = child.type.toString()
                when {
                    // 链接节点使用 CustomLinkButton
                    childType == "Markdown:INLINE_LINK" || childType == "Markdown:AUTOLINK" -> {
                        CustomLinkButton(content, child, onLinkClick)
                    }
                    // 跳过空白节点
                    childType == "Markdown:EOL" || childType.contains("WHITE_SPACE") -> {
                        // 不渲染
                    }
                    // 其他节点使用 AnnotatedString
                    else -> {
                        val annotatedText = buildAnnotatedString {
                            renderInlineNode(content, child, onLinkClick)
                        }
                        if (annotatedText.isNotEmpty()) {
                            Text(
                                text = annotatedText,
                                style = TextStyle(
                                    fontSize = fontSize,
                                    lineHeight = lineHeight,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color(0xFF0F1419)
                                )
                            )
                        }
                    }
                }
            }
        }
    } else {
        // 没有内容节点，直接获取文本
        val headingText = node.getTextInNode(content).toString()
            .replace(Regex("^#+\\s*"), "")
            .trim()

        Text(
            text = headingText,
            style = TextStyle(
                fontSize = fontSize,
                lineHeight = lineHeight,
                fontWeight = FontWeight.SemiBold,
                color = Color(0xFF0F1419)
            ),
            modifier = Modifier.padding(bottom = bottomPadding)
        )
    }
}

/**
 * 渲染段落（包含内联元素如加粗、斜体、链接、行内代码）
 * 使用自定义流式布局混合文本和链接按钮
 */
@Composable
private fun RenderParagraph(
    content: String,
    node: ASTNode,
    isInList: Boolean = false,
    onLinkClick: (String) -> Unit = {}
) {
    val children = node.children

    if (children.isEmpty()) {
        Text(
            text = node.getTextInNode(content).toString().trim(),
            style = TextStyle(
                fontSize = 15.sp,
                lineHeight = 20.sp,
                color = Color(0xFF333333)
            ),
            modifier = if (!isInList) Modifier.padding(bottom = 8.dp) else Modifier
        )
        return
    }

    // 使用自定义流式布局混合文本和链接按钮
    FlowLayout(
        modifier = if (!isInList) Modifier.padding(bottom = 8.dp) else Modifier
    ) {
        children.forEach { child ->
            val childType = child.type.toString()
            when {
                // 链接节点使用 CustomLinkButton
                childType == "Markdown:INLINE_LINK" || childType == "Markdown:AUTOLINK" -> {
                    CustomLinkButton(content, child, onLinkClick)
                }
                // 其他节点使用 AnnotatedString
                else -> {
                    val annotatedText = buildAnnotatedString {
                        renderInlineNode(content, child, onLinkClick)
                    }
                    if (annotatedText.isNotEmpty()) {
                        Text(
                            text = annotatedText,
                            style = TextStyle(
                                fontSize = 15.sp,
                                lineHeight = 20.sp,
                                color = Color(0xFF333333)
                            )
                        )
                    }
                }
            }
        }
    }
}

/**
 * 自定义流式布局，支持文本和链接按钮混合自动换行
 * 同一行的元素垂直居中对齐
 */
@Composable
private fun FlowLayout(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    Layout(
        content = content,
        modifier = modifier
    ) { measurables, constraints ->
        val placeables = measurables.map { it.measure(constraints) }

        // 第一步：计算每个元素所在的行
        var currentX = 0
        var currentRowIndex = 0
        val elementRows = mutableListOf<Int>() // 记录每个元素在哪一行
        val rowHeights = mutableListOf<Int>() // 记录每一行的最大高度

        placeables.forEach { placeable ->
            // 如果当前行放不下，换行
            if (currentX + placeable.width > constraints.maxWidth && currentX > 0) {
                currentX = 0
                currentRowIndex++
            }

            elementRows.add(currentRowIndex)
            currentX += placeable.width

            // 更新当前行的最大高度
            if (currentRowIndex >= rowHeights.size) {
                rowHeights.add(placeable.height)
            } else {
                rowHeights[currentRowIndex] = maxOf(rowHeights[currentRowIndex], placeable.height)
            }
        }

        // 第二步：计算每个元素的位置（垂直居中对齐）
        currentX = 0
        currentRowIndex = 0
        var currentY = 0
        val positions = mutableListOf<Pair<Int, Int>>()

        placeables.forEachIndexed { index, placeable ->
            val rowIndex = elementRows[index]

            // 换行
            if (rowIndex != currentRowIndex) {
                currentX = 0
                currentY += rowHeights[currentRowIndex]
                currentRowIndex = rowIndex
            }

            // 计算垂直居中位置
            val rowHeight = rowHeights[rowIndex]
            val verticalOffset = (rowHeight - placeable.height) / 2

            positions.add(currentX to currentY + verticalOffset)
            currentX += placeable.width
        }

        val totalHeight = rowHeights.sum()

        layout(constraints.maxWidth, totalHeight) {
            placeables.forEachIndexed { index, placeable ->
                val (x, y) = positions[index]
                placeable.placeRelative(x, y)
            }
        }
    }
}

/**
 * 渲染内联节点（递归处理加粗、斜体、行内代码等）
 * 注意：链接节点在 RenderParagraph 中单独处理，不在此处理
 */
private fun androidx.compose.ui.text.AnnotatedString.Builder.renderInlineNode(
    content: String,
    node: ASTNode,
    onLinkClick: (String) -> Unit
) {
    val nodeType = node.type.toString()

    when {
        // 加粗
        nodeType == "Markdown:STRONG" -> {
            withStyle(SpanStyle(fontWeight = FontWeight.Bold)) {
                node.children.forEach { child ->
                    // 跳过标记符号（** 或 __）
                    if (child.type.toString() != "Markdown:EMPH_DELIMITER") {
                        renderInlineNode(content, child, onLinkClick)
                    }
                }
            }
        }
        // 斜体
        nodeType == "Markdown:EMPH" -> {
            withStyle(SpanStyle(fontStyle = FontStyle.Italic)) {
                node.children.forEach { child ->
                    // 跳过标记符号（* 或 _）
                    if (child.type.toString() != "Markdown:EMPH_DELIMITER") {
                        renderInlineNode(content, child, onLinkClick)
                    }
                }
            }
        }
        // 行内代码
        nodeType == "Markdown:CODE_SPAN" -> {
            withStyle(
                SpanStyle(
                    background = Color(0xFFF5F5F5),
                    fontFamily = FontFamily.Monospace,
                    fontSize = 14.sp,
                    color = Color(0xFFE01E5A)
                )
            ) {
                // 获取代码文本，去除反引号
                val codeText = node.getTextInNode(content).toString()
                    .removePrefix("`")
                    .removeSuffix("`")
                append(" $codeText ")
            }
        }
        // 链接节点跳过，由 CustomLinkButton 处理
        nodeType == "Markdown:INLINE_LINK" || nodeType == "Markdown:AUTOLINK" -> {
            // 不处理，在 RenderParagraph 中使用 CustomLinkButton
        }
        // 普通文本
        nodeType == "Markdown:TEXT" || nodeType.contains("WHITE_SPACE") -> {
            append(node.getTextInNode(content).toString())
        }
        // 其他节点，递归处理子节点
        else -> {
            node.children.forEach { child ->
                renderInlineNode(content, child, onLinkClick)
            }
        }
    }
}

/**
 * 自定义链接按钮样式
 */
@Composable
private fun CustomLinkButton(
    content: String,
    node: ASTNode,
    onLinkClick: (String) -> Unit = {}
) {
    // 提取链接文本
    val linkText = node.children
        .firstOrNull { it.type.toString() == "Markdown:LINK_TEXT" || it.type.toString() == "Markdown:TEXT" }
        ?.getTextInNode(content)
        ?.toString()
        ?.removeSurrounding("[", "]")
        ?: node.getTextInNode(content).toString()

    // 提取链接 URL
    val linkUrl = node.children
        .firstOrNull { it.type.toString() == "Markdown:LINK_DESTINATION" }
        ?.getTextInNode(content)
        ?.toString()
        ?: ""

    Box(
        modifier = Modifier.padding(top = 2.dp)
    ) {
        Surface(
            shape = RoundedCornerShape(3.5.dp),
            color = Color(0x1F16B998),
            modifier = Modifier
                .height(20.dp)
                .padding(horizontal = 2.dp)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = ripple()
                ) {
                    onLinkClick(linkUrl)
                }
        ) {
            Box(
                modifier = Modifier.padding(horizontal = 6.5.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = linkText.trim(),
                    style = TextStyle(
                        fontSize = 12.sp,
                        color = Color(0xFF4d4d4d)
                    ),
                    maxLines = 1
                )
            }
        }
    }
}

/**
 * 代码块组件
 */
@Composable
private fun CodeBlock(
    content: String,
    node: ASTNode
) {
    // 提取代码内容
    val codeText = node.children
        .firstOrNull { it.type.toString() == "Markdown:CODE_FENCE_CONTENT" }
        ?.getTextInNode(content)
        ?.toString()
        ?.trim()
        ?: node.getTextInNode(content).toString()
            .replace(Regex("^```.*\\n"), "")  // 移除开始标记
            .replace(Regex("```$"), "")       // 移除结束标记
            .trim()

    Surface(
        shape = RoundedCornerShape(6.dp),
        color = Color(0xFFF5F5F5),
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
    ) {
        Box(
            modifier = Modifier
                .horizontalScroll(rememberScrollState())
                .padding(12.dp)
        ) {
            Text(
                text = codeText,
                style = TextStyle(
                    fontFamily = FontFamily.Monospace,
                    fontSize = 13.sp,
                    lineHeight = 18.sp,
                    color = Color(0xFF333333)
                )
            )
        }
    }
}

/**
 * 引用块组件
 */
@Composable
private fun BlockQuote(
    content: String,
    node: ASTNode,
    onLinkClick: (String) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
    ) {
        // 左侧绿色竖条
        Box(
            modifier = Modifier
                .width(4.dp)
                .fillMaxHeight()
                .background(Color(0xFF16B998))
        )

        // 12dp 间距
        Spacer(modifier = Modifier.width(12.dp))

        // 引用内容
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(vertical = 4.dp)
        ) {
            node.children.forEach { child ->
                when (child.type.toString()) {
                    "Markdown:PARAGRAPH" -> {
                        RenderParagraph(content, child, isInList = false, onLinkClick = onLinkClick)
                    }
                    "Markdown:BLOCK_QUOTE" -> {
                        // 嵌套引用
                        BlockQuote(content, child, onLinkClick)
                    }
                    else -> {
                        // 递归渲染其他节点
                        RenderMarkdownNode(content, child, onLinkClick = onLinkClick)
                    }
                }
            }
        }
    }
}