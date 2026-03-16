package com.slax.reader.domain.image

import com.github.panpf.sketch.fetch.FetchResult
import com.github.panpf.sketch.fetch.Fetcher
import com.github.panpf.sketch.request.RequestContext
import com.github.panpf.sketch.source.ByteArrayDataSource
import com.github.panpf.sketch.source.DataFrom
import com.slax.reader.utils.getMimeTypeFromUrl

class SlaxStaticFetcher(
    private val requestContext: RequestContext,
    private val imageDownloadManager: ImageDownloadManager,
    private val bookmarkId: String
) : Fetcher {

    override suspend fun fetch(): Result<FetchResult> = runCatching {
        val url = requestContext.request.uri.toString()

        val imageData = imageDownloadManager.ensureCached(url, bookmarkId)
            ?: throw IllegalStateException("Failed to load image: $url")

        val mimeType = getMimeTypeFromUrl(url)

        FetchResult(
            dataSource = ByteArrayDataSource(
                data = imageData,
                dataFrom = DataFrom.LOCAL
            ),
            mimeType = mimeType
        )
    }

    class Factory(
        private val imageDownloadManager: ImageDownloadManager,
        private val bookmarkId: String
    ) : Fetcher.Factory {

        override fun create(requestContext: RequestContext): SlaxStaticFetcher? {
            val uri = requestContext.request.uri.toString()
            return if (uri.startsWith("slaxstatic://") || uri.startsWith("slaxstatics://")) {
                SlaxStaticFetcher(requestContext, imageDownloadManager, bookmarkId)
            } else {
                null
            }
        }

        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other !is Factory) return false
            if (bookmarkId != other.bookmarkId) return false
            return true
        }

        override fun hashCode(): Int {
            return bookmarkId.hashCode()
        }

        override fun toString(): String {
            return "SlaxStaticFetcher.Factory(bookmarkId='$bookmarkId')"
        }
    }
}
