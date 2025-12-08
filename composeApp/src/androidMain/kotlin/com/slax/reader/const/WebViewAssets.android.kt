package com.slax.reader.const

/**
 * Android平台的资源域名实现
 *
 * 使用 https://appassets.local 作为自定义域名
 * 通过 WebViewAssetLoader 拦截该域名的请求
 */
actual fun getAssetDomain(): String = "https://appassets.local"