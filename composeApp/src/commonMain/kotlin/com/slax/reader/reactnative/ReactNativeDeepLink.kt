package com.slax.reader.reactnative

import io.ktor.http.encodeURLParameter

private const val REACT_NATIVE_SCHEME = "reactnativeapp"

fun buildReactNativeUrl(
    screen: String,
    params: Map<String, String> = emptyMap()
): String {
    val normalizedScreen = screen.trim().trimStart('/')
    val baseUrl = "$REACT_NATIVE_SCHEME://$normalizedScreen"
    if (params.isEmpty()) return baseUrl

    val query = params.entries
        .joinToString("&") { (key, value) ->
            "${key.encodeURLParameter()}=${value.encodeURLParameter()}"
        }

    return "$baseUrl?$query"
}
