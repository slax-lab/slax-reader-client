package com.slax.reader.ui.bookmark.components

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.layout.*
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.slax.reader.ui.bookmark.BookmarkDetailViewModel
import com.slax.reader.utils.AppWebView
import com.slax.reader.utils.timeUnix
import org.koin.compose.koinInject

@Composable
fun BookmarkContentView(
    bookmarkId: String,
    scrollState: ScrollState,
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
                scrollState = scrollState,
                onWebViewTap = onWebViewTap
            )
        }
    }
}

@Composable
fun AdaptiveWebView(
    htmlContent: String,
    scrollState: ScrollState,
    onWebViewTap: (() -> Unit)? = null
) {
    val density = LocalDensity.current

    var contentHeightPx by remember(htmlContent) { mutableStateOf(0.0) }
    var webViewStartScrollPx by remember(htmlContent) { mutableStateOf(0.0) }

    // 用于调用 native 层更新滚动状态的回调
    var onScrollUpdate: ((Double) -> Unit)? by remember { mutableStateOf(null) }

    // 记录当前 WebView 是否启用了滚动
    var isWebViewScrolling by remember(htmlContent) { mutableStateOf(false) }

    // 监听滚动变化，通过回调通知 native 层
    LaunchedEffect(webViewStartScrollPx) {
        // 只有在 webViewStartScrollPx 初始化完成后才开始监听
        if (webViewStartScrollPx > 0.0 && onScrollUpdate != null) {
            // 立即调用一次，初始化状态
            val initialScroll = scrollState.value.toDouble()
            onScrollUpdate?.invoke(initialScroll)
            isWebViewScrolling = initialScroll >= webViewStartScrollPx
            println("[Scroll Debug] Initial scroll check: scrollState=$initialScroll, startY=$webViewStartScrollPx, isWebViewScrolling=$isWebViewScrolling")

            // 然后持续监听滚动变化，使用防抖减少更新频率
            var lastUpdateTime = 0L
            val debounceMs = 16L // 约 60fps

            snapshotFlow { scrollState.value }
                .collect { scrollValue ->
                    val currentTime = timeUnix()
                    val scrollDouble = scrollValue.toDouble()
                    val shouldEnableWebView = scrollDouble >= webViewStartScrollPx

                    // 只有在状态变化时或经过足够时间后才通知，避免频繁调用
                    if (shouldEnableWebView != isWebViewScrolling ||
                        (currentTime - lastUpdateTime) > debounceMs
                    ) {

                        if (shouldEnableWebView != isWebViewScrolling) {
                            isWebViewScrolling = shouldEnableWebView
                            onScrollUpdate?.invoke(scrollDouble)
                            println("[Scroll Debug] Outer scroll state change: scroll=$scrollDouble, isWebViewScrolling=$isWebViewScrolling")
                        }
                        lastUpdateTime = currentTime
                    }
                }
        }
    }

    // WebView 高度 = 内容实际高度
    val webViewHeightDp = remember(contentHeightPx, density) {
        if (contentHeightPx > 0.0) {
            pxToDp(contentHeightPx, density)
        } else {
            with(density) { 800.dp }
        }
    }

    val webViewModifier = Modifier
        .fillMaxWidth()
        .padding(top = 20.dp)
        .windowInsetsPadding(WindowInsets(0, 0, 0, 0))
        .onGloballyPositioned { coordinates ->
            if (webViewStartScrollPx == 0.0) {
                val posY = coordinates.positionInRoot().y.toDouble()
                webViewStartScrollPx = scrollState.value.toDouble() + posY
                println("[Scroll Debug] WebView start position: $webViewStartScrollPx")
            }
        }
        .height(webViewHeightDp)

    AppWebView(
        htmlContent = htmlContent,
        modifier = webViewModifier,
        onHeightChange = { h ->
            contentHeightPx = h
        },
        onTap = onWebViewTap,
        webViewStartY = webViewStartScrollPx,
        onWebViewPositioned = { callback ->
            onScrollUpdate = callback
        }
    )
}

private fun pxToDp(px: Double, density: Density): Dp {
    if (px <= 0.0) return 0.dp
    return with(density) { px.toFloat().toDp() }
}
