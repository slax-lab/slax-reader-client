package com.slax.reader.utils

import java.util.Locale

/**
 * Android 平台获取系统语言代码
 */
actual fun getSystemLanguageCode(): String {
    return Locale.getDefault().toLanguageTag()
}