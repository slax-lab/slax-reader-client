package com.slax.reader.utils

import com.slax.reader.domain.image.ImageDownloadManager
import io.ktor.client.HttpClient
import io.ktor.client.request.prepareGet
import io.ktor.client.statement.bodyAsChannel
import io.ktor.http.contentLength
import io.ktor.http.contentType
import io.ktor.utils.io.readAvailable
import kotlinx.cinterop.*
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import platform.Foundation.*
import platform.WebKit.*
import platform.darwin.NSObject

@OptIn(ExperimentalForeignApi::class)
class CustomURLSchemeHandler(
    private val imageDownloadManager: ImageDownloadManager,
    private val httpClient: HttpClient,
    private val bookmarkId: String,
    private val scope: CoroutineScope
) : NSObject(), WKURLSchemeHandlerProtocol {

    private val activeTasks = mutableMapOf<WKURLSchemeTaskProtocol, Job>()

    @ObjCSignatureOverride
    override fun webView(webView: WKWebView, startURLSchemeTask: WKURLSchemeTaskProtocol) {
        val url = startURLSchemeTask.request.URL?.absoluteString ?: return
        val requestURL = startURLSchemeTask.request.URL ?: return

        activeTasks[startURLSchemeTask] = scope.launch(Dispatchers.Default) {
            val cached = imageDownloadManager.getCachedData(url, bookmarkId)
            if (cached != null) {
                respondWithData(startURLSchemeTask, requestURL, url, cached)
                return@launch
            }

            proxyFromOrigin(startURLSchemeTask, requestURL, url)
        }
    }

    private suspend fun respondWithData(
        task: WKURLSchemeTaskProtocol,
        requestURL: NSURL,
        url: String,
        data: ByteArray
    ) {
        withContext(Dispatchers.Main) {
            if (task !in activeTasks) return@withContext
            val nsData = data.toNSData()
            task.didReceiveResponse(
                NSURLResponse(requestURL, getMimeTypeFromUrl(url), data.size.toLong(), null)
            )
            task.didReceiveData(nsData)
            task.didFinish()
            activeTasks.remove(task)
        }
    }

    private suspend fun proxyFromOrigin(
        task: WKURLSchemeTaskProtocol,
        requestURL: NSURL,
        url: String
    ) {
        try {
            val originalUrl = imageDownloadManager.resolveUrl(url)

            httpClient.prepareGet(originalUrl).execute { response ->
                val contentLength = response.contentLength() ?: -1L
                val mimeType = response.contentType()?.let { "${it.contentType}/${it.contentSubtype}" }
                    ?: getMimeTypeFromUrl(url)

                withContext(Dispatchers.Main) {
                    if (task !in activeTasks) return@withContext
                    task.didReceiveResponse(
                        NSURLResponse(requestURL, mimeType, contentLength, null)
                    )
                }

                val channel = response.bodyAsChannel()
                val accumulator = mutableListOf<ByteArray>()
                val buffer = ByteArray(8192)

                while (!channel.isClosedForRead) {
                    val bytesRead = channel.readAvailable(buffer)
                    if (bytesRead <= 0) continue
                    val chunk = buffer.copyOf(bytesRead)
                    accumulator.add(chunk)

                    withContext(Dispatchers.Main) {
                        if (task !in activeTasks) return@withContext
                        task.didReceiveData(chunk.toNSData())
                    }
                }

                val fullData = ByteArray(accumulator.sumOf { it.size }).also { result ->
                    var offset = 0
                    accumulator.forEach { chunk ->
                        chunk.copyInto(result, offset)
                        offset += chunk.size
                    }
                }

                withContext(Dispatchers.Main) {
                    if (task !in activeTasks) return@withContext
                    imageDownloadManager.cacheData(url, bookmarkId, fullData)
                    task.didFinish()
                    activeTasks.remove(task)
                }
            }
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            println("[SchemeHandler] 代理失败: $url, ${e.message}")
            withContext(Dispatchers.Main) {
                if (task !in activeTasks) return@withContext
                task.didFailWithError(
                    NSError.errorWithDomain(
                        "com.slax.reader", 502,
                        mapOf(NSLocalizedDescriptionKey to (e.message ?: "Proxy error"))
                    )
                )
                activeTasks.remove(task)
            }
        }
    }

    @ObjCSignatureOverride
    override fun webView(webView: WKWebView, stopURLSchemeTask: WKURLSchemeTaskProtocol) {
        activeTasks.remove(stopURLSchemeTask)?.cancel()
    }

    @OptIn(BetaInteropApi::class)
    private fun ByteArray.toNSData(): NSData {
        return this.usePinned { pinned ->
            NSData.create(bytes = pinned.addressOf(0), length = this.size.toULong())
        }
    }
}
