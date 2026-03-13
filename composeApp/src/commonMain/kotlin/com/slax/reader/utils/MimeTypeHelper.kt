package com.slax.reader.utils

fun getMimeTypeFromUrl(url: String): String {
    val ext = url.substringAfterLast(".").substringBefore("?").lowercase()
    return when (ext) {
        "jpg", "jpeg" -> "image/jpeg"
        "png"         -> "image/png"
        "gif"         -> "image/gif"
        "webp"        -> "image/webp"
        "svg"         -> "image/svg+xml"
        else          -> "image/jpeg"
    }
}