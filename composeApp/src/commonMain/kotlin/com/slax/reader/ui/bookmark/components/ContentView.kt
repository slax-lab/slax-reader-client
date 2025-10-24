package com.slax.reader.ui.bookmark.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import com.slax.reader.ui.bookmark.BookmarkDetailViewModel
import com.slax.reader.utils.AppWebView
import com.slax.reader.utils.wrapHtmlWithCSS
import org.koin.compose.koinInject

@Composable
fun BookmarkContentView(
    bookmarkId: String,
    topContentHeightPx: Float,
    onWebViewTap: (() -> Unit)? = null,
    onScrollChange: ((scrollY: Float) -> Unit)? = null,
) {
    val detailView: BookmarkDetailViewModel = koinInject()

    var rawHtmlContent by remember { mutableStateOf<String?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }

    // 等待顶部测量完成后再包装 HTML
    // topContentHeightPx 基于 Compose 的 px（Android 为物理像素，iOS 为 points * scale）
    // WebView 中的 CSS px 对应设备无关像素，因此需要按照 density 折算
    val density = LocalDensity.current
    val cssTopPaddingPx by remember(topContentHeightPx, density) {
        derivedStateOf {
            if (topContentHeightPx > 0f) {
                topContentHeightPx / density.density
            } else {
                0f
            }
        }
    }

    val htmlContentWithPadding by remember(rawHtmlContent, cssTopPaddingPx) {
        derivedStateOf {
            if (rawHtmlContent != null && cssTopPaddingPx > 0f) {
                println("[WebView] Using CSS padding-top: ${cssTopPaddingPx}px (css)")
                wrapHtmlWithCSS(rawHtmlContent!!, cssTopPaddingPx)
            } else {
                null
            }
        }
    }

    LaunchedEffect(bookmarkId) {
        isLoading = true
        error = null
        try {
            rawHtmlContent = detailView.getBookmarkContent(bookmarkId)
        } catch (e: Exception) {
            error = e.message ?: "加载失败"
        } finally {
            isLoading = false
        }
    }

    when {
        isLoading -> {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = Color(0xFF16B998))
            }
        }

        error != null -> {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(text = "加载失败: $error", color = Color.Red)
            }
        }

        htmlContentWithPadding != null -> {
            // WebView 通过 CSS padding 预留顶部空间
            AppWebView(
                htmlContent = htmlContentWithPadding,
                modifier = Modifier.fillMaxSize(),
                onTap = onWebViewTap,
                onScrollChange = onScrollChange
            )
        }
    }
}
