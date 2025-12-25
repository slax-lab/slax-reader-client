package com.slax.reader.utils


expect fun getSystemLanguageCode(): String

fun getAppSystemLanguage(): String {
    val systemLanguageCode = getSystemLanguageCode()

    val languageCode = if (systemLanguageCode.length > 2) {
        systemLanguageCode.take(2).lowercase()
    } else {
        systemLanguageCode.lowercase()
    }

    return when (languageCode) {
        "zh" -> "zh"
        "en" -> "en"
        else -> "en"
    }
}