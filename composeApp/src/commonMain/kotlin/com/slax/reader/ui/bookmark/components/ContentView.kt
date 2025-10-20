package com.slax.reader.ui.bookmark.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.multiplatform.webview.web.LoadingState
import com.multiplatform.webview.web.WebView
import com.multiplatform.webview.web.rememberWebViewNavigator
import com.multiplatform.webview.web.rememberWebViewStateWithHTMLData
import com.slax.reader.ui.bookmark.BookmarkDetailViewModel
import com.slax.reader.utils.configureIOSWebView
import com.slax.reader.utils.webViewStateSetting
import org.koin.compose.koinInject


@Composable
fun BookmarkContentView(bookmarkId: String) {
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
                CircularProgressIndicator()
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
            )
        }
    }
}

@Composable
fun AdaptiveWebView(htmlContent: String) {
    var webViewHeight by remember { mutableStateOf(500.dp) }

    val webViewState = key(htmlContent) {
        rememberWebViewStateWithHTMLData(htmlContent).also {
            webViewStateSetting(it)
        }
    }

    val navigator = rememberWebViewNavigator()

    val isLoadingFinished by remember {
        derivedStateOf {
            println("web state change: ${webViewState.loadingState}")
            webViewState.loadingState is LoadingState.Finished
        }
    }

    val webViewModifier by remember {
        derivedStateOf {
            println("height changed: $webViewHeight")
            Modifier.fillMaxWidth().padding(top = 200.dp)
                .fillMaxWidth()
                .windowInsetsPadding(WindowInsets(0, 0, 0, 0))
                .height(webViewHeight)
        }
    }

    WebView(
        state = webViewState,
        modifier = webViewModifier,
        captureBackPresses = false,
        navigator = navigator,
        onCreated = { webView ->
            configureIOSWebView(webView)
        },
    )

    LaunchedEffect(isLoadingFinished) {
        if (isLoadingFinished) {
            val script = """
                (function() {
                    return Math.max(
                        document.body.scrollHeight,
                        document.body.offsetHeight,
                        document.documentElement.clientHeight,
                        document.documentElement.scrollHeight,
                        document.documentElement.offsetHeight
                    );
                })();
            """.trimIndent()

            navigator.evaluateJavaScript(script) { result ->
                result.let {
                    try {
                        val height = it.toDoubleOrNull() ?: 500.0
                        webViewHeight = height.dp + 10.dp
                        println("WebView 高度已更新: $webViewHeight")
                    } catch (e: Exception) {
                        println("获取高度失败: ${e.message}")
                    }
                }
            }
        }
    }
}