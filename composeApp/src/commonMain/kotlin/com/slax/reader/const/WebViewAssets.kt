package com.slax.reader.const

/**
 * WebView资源URL常量
 *
 * 使用自定义域名 https://appassets.local/ 来加载本地资源
 * Android: 通过 WebViewAssetLoader 拦截请求
 * iOS: 通过 WKURLSchemeHandler 拦截请求
 */
object WebViewAssets {
    /**
     * 自定义域名，用于加载本地资源
     */
    const val ASSET_DOMAIN = "https://appassets.local"

    /**
     * HTML模板URL
     */
    const val HTML_TEMPLATE_URL = "$ASSET_DOMAIN/html/webview-template.html"

    /**
     * CSS文件URLs
     */
    object CSS {
        const val RESET = "$ASSET_DOMAIN/css/reset.css"
        const val ARTICLE = "$ASSET_DOMAIN/css/article.css"
        const val BOTTOM_LINE = "$ASSET_DOMAIN/css/bottom-line.css"
    }

    /**
     * JavaScript文件URLs
     */
    object JS {
        const val BRIDGE = "$ASSET_DOMAIN/js/webview-bridge.js"
    }

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