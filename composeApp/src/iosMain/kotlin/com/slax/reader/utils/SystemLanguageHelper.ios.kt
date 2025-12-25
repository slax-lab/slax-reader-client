package com.slax.reader.utils

import platform.Foundation.NSLocale
import platform.Foundation.currentLocale
import platform.Foundation.languageCode
import platform.Foundation.countryCode

/**
 * iOS 平台获取系统语言代码
 */
actual fun getSystemLanguageCode(): String {
    val locale = NSLocale.currentLocale
    val language = locale.languageCode ?: "en"  // 如 "zh", "en"
    val country = locale.countryCode           // 如 "CN", "TW", "US"

    return if (country != null && country.isNotEmpty()) {
        "$language-$country"  // 如 "zh-CN", "en-US"
    } else {
        language  // 如 "zh", "en"
    }
}