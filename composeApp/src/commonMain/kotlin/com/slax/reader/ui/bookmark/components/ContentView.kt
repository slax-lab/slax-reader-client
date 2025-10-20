package com.slax.reader.ui.bookmark.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.multiplatform.webview.jsbridge.IJsMessageHandler
import com.multiplatform.webview.jsbridge.JsMessage
import com.multiplatform.webview.jsbridge.rememberWebViewJsBridge
import com.multiplatform.webview.web.LoadingState
import com.multiplatform.webview.web.WebView
import com.multiplatform.webview.web.WebViewNavigator
import com.multiplatform.webview.web.rememberWebViewNavigator
import com.multiplatform.webview.web.rememberWebViewStateWithHTMLData
import com.slax.reader.ui.bookmark.BookmarkDetailViewModel
import com.slax.reader.utils.configureIOSWebView
import com.slax.reader.utils.webViewStateSetting
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import org.koin.compose.koinInject

@Serializable
data class HeightChangeMessage(val height: Double)

// JSBridge 消息处理器,用于接收来自 WebView 的高度变化消息
class HeightChangeJsMessageHandler(
    private val onHeightChange: (Double) -> Unit
) : IJsMessageHandler {
    override fun methodName(): String = "onHeightChange"

    override fun handle(
        message: JsMessage,
        navigator: WebViewNavigator?,
        callback: (String) -> Unit
    ) {
        try {
            val json = Json { ignoreUnknownKeys = true }
            val heightMessage = json.decodeFromString<HeightChangeMessage>(message.params)
            onHeightChange(heightMessage.height)
            callback("{\"status\":\"success\"}")
        } catch (e: Exception) {
            println("处理高度变化消息失败: ${e.message}")
            callback("{\"status\":\"error\",\"message\":\"${e.message}\"}")
        }
    }
}


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

    // 创建 JsBridge
    val jsBridge = rememberWebViewJsBridge()

    // 注册高度变化消息处理器
    LaunchedEffect(jsBridge) {
        jsBridge.register(HeightChangeJsMessageHandler { height ->
            webViewHeight = height.dp
            println("通过 JSBridge 接收到高度变化: $height")
        })
    }

    val isLoadingFinished by remember {
        derivedStateOf {
            println("web state change: ${webViewState.loadingState}")
            webViewState.loadingState is LoadingState.Finished
        }
    }

    val webViewModifier by remember {
        derivedStateOf {
            println("height changed: $webViewHeight")
            Modifier.fillMaxWidth().padding(top = 20.dp)
                .background(Color.Green)
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
        webViewJsBridge = jsBridge,
        onCreated = { webView ->
            configureIOSWebView(webView)
        },
    )

    // 注入使用 JSBridge 的监听脚本
    LaunchedEffect(isLoadingFinished) {
        if (isLoadingFinished) {
            val heightMonitorScript = """
                (function() {
                    // 获取当前高度
                    function getContentHeight() {
                        return Math.max(
                            document.body.scrollHeight,
                            document.body.offsetHeight,
                            document.documentElement.clientHeight,
                            document.documentElement.scrollHeight,
                            document.documentElement.offsetHeight
                        );
                    }

                    // 通过 JSBridge 通知原生层高度变化
                    function notifyHeightChange(height) {
                        if (window.kmpJsBridge) {
                            window.kmpJsBridge.callNative(
                                'onHeightChange',
                                JSON.stringify({ height: height }),
                                function(response) {
                                    console.log('Height change notification response:', response);
                                }
                            );
                        } else {
                            console.error('kmpJsBridge not available');
                        }
                    }

                    // 初始高度
                    let lastHeight = getContentHeight();
                    notifyHeightChange(lastHeight);

                    // 使用 MutationObserver 监听 DOM 变化
                    const observer = new MutationObserver(function(mutations) {
                        const currentHeight = getContentHeight();
                        if (currentHeight !== lastHeight) {
                            lastHeight = currentHeight;
                            console.log('Height changed to:', currentHeight);
                            notifyHeightChange(currentHeight);
                        }
                    });

                    // 配置观察选项
                    const config = {
                        childList: true,
                        subtree: true,
                        attributes: true,
                        characterData: true
                    };

                    // 开始观察
                    observer.observe(document.body, config);

                    // 同时监听窗口 resize 事件
                    window.addEventListener('resize', function() {
                        const currentHeight = getContentHeight();
                        if (currentHeight !== lastHeight) {
                            lastHeight = currentHeight;
                            console.log('Height changed on resize:', currentHeight);
                            notifyHeightChange(currentHeight);
                        }
                    });

                    // 监听图片加载完成
                    const images = document.getElementsByTagName('img');
                    for (let i = 0; i < images.length; i++) {
                        images[i].addEventListener('load', function() {
                            const currentHeight = getContentHeight();
                            if (currentHeight !== lastHeight) {
                                lastHeight = currentHeight;
                                console.log('Height changed on image load:', currentHeight);
                                notifyHeightChange(currentHeight);
                            }
                        });
                    }

                    console.log('JSBridge height monitor initialized with height:', lastHeight);
                })();
            """.trimIndent()

            navigator.evaluateJavaScript(heightMonitorScript) { result ->
                println("JSBridge 高度监听脚本注入结果: $result")
            }
        }
    }
}