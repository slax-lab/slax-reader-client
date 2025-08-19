package com.slax.reader

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.navigation.NavController
import com.fleeksoft.ksoup.Ksoup
import com.multiplatform.webview.setting.PlatformWebSettings
import com.multiplatform.webview.web.WebView
import com.multiplatform.webview.web.rememberWebViewStateWithHTMLData

@Composable
fun HybridReaderView(nav: NavController) {
    val contentElements = remember(htmlData) {
        val doc = Ksoup.parse(htmlData)
        val elements = mutableListOf<ContentElement>()

        val allElements = doc.select("p, h1, h2, h3, h4, h5, h6, table, pre, blockquote, ul, ol")
        val standaloneImages = doc.select("img").filter { img ->
            img.parents().none { parent -> parent.tagName() in setOf("p", "div", "figure") }
        }

        allElements.forEach { element ->
            when (element.tagName()) {
                "h1", "h2", "h3", "h4", "h5", "h6" -> {
                    val text = element.text().trim()
                    if (text.isNotEmpty()) {
                        elements.add(ContentElement.Header(text, element.tagName()))
                    }
                }

                "p" -> {
                    val images = element.select("img")
                    val text = element.text().trim()

                    if (images.isNotEmpty()) {
                        if (text.isNotEmpty()) {
                            elements.add(ContentElement.Paragraph(text))
                        }
                        images.forEach { img ->
                            val src = getImageSrc(img)
                            if (src.isNotEmpty()) {
                                elements.add(ContentElement.Image(src, img.attr("alt")))
                            }
                        }
                    } else if (text.isNotEmpty()) {
                        elements.add(ContentElement.Paragraph(text))
                    }
                }

                "img" -> {}

                "table", "pre", "blockquote", "ul", "ol" -> {
                    elements.add(ContentElement.WebViewContent(element.outerHtml()))
                }
            }
        }

        standaloneImages.forEach { img ->
            val src = getImageSrc(img)
            if (src.isNotEmpty()) {
                elements.add(ContentElement.Image(src, img.attr("alt")))
            }
        }

        elements
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(contentElements.size) { index ->
            val element = contentElements[index]
            when (element) {
                is ContentElement.Header -> {
                    NativeHeader(element)
                }

                is ContentElement.Paragraph -> {
                    NativeParagraph(element)
                }

                is ContentElement.Image -> {
                    CompactImageWebView(element)
                }

                is ContentElement.WebViewContent -> {
                    CompactContentWebView(element)
                }
            }
        }
    }
}

private fun getImageSrc(imgElement: com.fleeksoft.ksoup.nodes.Element): String {
    return when {
        imgElement.attr("src").isNotEmpty() -> imgElement.attr("src")
        imgElement.attr("data-src").isNotEmpty() -> imgElement.attr("data-src")
        imgElement.attr("data-backsrc").isNotEmpty() -> imgElement.attr("data-backsrc")
        else -> ""
    }
}

sealed class ContentElement {
    data class Header(val text: String, val tag: String) : ContentElement()
    data class Paragraph(val text: String) : ContentElement()
    data class Image(val src: String, val alt: String) : ContentElement()
    data class WebViewContent(val html: String) : ContentElement()
}

@Composable
private fun NativeHeader(element: ContentElement.Header) {
    val textStyle = when (element.tag) {
        "h1" -> MaterialTheme.typography.headlineLarge
        "h2" -> MaterialTheme.typography.headlineMedium
        "h3" -> MaterialTheme.typography.headlineSmall
        "h4" -> MaterialTheme.typography.titleLarge
        "h5" -> MaterialTheme.typography.titleMedium
        else -> MaterialTheme.typography.titleSmall
    }

    Text(
        text = element.text,
        style = textStyle.copy(fontWeight = FontWeight.Bold),
        color = MaterialTheme.colorScheme.onBackground,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
    )
}

@Composable
private fun NativeParagraph(element: ContentElement.Paragraph) {
    Text(
        text = element.text,
        style = MaterialTheme.typography.bodyLarge.copy(
            lineHeight = 1.6.em,
            textAlign = TextAlign.Justify
        ),
        color = MaterialTheme.colorScheme.onBackground,
        modifier = Modifier.fillMaxWidth()
    )
}

