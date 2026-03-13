package com.slax.reader.domain.image

import com.slax.reader.data.file.FileManager
import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.statement.readRawBytes
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import okio.ByteString.Companion.encodeUtf8

class ImageDownloadManager(
    private val fileManager: FileManager,
    private val httpClient: HttpClient
) {
    private val inFlightRequests = mutableMapOf<String, CompletableDeferred<ByteArray?>>()
    private val mutex = Mutex()

    fun getCachedData(customUrl: String, bookmarkId: String): ByteArray? {
        val originalUrl = resolveUrl(customUrl)
        val path = generateCacheFilePath(bookmarkId, originalUrl)
        return fileManager.streamDataFile(path)
    }

    fun cacheData(customUrl: String, bookmarkId: String, data: ByteArray) {
        val originalUrl = resolveUrl(customUrl)
        val path = generateCacheFilePath(bookmarkId, originalUrl)
        fileManager.writeDataFile(path, data)
    }

    suspend fun ensureCached(customUrl: String, bookmarkId: String): ByteArray? {
        val originalUrl = resolveUrl(customUrl)
        val path = generateCacheFilePath(bookmarkId, originalUrl)

        fileManager.streamDataFile(path)?.let { return it }

        var isOwner = false
        val deferred = mutex.withLock {
            fileManager.streamDataFile(path)?.let { return it }
            inFlightRequests.getOrPut(originalUrl) {
                isOwner = true
                CompletableDeferred()
            }
        }

        if (isOwner) {
            try {
                val data = httpClient.get(originalUrl).readRawBytes()
                fileManager.writeDataFile(path, data)
                deferred.complete(data)
            } catch (e: CancellationException) {
                deferred.cancel(e)
                throw e
            } catch (e: Exception) {
                println("[ImageDownloadManager] 下载失败: $originalUrl, ${e.message}")
                deferred.complete(null)
            } finally {
                mutex.withLock { inFlightRequests.remove(originalUrl) }
            }
        }

        return deferred.await()
    }

    fun resolveUrl(customUrl: String): String {
        return customUrl
            .replace(HTTPS_SCHEME_REGEX, "https://")
            .replace(HTTP_SCHEME_REGEX, "http://")
    }

    private fun generateCacheFilePath(bookmarkId: String, url: String): String {
        val urlMd5 = url.encodeUtf8().md5().hex()
        val ext = url.substringAfterLast(".", "")
            .substringBefore("?")
            .substringBefore("&")
            .take(10)
            .lowercase()
            .ifEmpty { "jpg" }
        return "bookmark/$bookmarkId/images/$urlMd5.$ext"
    }

    companion object {
        private val HTTPS_SCHEME_REGEX = Regex("^slaxstatics://")
        private val HTTP_SCHEME_REGEX = Regex("^slaxstatic://")
    }
}
