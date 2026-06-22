package com.slax.reader.utils

import com.fleeksoft.ksoup.Ksoup
import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsBytes

data class ImageSize(val width: Int, val height: Int)

expect fun decodeImageSize(bytes: ByteArray): ImageSize?

private const val MIN_SHARE_IMAGE_DIMEN = 100
private const val MAX_SHARE_IMAGE_CANDIDATES = 8

suspend fun pickFirstArticleShareImage(html: String, httpClient: HttpClient): ByteArray? {
    val candidates = extractImageUrls(html).take(MAX_SHARE_IMAGE_CANDIDATES)
    for (url in candidates) {
        val bytes = runCatching { httpClient.get(url).bodyAsBytes() }.getOrNull() ?: continue
        if (bytes.isEmpty()) continue
        val size = decodeImageSize(bytes) ?: continue
        if (size.width > MIN_SHARE_IMAGE_DIMEN && size.height > MIN_SHARE_IMAGE_DIMEN) {
            return bytes
        }
    }
    return null
}

internal fun extractImageUrls(html: String): List<String> = runCatching {
    Ksoup.parse(html)
        .select("img")
        .mapNotNull { img -> restoreImageUrl(img.attr("src").trim()) }
}.getOrElse { emptyList() }

private fun restoreImageUrl(src: String): String? = when {
    src.isEmpty() -> null
    src.startsWith("slaxstatics://") -> "https://" + src.removePrefix("slaxstatics://")
    src.startsWith("slaxstatic://") -> "http://" + src.removePrefix("slaxstatic://")
    src.startsWith("https://") || src.startsWith("http://") -> src
    else -> null
}
