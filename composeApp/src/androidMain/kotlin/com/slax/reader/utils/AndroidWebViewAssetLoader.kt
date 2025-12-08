package com.slax.reader.utils

import android.content.Context
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import androidx.webkit.WebViewAssetLoader
import androidx.webkit.WebViewAssetLoader.PathHandler
import com.slax.reader.const.WebViewAssets
import kotlinx.coroutines.runBlocking
import java.io.ByteArrayInputStream

/**
 * Android WebView 资源加载器
 *
 * 使用WebViewAssetLoader来拦截并处理自定义域名的资源请求
 */
class AndroidWebViewAssetLoader(context: Context) {

    private val assetLoader: WebViewAssetLoader

    init {
        // 创建自定义的PathHandler来处理composeResources
        val composeResourcesHandler = object : PathHandler {
            override fun handle(path: String): WebResourceResponse? {
                return try {
                    // 构建资源路径
                    val resourcePath = "files/$path"
                    println("[AndroidWebViewAssetLoader] 加载资源: $resourcePath")

                    // 通过WebViewResourceLoader读取资源
                    val resourceBytes = runBlocking {
                        WebViewResourceLoader.readResourceBytes(resourcePath)
                    }

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