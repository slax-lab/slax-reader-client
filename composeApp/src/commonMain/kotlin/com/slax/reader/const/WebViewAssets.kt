package com.slax.reader.const

/**
 * WebView资源URL常量
 *
 * 使用自定义域名来加载本地资源
 * Android: 使用 https://appassets.local，通过 WebViewAssetLoader 拦截请求
 * iOS: 使用 appassets://local，通过 WKURLSchemeHandler 拦截请求
 */
object WebViewAssets {
    /**
     * 自定义域名，用于加载本地资源
     * 平台特定实现：
     * - Android: https://appassets.local
     * - iOS: appassets://local
     */
    val ASSET_DOMAIN: String
        get() = getAssetDomain()

    /**
     * 资源文件路径（相对于composeResources/files/）
     */
    object Paths {
        const val HTML_TEMPLATE = "files/html/webview-template.html"
        const val CSS_RESET = "files/css/reset.css"
        const val CSS_ARTICLE = "files/css/article.css"
        const val CSS_BOTTOM_LINE = "files/css/bottom-line.css"
        const val JS_BRIDGE = "files/js/webview-bridge.js"
    }
}

/**
 * 获取平台特定的资源域名
 * 需要在各平台实现
 */
expect fun getAssetDomain(): String