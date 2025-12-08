package com.slax.reader.utils

import org.jetbrains.compose.resources.ExperimentalResourceApi
import slax_reader_client.composeapp.generated.resources.Res

/**
 * WebView资源加载器
 *
 * 负责从composeResources加载HTML、CSS、JS等资源文件
 */
object WebViewResourceLoader {

    /**
     * 读取资源文件内容
     * @param path 资源路径（相对于composeResources/）
     * @return 资源内容字符串
     */
    @OptIn(ExperimentalResourceApi::class)
    suspend fun readResource(path: String): String {
        return try {
            Res.readBytes(path).decodeToString()
        } catch (e: Exception) {
            println("[WebViewResourceLoader] 读取资源失败: $path, error: ${e.message}")
            throw e
        }
    }

    /**
     * 读取资源文件字节数据
     * @param path 资源路径（相对于composeResources/）
     * @return 资源字节数组
     */
    @OptIn(ExperimentalResourceApi::class)
    suspend fun readResourceBytes(path: String): ByteArray {
        return try {
            Res.readBytes(path)
        } catch (e: Exception) {
            println("[WebViewResourceLoader] 读取资源失败: $path, error: ${e.message}")
            throw e
        }
    }

    /**
     * 获取资源的MIME类型
     * @param path 资源路径
     * @return MIME类型字符串
     */
    fun getMimeType(path: String): String {
        return when {
            path.endsWith(".html") -> "text/html"
            path.endsWith(".css") -> "text/css"
            path.endsWith(".js") -> "application/javascript"
            path.endsWith(".json") -> "application/json"
            path.endsWith(".png") -> "image/png"
            path.endsWith(".jpg") || path.endsWith(".jpeg") -> "image/jpeg"
            path.endsWith(".gif") -> "image/gif"
            path.endsWith(".svg") -> "image/svg+xml"
            path.endsWith(".woff") -> "font/woff"
            path.endsWith(".woff2") -> "font/woff2"
            path.endsWith(".ttf") -> "font/ttf"
            else -> "application/octet-stream"
        }
    }
}