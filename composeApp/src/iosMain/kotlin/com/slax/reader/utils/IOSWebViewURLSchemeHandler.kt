package com.slax.reader.utils

import com.slax.reader.const.WebViewAssets
import kotlinx.cinterop.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import platform.Foundation.*
import platform.WebKit.*
import platform.darwin.NSObject

/**
 * iOS WebView URL Scheme Handler
 *
 * 用于拦截并处理自定义域名（https://appassets.local/）的资源请求
 */
@OptIn(ExperimentalForeignApi::class)
class IOSWebViewURLSchemeHandler : NSObject(), WKURLSchemeHandlerProtocol {

    private val coroutineScope = CoroutineScope(Dispatchers.Main)

    /**
     * 开始处理URL请求
     */
    @ObjCSignatureOverride
    override fun webView(webView: WKWebView, startURLSchemeTask: WKURLSchemeTaskProtocol) {
        val request = startURLSchemeTask.request
        val url = request.URL?.absoluteString ?: return

        println("[IOSWebViewURLSchemeHandler] 开始处理请求: $url")

        // 检查是否是自定义域名的请求
        if (!url.startsWith(WebViewAssets.ASSET_DOMAIN)) {
            println("[IOSWebViewURLSchemeHandler] 非自定义域名，忽略: $url")
            return
        }

        // 异步加载资源
        coroutineScope.launch {
            try {
                // 提取资源路径
                val resourcePath = extractResourcePath(url)
                println("[IOSWebViewURLSchemeHandler] 资源路径: $resourcePath")

                // 读取资源内容
                val resourceBytes = WebViewResourceLoader.readResourceBytes("files/$resourcePath")

                // 获取MIME类型
                val mimeType = WebViewResourceLoader.getMimeType(resourcePath)

                // 创建响应
                val response = NSHTTPURLResponse(
                    uRL = request.URL!!,
                    statusCode = 200,
                    HTTPVersion = "HTTP/1.1",
                    headerFields = mapOf(
                        "Content-Type" to mimeType,
                        "Content-Length" to resourceBytes.size.toString(),
                        "Access-Control-Allow-Origin" to "*",
                        "Cache-Control" to "public, max-age=3600"
                    )
                )

                // 发送响应头
                startURLSchemeTask.didReceiveResponse(response)

                // 发送数据
                val data = resourceBytes.toNSData()
                startURLSchemeTask.didReceiveData(data)

                // 完成请求
                startURLSchemeTask.didFinish()

                println("[IOSWebViewURLSchemeHandler] 请求处理完成: $url")
            } catch (e: Exception) {
                println("[IOSWebViewURLSchemeHandler] 请求处理失败: $url, error: ${e.message}")
                e.printStackTrace()

                // 发送错误响应
                val error = NSError.errorWithDomain(
                    domain = "com.slax.reader.webview",
                    code = -1,
                    userInfo = mapOf(
                        NSLocalizedDescriptionKey to "资源加载失败: ${e.message}"
                    )
                )
                startURLSchemeTask.didFailWithError(error)
            }
        }
    }

    /**
     * 停止处理URL请求
     */
    @ObjCSignatureOverride
    override fun webView(webView: WKWebView, stopURLSchemeTask: WKURLSchemeTaskProtocol) {
        val url = stopURLSchemeTask.request.URL?.absoluteString
        println("[IOSWebViewURLSchemeHandler] 停止处理请求: $url")
        // 可以在这里取消正在进行的请求，但由于我们使用协程，通常请求会很快完成
    }

    /**
     * 从URL中提取资源路径
     */
    private fun extractResourcePath(url: String): String {
        // 移除域名部分，例如：
        // https://appassets.local/js/webview-bridge.js -> js/webview-bridge.js
        return url.removePrefix(WebViewAssets.ASSET_DOMAIN).removePrefix("/")
    }

    /**
     * 将ByteArray转换为NSData
     */
    @OptIn(UnsafeNumber::class, BetaInteropApi::class)
    private fun ByteArray.toNSData(): NSData {
        if (isEmpty()) return NSData()
        return usePinned { pinned ->
            NSData.create(bytes = pinned.addressOf(0), length = size.toULong())
        }
    }
}