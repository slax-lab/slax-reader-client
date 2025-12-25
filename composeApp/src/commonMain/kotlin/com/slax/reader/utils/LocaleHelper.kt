package com.slax.reader.utils

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.slax.reader.const.localeString
import com.slax.reader.data.preferences.AppPreferences
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

object LocaleString : KoinComponent {
    var currentLocale by mutableStateOf("en")
    private const val FALLBACK_LOCALE: String = "en"

    private val appPreferences: AppPreferences by inject()

    private val positionalTemplateCache = mutableMapOf<String, ParsedPositionalTemplate>()
    private val namedTemplateCache = mutableMapOf<String, ParsedNamedTemplate>()



    init {
        runBlocking {
            val savedLanguage = appPreferences.getUserLanguage()

            if (savedLanguage != null) {
                currentLocale = savedLanguage
            } else {
                val systemLanguage = getAppSystemLanguage()
                currentLocale = systemLanguage
            }
        }
    }

    suspend fun changeLocale(language: String) {
        withContext(Dispatchers.Main) {
            appPreferences.setUserLanguage(language)
            currentLocale = language
        }
    }

    fun getString(key: String): String {
        return localeString[key]?.get(currentLocale)
            ?: localeString[key]?.get(FALLBACK_LOCALE)
            ?: key
    }

    fun getString(key: String, vararg args: Any?): String {
        val template = getString(key)
        return formatPositional(template, *args)
    }

    fun getString(key: String, args: Map<String, Any?>): String {
        val template = getString(key)
        return formatNamed(template, args)
    }

    private sealed class PositionalPart {
        data class Literal(val text: String) : PositionalPart()
        data class Placeholder(val index: Int) : PositionalPart()
    }

    private data class ParsedPositionalTemplate(val parts: List<PositionalPart>)

    private sealed class NamedPart {
        data class Literal(val text: String) : NamedPart()
        data class Placeholder(val name: String) : NamedPart()
    }

    private data class ParsedNamedTemplate(val parts: List<NamedPart>)

    private fun formatPositional(template: String, vararg args: Any?): String {
        if (args.isEmpty()) return template

        val parsed = positionalTemplateCache.getOrPut(template) {
            parsePositionalTemplate(template)
        }

        val result = StringBuilder(template.length + args.size * 8)
        for (part in parsed.parts) {
            when (part) {
                is PositionalPart.Literal -> result.append(part.text)
                is PositionalPart.Placeholder -> {
                    if (part.index < args.size) {
                        result.append(args[part.index].toString())
                    }
                }
            }
        }
        return result.toString()
    }

    private fun parsePositionalTemplate(template: String): ParsedPositionalTemplate {
        val parts = mutableListOf<PositionalPart>()
        var i = 0
        val literalBuilder = StringBuilder()

        while (i < template.length) {
            if (template[i] == '{' && i + 2 < template.length) {
                val closeBrace = template.indexOf('}', i + 1)
                if (closeBrace != -1) {
                    val indexStr = template.substring(i + 1, closeBrace)
                    val index = indexStr.toIntOrNull()
                    if (index != null) {
                        if (literalBuilder.isNotEmpty()) {
                            parts.add(PositionalPart.Literal(literalBuilder.toString()))
                            literalBuilder.clear()
                        }
                        parts.add(PositionalPart.Placeholder(index))
                        i = closeBrace + 1
                        continue
                    }
                }
            }
            literalBuilder.append(template[i])
            i++
        }

        if (literalBuilder.isNotEmpty()) {
            parts.add(PositionalPart.Literal(literalBuilder.toString()))
        }

        return ParsedPositionalTemplate(parts)
    }

    private fun formatNamed(template: String, args: Map<String, Any?>): String {
        if (args.isEmpty()) return template

        val parsed = namedTemplateCache.getOrPut(template) {
            parseNamedTemplate(template)
        }

        val result = StringBuilder(template.length + args.size * 16)
        for (part in parsed.parts) {
            when (part) {
                is NamedPart.Literal -> result.append(part.text)
                is NamedPart.Placeholder -> {
                    args[part.name]?.let { result.append(it.toString()) }
                }
            }
        }
        return result.toString()
    }

    private fun parseNamedTemplate(template: String): ParsedNamedTemplate {
        val parts = mutableListOf<NamedPart>()
        var i = 0
        val literalBuilder = StringBuilder()

        while (i < template.length) {
            if (template[i] == '{' && i + 1 < template.length) {
                val closeBrace = template.indexOf('}', i + 1)
                if (closeBrace != -1) {
                    val name = template.substring(i + 1, closeBrace)
                    if (name.isNotEmpty()) {
                        if (literalBuilder.isNotEmpty()) {
                            parts.add(NamedPart.Literal(literalBuilder.toString()))
                            literalBuilder.clear()
                        }
                        parts.add(NamedPart.Placeholder(name))
                        i = closeBrace + 1
                        continue
                    }
                }
            }
            literalBuilder.append(template[i])
            i++
        }

        if (literalBuilder.isNotEmpty()) {
            parts.add(NamedPart.Literal(literalBuilder.toString()))
        }

        return ParsedNamedTemplate(parts)
    }
}

fun String.i18n(): String = LocaleString.getString(this)

fun String.i18n(vararg args: Any?): String = LocaleString.getString(this, *args)

fun String.i18n(args: Map<String, Any?>): String = LocaleString.getString(this, args)