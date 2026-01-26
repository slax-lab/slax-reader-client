package com.slax.reader.reactnative

import com.slax.reader.const.localeString
import com.slax.reader.utils.LocaleString
import de.voize.reaktnativetoolkit.annotation.ReactNativeMethod
import de.voize.reaktnativetoolkit.annotation.ReactNativeModule

/**
 * React Native 国际化桥接模块
 *
 * 功能：
 * 1. 提供获取翻译文本的方法（支持参数插值）
 * 2. 提供当前语言查询方法
 * 3. 提供语言切换方法
 * 4. 提供批量获取所有翻译的方法
 */
@ReactNativeModule("LocaleModule")
class LocaleModule {

    /**
     * 获取当前语言代码
     * @return 语言代码（"zh" 或 "en"）
     */
    @ReactNativeMethod
    fun getCurrentLocale(): String {
        return LocaleString.currentLocale
    }

    /**
     * 获取当前语言的所有翻译文本
     * @return Map<String, String> 所有翻译的键值对
     */
    @ReactNativeMethod
    fun getAllStrings(): Map<String, String> {
        val currentLang = LocaleString.currentLocale
        return localeString.mapNotNull { (key, translations) ->
            translations[currentLang]?.let { key to it }
        }.toMap()
    }

    /**
     * 获取翻译文本（无参数）
     * @param key 翻译键，对应 LocaleString.kt 中的 key
     * @return 翻译后的文本，如果 key 不存在则返回 key 本身
     */
    @ReactNativeMethod
    fun getString(key: String): String {
        return LocaleString.getString(key)
    }

    /**
     * 获取翻译文本（位置参数）
     * @param key 翻译键
     * @param args 位置参数数组，例如 ["John", "Monday"]
     * @return 格式化后的文本
     */
    @ReactNativeMethod
    fun getStringWithArgs(key: String, args: List<String>): String {
        return LocaleString.getString(key, *args.toTypedArray())
    }

    /**
     * 获取翻译文本（命名参数）
     * @param key 翻译键
     * @param args 命名参数 Map，例如 {"name": "John", "day": "Monday"}
     * @return 格式化后的文本
     */
    @ReactNativeMethod
    fun getStringWithNamedArgs(key: String, args: Map<String, String>): String {
        return LocaleString.getString(key, args)
    }

    /**
     * 切换语言
     * @param language 语言代码（"zh" 或 "en"）
     */
    @ReactNativeMethod
    suspend fun changeLocale(language: String) {
        LocaleString.changeLocale(language)
    }
}
