package com.slax.reader.utils

import android.content.Context
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import androidx.webkit.WebViewAssetLoader
import androidx.webkit.WebViewAssetLoader.PathHandler
import com.slax.reader.const.WebViewAssets
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import java.io.ByteArrayInputStream
import java.util.Locale

/**
 * Android WebView 资源加载器
 *
 * 使用WebViewAssetLoader来拦截并处理自定义域名的资源请求
 */
class AndroidWebViewAssetLoader(context: Context) {

    companion object {
        /**
         * 是否启用模拟延时（用于测试阻塞情况）
         */
        var enableSimulatedDelay: Boolean = true

        /**
         * 模拟延时的时长（毫秒）
         */
        var simulatedDelayMillis: Long = 10000L
    }

    private val assetLoader: WebViewAssetLoader

    init {
        // 创建自定义的PathHandler来处理composeResources
        val composeResourcesHandler = object : PathHandler {
            override fun handle(path: String): WebResourceResponse? {
                return try {
                    // 构建资源路径
                    val resourcePath = "files/$path"
                    println("[AndroidWebViewAssetLoader] 加载资源: $resourcePath")

                    // 记录开始时间（纳秒）
                    val startTime = System.nanoTime()

                    val isJavaScriptFile = path.lowercase(Locale.US).endsWith(".js")
                    val isCssFile = path.lowercase(Locale.US).endsWith(".css")

                    // 通过WebViewResourceLoader读取资源
                    val resourceBytes = runBlocking {
                        // 模拟延时（用于测试阻塞情况）
                        if (enableSimulatedDelay && isJavaScriptFile) {
                            println("[AndroidWebViewAssetLoader] 模拟延时: ${simulatedDelayMillis}ms")
                            delay(simulatedDelayMillis)
                        }

                        WebViewResourceLoader.readResourceBytes(resourcePath)
                    }

                    // 记录结束时间并计算耗时
                    val endTime = System.nanoTime()
                    val durationMicros = (endTime - startTime) / 1000
                    val durationMillis = durationMicros / 1000.0
                    println("[AndroidWebViewAssetLoader] 文件读取耗时: ${durationMicros}μs (${String.format(Locale.US, "%.2f", durationMillis)}ms), 路径: $resourcePath, 大小: ${resourceBytes.size} bytes")

                    // 获取MIME类型
                    val mimeType = WebViewResourceLoader.getMimeType(path)

                    // 创建WebResourceResponse
                    WebResourceResponse(
                        mimeType,
                        "UTF-8",
                        ByteArrayInputStream(resourceBytes)
                    ).apply {
                        // 设置响应头
                        responseHeaders = mapOf(
                            "Access-Control-Allow-Origin" to "*",
                            "Cache-Control" to "public, max-age=3600"
                        )
                    }
                } catch (e: Exception) {
                    println("[AndroidWebViewAssetLoader] 加载资源失败: $path, error: ${e.message}")
                    e.printStackTrace()
                    null
                }
            }
        }

        // 配置WebViewAssetLoader
        assetLoader = WebViewAssetLoader.Builder()
            .setDomain("appassets.local")
            // 注册自定义PathHandler到根路径，这样可以处理所有请求
            .addPathHandler("/", composeResourcesHandler)
            .build()
    }

    /**
     * 拦截WebView资源请求
     *
     * @param request WebView资源请求
     * @return WebResourceResponse 如果成功拦截则返回响应，否则返回null
     */
    fun shouldInterceptRequest(request: WebResourceRequest): WebResourceResponse? {
        val url = request.url.toString()

        // 只拦截自定义域名的请求
        if (!url.startsWith(WebViewAssets.ASSET_DOMAIN)) {
            return null
        }

        println("[AndroidWebViewAssetLoader] 拦截请求: $url")
        return assetLoader.shouldInterceptRequest(request.url)
    }
}