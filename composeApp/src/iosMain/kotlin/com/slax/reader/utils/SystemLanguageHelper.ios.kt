package com.slax.reader.utils

import platform.Foundation.NSLocale
import platform.Foundation.preferredLanguages

/**
 * iOS 平台获取系统语言代码
 */
actual fun getSystemLanguageCode(): String {
    val preferredLanguages = NSLocale.preferredLanguages()

    return if (preferredLanguages.isNotEmpty()) {
        preferredLanguages.first() as String
    } else {
        "en"
    }
}