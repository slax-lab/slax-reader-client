package com.slax.reader.utils

import com.slax.reader.const.WebViewAssets
import kotlinx.cinterop.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import platform.Foundation.*
import platform.WebKit.*
import platform.darwin.NSObject
import kotlin.time.TimeSource

/**
 * iOS WebView 资源加载配置
 */
object IOSWebViewConfig {
    /**
     * 是否启用模拟延时（用于测试阻塞情况）
     */
    var enableSimulatedDelay: Boolean = true

    /**
     * 模拟延时的时长（毫秒）
     */
    var simulatedDelayMillis: Long = 10000L
}

/**
 * iOS WebView URL Scheme Handler
 *
 * 用于拦截并处理自定义域名（appassets://local/）的资源请求
 * 注意：iOS不允许拦截原生的https scheme，因此必须使用自定义scheme
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

                // 记录开始时间
                val startMark = TimeSource.Monotonic.markNow()

                val isJavaScriptFile = resourcePath.lowercase().endsWith(".js")

                // 模拟延时（用于测试阻塞情况）
                if (IOSWebViewConfig.enableSimulatedDelay && isJavaScriptFile) {
                    println("[IOSWebViewURLSchemeHandler] 模拟延时: ${IOSWebViewConfig.simulatedDelayMillis}ms")
                    delay(IOSWebViewConfig.simulatedDelayMillis)
                }

                // 读取资源内容
                val resourceBytes = WebViewResourceLoader.readResourceBytes("files/$resourcePath")

                // 计算耗时
                val durationMicros = startMark.elapsedNow().inWholeMicroseconds
                val durationMillis = (durationMicros / 10).toDouble() / 100.0  // 保留两位小数
                println("[IOSWebViewURLSchemeHandler] 文件读取耗时: ${durationMicros}μs (${durationMillis}ms), 路径: files/$resourcePath, 大小: ${resourceBytes.size} bytes")

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
        // appassets://local/js/webview-bridge.js -> js/webview-bridge.js
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
