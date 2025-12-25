package com.slax.reader.utils

import java.util.Locale

/**
 * Android 平台获取系统语言代码
 */
actual fun getSystemLanguageCode(): String {
    val locale = Locale.getDefault()
    val language = locale.language
    val country = locale.country

    return if (country.isNotEmpty()) {
        "$language-$country"
    } else {
        language
    }
}