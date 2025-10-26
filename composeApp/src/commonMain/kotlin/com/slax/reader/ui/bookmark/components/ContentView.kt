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

    // 计算 CSS padding（仅 Android 用）和 contentInset（仅 iOS 用）
    val density = LocalDensity.current.density

    // 获取 statusBarsPadding 的高度
    val windowInsets = WindowInsets.statusBars
    val statusBarHeightPx = windowInsets.getTop(LocalDensity.current).toFloat()

    println("[ContentView] platformType=$platformType, topContentInsetPx=$topContentInsetPx, statusBarHeightPx=$statusBarHeightPx")

    // Android: CSS padding = Column 高度 + statusBarsPadding
    // iOS: 不使用 CSS padding，用原生 contentInset（已包含所有高度）
    val cssTopPaddingPx by remember(topContentInsetPx, statusBarHeightPx) {
        derivedStateOf {
            if (platformType == "android" && topContentInsetPx > 0f) {
                // topContentInsetPx 已经包含了 Column 的 padding(bottom = 16.dp)
                // 所以这里只需要加 statusBarsPadding，不需要再加 16f
                val result = (topContentInsetPx + statusBarHeightPx) / density
                println("[Android CSS] topContentInsetPx=$topContentInsetPx, statusBarHeightPx=$statusBarHeightPx, density=$density, cssTopPaddingPx=$result")
                result
            } else {
                println("[iOS] Skip CSS padding, use contentInset instead")
                0f  // iOS 不使用 CSS padding
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
            // Android 使用 CSS padding，iOS 使用原生 contentInset
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