@Composable
private fun CompactImageWebView(element: ContentElement.Image) {
    val imageHtml = """
        <!DOCTYPE html>
        <html>
        <head>
            <meta charset="UTF-8">
            <meta name="viewport" content="width=device-width, initial-scale=1.0">
            <style>
                * { margin: 0; padding: 0; box-sizing: border-box; }
                html, body { 
                    height: 100%;
                    overflow: hidden;
                    background: transparent;
                }
                body { 
                    display: flex; 
                    justify-content: center;
                    align-items: center;
                    padding: 8px;
                }
                img { 
                    max-width: 100%; 
                    max-height: 400px;
                    height: auto; 
                    border-radius: 8px;
                    box-shadow: 0 2px 8px rgba(0,0,0,0.1);
                    object-fit: contain;
                }
                .loading {
                    width: 100%;
                    height: 200px;
                    background: #f5f5f5;
                    display: flex;
                    align-items: center;
                    justify-content: center;
                    border-radius: 8px;
                    color: #666;
                    font-size: 14px;
                }
                .error {
                    width: 100%;
                    height: 120px;
                    background: #f5f5f5;
                    display: flex;
                    align-items: center;
                    justify-content: center;
                    border-radius: 8px;
                    color: #666;
                    border: 2px dashed #ddd;
                    font-size: 14px;
                    flex-direction: column;
                }
                @media (prefers-color-scheme: dark) {
                    .loading, .error { background: #2a2a2a; color: #ccc; border-color: #555; }
                    img { box-shadow: 0 2px 8px rgba(255,255,255,0.1); }
                }
            </style>
        </head>
        <body>
            <div class="loading" id="loading">加载中...</div>
            <img src="${element.src}" alt="${element.alt}" 
                 style="display:none;"
                 onload="this.style.display='block'; document.getElementById('loading').style.display='none';"
                 onerror="document.getElementById('loading').style.display='none'; document.querySelector('.error').style.display='flex';" />
            <div class="error" style="display:none;">
                <div>📷 图片加载失败</div>
                <div style="font-size:12px; margin-top:4px; color:#999;">${element.src.take(50)}...</div>
            </div>
        </body>
        </html>
    """.trimIndent()

    val webState = rememberWebViewStateWithHTMLData(imageHtml)

    LaunchedEffect(Unit) {
        webState.webSettings.apply {
            isJavaScriptEnabled = true
            supportZoom = false
            allowFileAccessFromFileURLs = false
            allowUniversalAccessFromFileURLs = false

            androidWebSettings.apply {
                domStorageEnabled = false
                layerType = PlatformWebSettings.AndroidWebSettings.LayerType.HARDWARE
            }
        }
    }

    WebView(
        state = webState,
        modifier = Modifier
            .fillMaxWidth()
            .height(250.dp)
            .clip(RoundedCornerShape(8.dp)),
        captureBackPresses = false
    )
}

@Composable
private fun CompactContentWebView(element: ContentElement.WebViewContent) {
    val contentHtml = """
        <!DOCTYPE html>
        <html lang="zh-CN">
        <head>
            <meta charset="UTF-8">
            <meta name="viewport" content="width=device-width, initial-scale=1.0">
            <style>
                * { box-sizing: border-box; margin: 0; padding: 0; }
                html, body {
                    overflow: hidden;
                    height: 100%;
                    background: transparent;
                }
                body {
                    font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif;
                    font-size: 16px;
                    line-height: 1.6;
                    color: #333;
                    padding: 12px;
                }
                
                table {
                    width: 100%;
                    border-collapse: collapse;
                    border: 1px solid #e1e4e8;
                    border-radius: 6px;
                    overflow: hidden;
                    font-size: 14px;
                }
                th, td {
                    padding: 8px 10px;
                    text-align: left;
                    border-bottom: 1px solid #e1e4e8;
                }
                th { background: #f6f8fa; font-weight: 600; }
                
                pre {
                    background: #f6f8fa;
                    border: 1px solid #e1e4e8;
                    border-radius: 6px;
                    padding: 12px;
                    font-family: 'SF Mono', Monaco, monospace;
                    font-size: 13px;
                    overflow-x: auto;
                }
                
                blockquote {
                    padding: 12px 16px;
                    border-left: 4px solid #0066cc;
                    background: #f8f9fa;
                    border-radius: 0 6px 6px 0;
                    font-style: italic;
                }
                
                ul, ol { 
                    padding-left: 20px; 
                }
                li { margin: 4px 0; }
                
                @media (prefers-color-scheme: dark) {
                    body { color: #e1e1e1; }
                    table, th, td { border-color: #404040; }
                    th { background: #2d2d2d; }
                    pre { background: #2d2d2d; color: #e1e1e1; border-color: #404040; }
                    blockquote { background: #2d2d2d; border-left-color: #8ab4f8; }
                }
            </style>
        </head>
        <body>${element.html}</body>
        </html>
    """.trimIndent()

    val webState = rememberWebViewStateWithHTMLData(contentHtml)

    LaunchedEffect(Unit) {
        webState.webSettings.apply {
            isJavaScriptEnabled = true
            supportZoom = false
            allowFileAccessFromFileURLs = false
            allowUniversalAccessFromFileURLs = false

            androidWebSettings.apply {
                domStorageEnabled = false
                layerType = PlatformWebSettings.AndroidWebSettings.LayerType.HARDWARE
            }
        }
    }

    WebView(
        state = webState,
        modifier = Modifier
            .fillMaxWidth()
            .height(180.dp)
            .clip(RoundedCornerShape(8.dp)),
        captureBackPresses = false
    )
}
