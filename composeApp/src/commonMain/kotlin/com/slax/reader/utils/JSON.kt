package com.slax.reader.utils

import kotlinx.serialization.json.Json

/**
 * Parse concatenated JSON strings separated by "}\n{"
 */
inline fun <reified T> String.parseConcatenatedJson(): List<T> {
    val trimmedString = this.trim()
    if (trimmedString.isEmpty()) {
        return emptyList()
    }

    val parts = trimmedString.split("}\n{")
    val fixedParts = parts.mapIndexed { index, part ->
        when {
            index == 0 && parts.size > 1 -> "$part}"
            index == parts.size - 1 && parts.size > 1 -> "{$part"
            parts.size > 1 -> "{$part}"
            else -> part
        }
    }

    return fixedParts.mapNotNull { str ->
        try {
            Json.decodeFromString<T>(str)
        } catch (e: Exception) {
            println("Failed to parse JSON: $str, error: ${e.message}")
            null
        }
    }
}