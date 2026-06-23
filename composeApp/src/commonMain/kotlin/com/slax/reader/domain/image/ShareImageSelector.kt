package com.slax.reader.domain.image

import com.slax.reader.utils.decodeImageSize
import com.slax.reader.utils.isAndroid

class ShareImageSelector(
    private val imageDownloadManager: ImageDownloadManager
) {
    companion object {
        private const val MIN_DIMEN = 100
        private const val MAX_CANDIDATES = 2
    }

    suspend fun pick(imageUrls: List<String>, bookmarkId: String): ByteArray? {
        if (isAndroid()) return null
        for (url in imageUrls.take(MAX_CANDIDATES)) {
            val bytes = runCatching { imageDownloadManager.ensureCached(url, bookmarkId) }.getOrNull() ?: continue
            if (bytes.isEmpty()) continue
            val size = decodeImageSize(bytes) ?: continue
            if (size.width > MIN_DIMEN && size.height > MIN_DIMEN) {
                return bytes
            }
        }
        return null
    }
}
