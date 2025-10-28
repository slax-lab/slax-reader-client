package com.slax.reader.ui.bookmark.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.statusBars
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import com.slax.reader.ui.bookmark.BookmarkDetailViewModel
import com.slax.reader.utils.AppWebView
import com.slax.reader.utils.platformType
import com.slax.reader.utils.wrapHtmlWithCSS
import org.koin.compose.koinInject

@Composable
fun BookmarkContentView(
    bookmarkId: String,
    topContentInsetPx: Float,
    onWebViewTap: (() -> Unit)? = null,
    onScrollChange: ((scrollY: Float) -> Unit)? = null,
) {
    val detailView: BookmarkDetailViewModel = koinInject()

    var htmlContent by remember { mutableStateOf<String?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }

    val density = LocalDensity.current.density

    // 获取 statusBarsPadding 的高度
    val windowInsets = WindowInsets.statusBars
    val statusBarHeightPx = windowInsets.getTop(LocalDensity.current).toFloat()

    val isAndroid = remember { platformType == "android" }

    val cssTopPaddingPx by remember(topContentInsetPx, statusBarHeightPx, density) {
        derivedStateOf {
            if (isAndroid && topContentInsetPx > 0f) {
                (topContentInsetPx + statusBarHeightPx) / density
            } else {
                0f
            }
        }
    }

    val wrappedHtml by remember(htmlContent, cssTopPaddingPx) {
        derivedStateOf {
            htmlContent?.let { wrapHtmlWithCSS(it, cssTopPaddingPx) }
        }
    }

    LaunchedEffect(bookmarkId) {
        isLoading = true
        error = null
        try {
            htmlContent = detailView.getBookmarkContent(bookmarkId)
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

        wrappedHtml != null -> {
            AppWebView(
                htmlContent = wrappedHtml,
                modifier = Modifier.fillMaxSize(),
                topContentInsetPx = topContentInsetPx,
                onTap = onWebViewTap,
                onScrollChange = onScrollChange
            )
        }
    }
}
