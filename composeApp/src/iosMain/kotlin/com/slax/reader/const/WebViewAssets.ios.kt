package com.slax.reader.const

/**
 * iOS平台的资源域名实现
 *
 * 使用 appassets://local 作为自定义URL Scheme
 * 通过 WKURLSchemeHandler 拦截该scheme的请求
 *
 * 注意：iOS不允许拦截原生的 https scheme，因此必须使用自定义scheme
 */
actual fun getAssetDomain(): String = "appassets://local"