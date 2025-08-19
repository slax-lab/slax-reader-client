package com.slax.reader

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.multiplatform.webview.web.WebView
import com.multiplatform.webview.web.rememberWebViewStateWithHTMLData

@Composable
fun RichRender(
    nav: NavController
) {
    val renderBlocks = remember(htmlData) {
        parseHtmlToBlocksOptimized(htmlData)
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(4.dp),
        contentPadding = PaddingValues(12.dp)
    ) {
        itemsIndexed(
            items = renderBlocks,
            key = { index, _ -> "block_$index" }
        ) { index, block ->
            when (block.type) {
                BlockType.TEXT -> {
                    OptimizedTextBlock(
                        content = block.content,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                BlockType.WEBVIEW -> {
                    LazyWebViewBlock(
                        content = block.content,
                        estimatedHeight = block.estimatedHeight,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    }
}

enum class BlockType {
    TEXT,
    WEBVIEW
}

data class RenderBlock(
    val type: BlockType,
    val content: String,
    val estimatedHeight: Int = 120
)

@Composable
private fun OptimizedTextBlock(
    content: String,
    modifier: Modifier = Modifier
) {
    val cleanText = remember(content) {
        content
            .replace(Regex("<[^>]+>"), "")
            .replace("&nbsp;", " ")
            .replace("&lt;", "<")
            .replace("&gt;", ">")
            .replace("&amp;", "&")
            .replace("&quot;", "\"")
            .trim()
    }

    if (cleanText.isNotEmpty() && cleanText.length > 10) {
        SelectionContainer {
            Text(
                text = cleanText,
                style = MaterialTheme.typography.bodyLarge.copy(
                    lineHeight = MaterialTheme.typography.bodyLarge.lineHeight * 1.4,
                    textAlign = TextAlign.Justify
                ),
                modifier = modifier.padding(vertical = 8.dp, horizontal = 4.dp)
            )
        }
    }
}


@Composable
private fun LazyWebViewBlock(
    content: String,
    estimatedHeight: Int,
    modifier: Modifier = Modifier
) {
    val webViewState = rememberWebViewStateWithHTMLData(
        data = wrapHtmlForWebViewOptimized(content)
    )

    Card(
        modifier = modifier.padding(vertical = 2.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        WebView(
            state = webViewState,
            modifier = Modifier
                .fillMaxWidth()
                .height(minOf(estimatedHeight, 250).dp)
                .clip(RoundedCornerShape(4.dp))
        )
    }
}

private fun parseHtmlToBlocksOptimized(html: String): List<RenderBlock> {
    val blocks = mutableListOf<RenderBlock>()
    val cleanHtml = html.trim()

    if (cleanHtml.isEmpty()) {
        return blocks
    }

    val webViewTags = setOf(
        "img", "video", "audio", "iframe", "embed", "object", "canvas",
        "svg", "form", "input", "button", "select", "textarea",
        "script", "style", "table", "tr", "td", "th", "thead", "tbody",
        "tfoot", "col", "colgroup", "caption", "details", "summary",
        "dialog", "menu", "menuitem", "progress", "meter"
    )

    val contentChunks = mutableListOf<ContentChunk>()
    val paragraphs = cleanHtml.split(Regex("(?=<(?:h[1-6]|p|div))")).filter { it.trim().isNotEmpty() }

    for (paragraph in paragraphs) {
        if (paragraph.trim().length < 15) continue

        val hasComplexContent = webViewTags.any { tag ->
            paragraph.contains("<$tag", ignoreCase = true)
        }

        if (hasComplexContent) {
            contentChunks.add(ContentChunk(paragraph.trim(), true))
        } else {
            val textContent = paragraph.trim()
            if (textContent.length > 25) {
                contentChunks.add(ContentChunk(textContent, false))
            }
        }
    }

    var i = 0
    while (i < contentChunks.size) {
        val currentChunk = contentChunks[i]
        val combinedContent = StringBuilder(currentChunk.content)
        var combinedLength = currentChunk.content.length
        var j = i + 1

        while (j < contentChunks.size &&
            contentChunks[j].isComplex == currentChunk.isComplex &&
            combinedLength < 3000
        ) {
            combinedContent.append("\n").append(contentChunks[j].content)
            combinedLength += contentChunks[j].content.length
            j++
        }

        if (currentChunk.isComplex) {
            val height = calculateOptimizedWebViewHeight(combinedContent.toString())
            blocks.add(RenderBlock(BlockType.WEBVIEW, combinedContent.toString(), height))
        } else {
            blocks.add(RenderBlock(BlockType.TEXT, combinedContent.toString()))
        }

        i = j
    }

    return blocks.ifEmpty {
        listOf(RenderBlock(BlockType.TEXT, cleanHtml))
    }
}

data class ContentChunk(val content: String, val isComplex: Boolean)


private fun calculateOptimizedWebViewHeight(content: String): Int {
    var height = 60

    when {
        content.contains("<img", ignoreCase = true) -> {
            val imageCount = content.split("<img").size - 1
            height += imageCount * 120
        }

        content.contains("<table", ignoreCase = true) -> height += 100
        content.contains("<video", ignoreCase = true) -> height += 180
        content.contains("<iframe", ignoreCase = true) -> height += 200
        else -> height += (content.length / 500) * 10
    }

    return height.coerceIn(50, 220)
}

private fun wrapHtmlForWebViewOptimized(content: String): String {
    return """
<!DOCTYPE html>
<html>
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0, user-scalable=no">
    <style>
        * {
            box-sizing: border-box;
            margin: 0;
            padding: 0;
        }
        html, body {
            overflow-x: hidden;
            font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', sans-serif;
            font-size: 14px;
            line-height: 1.4;
            color: #333;
            background: transparent;
        }
        body {
            padding: 8px;
        }
        
        img {
            max-width: 100% !important;
            height: auto !important;
            display: block;
            margin: 6px 0;
            border-radius: 4px;
            object-fit: contain;
            loading: lazy;
            decoding: async;
        }
        
        table {
            width: 100%;
            border-collapse: collapse;
            font-size: 12px;
            margin: 8px 0;
        }
        th, td {
            border: 1px solid #ddd;
            padding: 6px;
            text-align: left;
        }
        th {
            background-color: #f5f5f5;
            font-weight: 600;
        }
        
        video, audio {
            max-width: 100%;
            height: auto;
            margin: 8px 0;
        }
        
        p, div { margin: 4px 0; }
        h1, h2, h3, h4, h5, h6 { 
            margin: 8px 0 4px 0;
            line-height: 1.2;
        }
        
        @media (prefers-color-scheme: dark) {
            body { color: #e1e1e1; }
            table, th, td { border-color: #555; }
            th { background-color: #333; }
        }
    </style>
</head>
<body>$content</body>
</html>
    """.trimIndent()
}