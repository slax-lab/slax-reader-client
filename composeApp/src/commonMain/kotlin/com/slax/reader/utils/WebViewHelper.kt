package com.slax.reader.utils

import com.multiplatform.webview.setting.PlatformWebSettings
import com.multiplatform.webview.util.KLogSeverity
import com.multiplatform.webview.web.WebViewState
import org.jetbrains.compose.resources.ExperimentalResourceApi
import slax_reader_client.composeapp.generated.resources.Res

/**
 * 配置 WebView 的平台特定设置
 * @param webView 平台原生的 WebView 实例
 * @param disableScrolling 是否禁用滚动
 * @param removeContentInsets 是否去除内容安全距离
 */
expect fun configureNativeWebView(
    webView: Any,
    disableScrolling: Boolean = true,
    removeContentInsets: Boolean = true
)


fun webViewStateSetting(webViewState: WebViewState) {
    webViewState.webSettings.apply {
        // JavaScript 支持
        isJavaScriptEnabled = true

        // 禁用缩放
        supportZoom = false

        // 安全设置：禁止文件访问
        allowFileAccessFromFileURLs = false
        allowUniversalAccessFromFileURLs = false

        // 日志级别
        logSeverity = KLogSeverity.Error

        // Android 平台特定设置
        androidWebSettings.apply {
            // 启用 DOM 存储
            domStorageEnabled = true

            // 启用安全浏览
            safeBrowsingEnabled = true

            // 禁止文件访问
            allowFileAccess = false

            // 使用硬件加速
            layerType = PlatformWebSettings.AndroidWebSettings.LayerType.HARDWARE
        }

        iOSWebSettings.apply {
            isInspectable = true
        }
    }
}


@OptIn(ExperimentalResourceApi::class)
suspend fun loadCSSFromResources(fileName: String): String {
    return try {
        Res.readBytes("files/$fileName").decodeToString()
    } catch (e: Exception) {
        println("加载 CSS 文件失败: $fileName - ${e.message}")
        ""
    }
}

@OptIn(ExperimentalResourceApi::class)
suspend fun loadMultipleCSSFromResources(vararg fileNames: String): String {
    val cssContents = mutableListOf<String>()
    for (fileName in fileNames) {
        try {
            val content = Res.readBytes("files/$fileName").decodeToString()
            cssContents.add(content)
        } catch (e: Exception) {
            println("加载 CSS 文件失败: $fileName - ${e.message}")
        }
    }
    return cssContents.joinToString("\n\n")
}

fun wrapHtmlWithCSS(htmlContent: String, cssContent: String): String {
    return """
        <!DOCTYPE html>
        <html>
        <head>
            <meta charset="UTF-8">
            <meta name="viewport" content="width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=no">
            <meta name="color-scheme" content="light only">
            <meta name="supported-color-schemes" content="light">
            <style>
                $cssContent
            </style>
        </head>
        <body>
            $htmlContent
        </body>
        </html>
    """.trimIndent()
}