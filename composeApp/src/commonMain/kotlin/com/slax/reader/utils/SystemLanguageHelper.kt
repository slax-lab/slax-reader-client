package com.slax.reader.utils

/**
 * 获取系统语言代码
 * @return 系统语言代码（如 "zh-CN", "zh-TW", "en-US", "ja-JP" 等）
 */
expect fun getSystemLanguageCode(): String

/**
 * 将系统语言代码转换为 App 支持的语言代码
 * @param systemLanguageCode 系统语言代码
 * @return App 语言代码（"zh" 或 "en"）
 */
fun mapSystemLanguageToAppLanguage(systemLanguageCode: String): String {
    return when {
        // 简体中文和繁体中文都归类为中文
        systemLanguageCode.startsWith("zh", ignoreCase = true) -> "zh"
        // 其他所有语言都使用英文（兜底）
        else -> "en"
    }
}

/**
 * 获取 App 应该使用的系统语言
 * @return App 语言代码（"zh" 或 "en"）
 */
fun getAppSystemLanguage(): String {
    val systemLanguage = getSystemLanguageCode()
    return mapSystemLanguageToAppLanguage(systemLanguage)
}