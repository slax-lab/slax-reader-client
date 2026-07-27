package com.slax.reader.domain.sync

import app.slax.reader.SlaxConfig
import com.fleeksoft.ksoup.Ksoup
import com.slax.reader.const.AppError
import com.slax.reader.data.database.dao.CollectionDao
import com.slax.reader.data.database.dao.LocalBookmarkDao
import com.slax.reader.data.database.model.CollectionBookmarkCacheCandidate
import com.slax.reader.data.database.model.LocalBookmarkInfo
import com.slax.reader.data.database.model.isDownloaded
import com.slax.reader.data.file.FileManager
import com.slax.reader.data.network.ApiService
import com.slax.reader.data.preferences.AppPreferences
import com.slax.reader.domain.image.ImageDownloadManager
import kotlinx.atomicfu.atomic
import kotlinx.atomicfu.getAndUpdate
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.IO
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapMerge
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private data class CollectionCacheSnapshot(
    val bookmarks: List<CollectionBookmarkCacheCandidate>,
    val localBookmarks: Map<String, LocalBookmarkInfo>,
    val cacheCount: Int,
)

private data class CollectionTaskItem(
    val bookmarkId: String,
    val cacheKey: String,
)

class CollectionBackgroundDomain(
    private val collectionDao: CollectionDao,
    private val localBookmarkDao: LocalBookmarkDao,
    private val fileManager: FileManager,
    private val apiService: ApiService,
    private val imageDownloadManager: ImageDownloadManager,
    private val appPreferences: AppPreferences,
) {
    private val successStatus = "success"
    private val maxDownloadConcurrent = 3
    private val inQueue = atomic(setOf<String>())
    private val failedInSession = atomic(setOf<String>())
    private val isCleaningUp = atomic(false)
    private var workerScope: CoroutineScope? = null
    private var downloadQueue: Channel<CollectionTaskItem>? = null

    @OptIn(ExperimentalCoroutinesApi::class, FlowPreview::class)
    fun startup() {
        if (workerScope != null) return

        val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        val queue = Channel<CollectionTaskItem>(100)
        workerScope = scope
        downloadQueue = queue

        scope.launch {
            combine(
                collectionDao.watchCollectionBookmarkCacheCandidates(),
                localBookmarkDao.watchUserLocalBookmarkMap(),
                appPreferences.getCacheCount(),
            ) { bookmarks, localBookmarks, cacheCount ->
                CollectionCacheSnapshot(bookmarks, localBookmarks, cacheCount)
            }.collect { snapshot ->
                scheduleSnapshot(snapshot, queue)
            }
        }

        scope.launch {
            queue.receiveAsFlow()
                .flatMapMerge(maxDownloadConcurrent) { task ->
                    flow {
                        downloadBookmark(task)
                        emit(Unit)
                    }
                }
                .collect {
                    println("[CollectionBackgroundDomain] task completed")
                }
        }
    }

    private suspend fun scheduleSnapshot(snapshot: CollectionCacheSnapshot, queue: Channel<CollectionTaskItem>) {
        val cacheLimit = if (snapshot.cacheCount == -1) {
            Int.MAX_VALUE
        } else {
            snapshot.cacheCount.coerceAtLeast(0)
        }
        val collectionCounts = mutableMapOf<String, Int>()
        val cacheWindowKeys = mutableSetOf<String>()
        val toDownload = linkedMapOf<String, CollectionTaskItem>()

        for (item in snapshot.bookmarks) {
            if (item.collectionId.isBlank() || item.metadataStatus != successStatus) continue

            val cacheKey = cacheKey(item.collectionId, item.id)
            val local = snapshot.localBookmarks[cacheKey]
            if (local != null && !local.isAutoCached && local.isDownloaded()) continue

            val collectionCount = collectionCounts[item.collectionId] ?: 0
            if (collectionCount >= cacheLimit) continue

            collectionCounts[item.collectionId] = collectionCount + 1
            cacheWindowKeys.add(cacheKey)
            if (local?.isDownloaded() != true && cacheKey !in failedInSession.value) {
                toDownload[cacheKey] = CollectionTaskItem(item.id, cacheKey)
            }
        }

        val toCleanupKeys = snapshot.localBookmarks.mapNotNull { (key, _) ->
            key.takeIf {
                key.startsWith(CACHE_KEY_PREFIX) &&
                    key !in cacheWindowKeys
            }
        }

        println(
            "[CollectionBackgroundDomain] collections=${collectionCounts.size}, " +
                "window=${collectionCounts.values.sum()}, " +
                "toDownload=${toDownload.size}, toCleanup=${toCleanupKeys.size}"
        )

        if (toCleanupKeys.isNotEmpty()) cleanupOldCache(toCleanupKeys)

        for (task in toDownload.values) {
            val added = inQueue.getAndUpdate { it + task.cacheKey }.let { task.cacheKey !in it }
            if (added) queue.send(task)
        }
    }

    private suspend fun downloadBookmark(task: CollectionTaskItem) {
        val contentPath = "bookmark/${task.cacheKey}/content.html"
        try {
            updateBookmarkStatus(task.cacheKey, DownloadStatus.DOWNLOADING)
            val content = processContent(apiService.getBookmarkRawContent(task.bookmarkId))
            fileManager.writeDataFile(contentPath, content.html.encodeToByteArray())

            if (appPreferences.getDownloadImages().first() && content.imageUrls.isNotEmpty()) {
                coroutineScope {
                    launch {
                        content.imageUrls.forEach { url ->
                            runCatching { imageDownloadManager.ensureCached(url, task.cacheKey) }
                                .onFailure { error ->
                                    println("[CollectionBackgroundDomain] image cache failed: $url, ${error.message}")
                                }
                        }
                    }
                }
            }

            updateBookmarkStatus(task.cacheKey, DownloadStatus.COMPLETED)
            failedInSession.getAndUpdate { it - task.cacheKey }
        } catch (error: Exception) {
            println("[CollectionBackgroundDomain] download failed ${task.bookmarkId}: ${error.message}")
            failedInSession.getAndUpdate { it + task.cacheKey }
            updateBookmarkStatus(task.cacheKey, DownloadStatus.FAILED)
        } finally {
            inQueue.getAndUpdate { it - task.cacheKey }
        }
    }

    private suspend fun cleanupOldCache(ids: List<String>) {
        if (!isCleaningUp.compareAndSet(expect = false, update = true)) return
        try {
            ids.forEach { id ->
                runCatching { fileManager.deleteDataDirectory("bookmark/$id") }
                    .onFailure { error ->
                        println("[CollectionBackgroundDomain] cleanup failed $id: ${error.message}")
                    }
            }
            runCatching { localBookmarkDao.deleteLocalBookmarkInfo(ids) }
                .onFailure { error ->
                    println("[CollectionBackgroundDomain] failed to delete cache state: ${error.message}")
                }
        } finally {
            isCleaningUp.value = false
        }
    }

    private suspend fun updateBookmarkStatus(id: String, status: DownloadStatus) {
        val statusCode = when (status) {
            DownloadStatus.NONE -> 0
            DownloadStatus.DOWNLOADING -> 1
            DownloadStatus.COMPLETED -> 2
            DownloadStatus.FAILED -> 3
        }
        localBookmarkDao.updateLocalBookmarkDownloadStatus(id, statusCode, isAutoCached = true)
    }

    suspend fun getBookmarkContent(id: String, collectionId: String): ProcessedContent {
        val cacheKey = cacheKey(collectionId, id)
        val contentPath = "bookmark/$cacheKey/content.html"
        fileManager.streamDataFile(contentPath)?.let { cached ->
            return processContent(cached.decodeToString())
        }

        inQueue.getAndUpdate { it + cacheKey }
        return try {
            withContext(Dispatchers.IO) {
                updateBookmarkStatus(cacheKey, DownloadStatus.DOWNLOADING)
            }
            val content = processContent(apiService.getBookmarkRawContent(id))
            workerScope?.launch(Dispatchers.IO) {
                runCatching {
                    fileManager.writeDataFile(contentPath, content.html.encodeToByteArray())
                    updateBookmarkStatus(cacheKey, DownloadStatus.COMPLETED)
                    failedInSession.getAndUpdate { it - cacheKey }
                }.onFailure { error ->
                    println("[CollectionBackgroundDomain] background write failed $id: ${error.message}")
                    failedInSession.getAndUpdate { it + cacheKey }
                    updateBookmarkStatus(cacheKey, DownloadStatus.FAILED)
                }
            }
            content
        } catch (error: Exception) {
            println("[CollectionBackgroundDomain] API request failed $id: ${error.message}")
            failedInSession.getAndUpdate { it + cacheKey }
            val errorInfo = when (error) {
                is AppError.ApiException.HttpError -> "Error code: ${error.code}" to error.message
                else -> "Network error" to (error.message ?: "Unknown error")
            }
            ProcessedContent(
                SlaxConfig.DETAIL_ERROR_TEMPLATE
                    .replace("{{TITLE}}", "<center>Failed to load content</center>")
                    .replace("{{REASON}}", "<center>${errorInfo.first}</center>")
                    .replace("{{DETAIL}}", "<center>${errorInfo.second}</center>"),
                emptyList(),
            )
        } finally {
            inQueue.getAndUpdate { it - cacheKey }
        }
    }

    private fun processContent(html: String): ProcessedContent {
        return runCatching {
            val doc = Ksoup.parse(html)
            doc.outputSettings().prettyPrint(false)
            val urls = mutableListOf<String>()
            doc.select("img").forEach { image ->
                val source = image.attr("src")
                if (source.isNotEmpty() && !source.startsWith("data:")) {
                    val cachedSource = source
                        .replace(HTTPS_REGEX, "slaxstatics://")
                        .replace(HTTP_REGEX, "slaxstatic://")
                    image.attr("src", cachedSource)
                    urls.add(cachedSource)
                }
            }
            ProcessedContent(doc.html(), urls)
        }.getOrElse { error ->
            println("[CollectionBackgroundDomain] HTML processing failed: ${error.message}")
            ProcessedContent(html, emptyList())
        }
    }

    fun restart() {
        cleanup()
        startup()
    }

    fun cleanup() {
        downloadQueue?.cancel()
        downloadQueue = null
        workerScope?.cancel()
        workerScope = null
        inQueue.value = emptySet()
        failedInSession.value = emptySet()
    }

    companion object {
        private const val CACHE_KEY_PREFIX = "collection-"
        private val HTTPS_REGEX = Regex("^https://")
        private val HTTP_REGEX = Regex("^http://")

        fun cacheKey(collectionId: String, bookmarkId: String): String =
            "$CACHE_KEY_PREFIX$collectionId-$bookmarkId"
    }
}
