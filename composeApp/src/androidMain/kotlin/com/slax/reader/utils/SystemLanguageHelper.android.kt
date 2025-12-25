package com.slax.reader.utils

import java.util.Locale

/**
 * Android 平台获取系统语言代码
 */
actual fun getSystemLanguageCode(): String {
    val locale = Locale.getDefault()
    val language = locale.language  // 如 "zh", "en"
    val country = locale.country    // 如 "CN", "TW", "US"

    return if (country.isNotEmpty()) {
        "$language-$country"  // 如 "zh-CN", "en-US"
    } else {
        language  // 如 "zh", "en"
    }
}