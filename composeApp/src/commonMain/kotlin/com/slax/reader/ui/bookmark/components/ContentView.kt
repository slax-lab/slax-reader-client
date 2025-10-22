package com.slax.reader.ui.bookmark.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.slax.reader.ui.bookmark.BookmarkDetailViewModel
import com.slax.reader.utils.AppWebView
import org.koin.compose.koinInject

@Composable
fun BookmarkContentView(
    bookmarkId: String,
    onWebViewTap: (() -> Unit)? = null
) {
    // println("[watch][UI] recomposition BookmarkContentView")

    val detailView: BookmarkDetailViewModel = koinInject()
    var htmlContent by remember { mutableStateOf<String?>(null) }
    var isLoadingContent by remember { mutableStateOf(true) }
    var loadError by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        isLoadingContent = true
        try {
            htmlContent = detailView.getBookmarkContent(bookmarkId)
        } catch (e: Exception) {
            loadError = e.message ?: "加载失败"
            println("加载内容失败: ${e.message}")
        } finally {
            isLoadingContent = false
        }
    }
    when {
        isLoadingContent -> {
            Box(
                modifier = Modifier.fillMaxWidth().padding(top = 20.dp).height(200.dp),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(
                    color = Color(0xFF16B998)
                )
            }
        }

        loadError != null -> {
            Box(
                modifier = Modifier.fillMaxWidth().padding(top = 20.dp).height(200.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "加载失败: $loadError",
                    color = Color.Red
                )
            }
        }

        htmlContent != null -> {
            AdaptiveWebView(
                htmlContent = htmlContent!!,
                onWebViewTap = onWebViewTap
            )
        }
    }
}

@Composable
fun AdaptiveWebView(
    htmlContent: String,
    onWebViewTap: (() -> Unit)? = null
) {
    var webViewHeight by remember { mutableStateOf(500.dp) }

    val webViewModifier by remember {
        derivedStateOf {
            Modifier.fillMaxWidth()
                .padding(top = 20.dp)
                .windowInsetsPadding(WindowInsets(0, 0, 0, 0))
                .height(webViewHeight)
        }
    }

    AppWebView(
        htmlContent = htmlContent,
        modifier = webViewModifier,
        onHeightChange = { h -> webViewHeight = h.dp },
        onTap = onWebViewTap
    )
}
