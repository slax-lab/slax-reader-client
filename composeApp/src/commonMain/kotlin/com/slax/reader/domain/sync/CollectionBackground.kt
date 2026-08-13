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
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.IO
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapMerge
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
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
    private val foregroundAccessCounts = atomic(emptyMap<String, Int>())
    private val isCleaningUp = atomic(false)
    private val lifecycleMutex = Mutex()
    private var workerScope: CoroutineScope? = null
    private var downloadQueue: Channel<CollectionTaskItem>? = null

    @OptIn(ExperimentalCoroutinesApi::class, FlowPreview::class)
    suspend fun startup() = lifecycleMutex.withLock {
        if (workerScope != null) return@withLock

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
        val ownerCounts = mutableMapOf<String, Int>()
        val cacheWindowKeys = mutableSetOf<String>()
        val toDownload = linkedMapOf<String, CollectionTaskItem>()

        for (item in snapshot.bookmarks) {
            if (item.ownerId.isBlank() || item.metadataStatus != successStatus) continue

            val cacheKey = cacheKey(item.ownerId, item.id)
            val local = snapshot.localBookmarks[cacheKey]
            if (local != null && !local.isAutoCached && local.isDownloaded()) continue

            val ownerCount = ownerCounts[item.ownerId] ?: 0
            if (ownerCount >= cacheLimit) continue

            ownerCounts[item.ownerId] = ownerCount + 1
            cacheWindowKeys.add(cacheKey)
            if (
                local?.isDownloaded() != true &&
                cacheKey !in failedInSession.value &&
                cacheKey !in foregroundAccessCounts.value
            ) {
                toDownload[cacheKey] = CollectionTaskItem(item.id, cacheKey)
            }
        }

        val toCleanupKeys = snapshot.localBookmarks.mapNotNull { (key, info) ->
            key.takeIf {
                key.startsWith(CACHE_KEY_PREFIX) &&
                    key !in cacheWindowKeys &&
                    info.isAutoCached &&
                    info.isDownloaded() &&
                    key !in inQueue.value &&
                    key !in foregroundAccessCounts.value
            }
        }

        println(
            "[CollectionBackgroundDomain] owners=${ownerCounts.size}, window=${ownerCounts.values.sum()}, " +
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
                            try {
                                imageDownloadManager.ensureCached(url, task.cacheKey)
                            } catch (error: CancellationException) {
                                throw error
                            } catch (error: Exception) {
                                println("[CollectionBackgroundDomain] image cache failed: $url, ${error.message}")
                            }
                        }
                    }
                }
            }

            updateBookmarkStatus(task.cacheKey, DownloadStatus.COMPLETED)
            failedInSession.getAndUpdate { it - task.cacheKey }
        } catch (error: CancellationException) {
            throw error
        } catch (error: Exception) {
            println("[CollectionBackgroundDomain] download failed ${task.bookmarkId}: ${error.message}")
            failedInSession.getAndUpdate { it + task.cacheKey }
            try {
                updateBookmarkStatus(task.cacheKey, DownloadStatus.FAILED)
            } catch (statusError: CancellationException) {
                throw statusError
            } catch (statusError: Exception) {
                println("[CollectionBackgroundDomain] failed to persist FAILED status: ${statusError.message}")
            }
        } finally {
            inQueue.getAndUpdate { it - task.cacheKey }
        }
    }

    private suspend fun cleanupOldCache(ids: List<String>) {
        if (!isCleaningUp.compareAndSet(expect = false, update = true)) return
        try {
            val deletedIds = mutableListOf<String>()
            ids.forEach { id ->
                val isManual = try {
                    localBookmarkDao.isManuallyCached(id)
                } catch (error: CancellationException) {
                    throw error
                } catch (error: Exception) {
                    println("[CollectionBackgroundDomain] failed to verify cache ownership $id: ${error.message}")
                    true
                }
                if (
                    id in inQueue.value ||
                    id in foregroundAccessCounts.value ||
                    isManual
                ) return@forEach
                try {
                    if (fileManager.deleteDataDirectory("bookmark/$id")) {
                        deletedIds.add(id)
                    } else {
                        println("[CollectionBackgroundDomain] cleanup failed $id")
                    }
                } catch (error: CancellationException) {
                    throw error
                } catch (error: Exception) {
                    println("[CollectionBackgroundDomain] cleanup failed $id: ${error.message}")
                }
            }
            try {
                localBookmarkDao.deleteLocalBookmarkInfo(deletedIds)
            } catch (error: CancellationException) {
                throw error
            } catch (error: Exception) {
                println("[CollectionBackgroundDomain] failed to delete cache state: ${error.message}")
            }
        } finally {
            isCleaningUp.value = false
        }
    }

    private suspend fun updateBookmarkStatus(
        id: String,
        status: DownloadStatus,
        isAutoCached: Boolean = true,
    ) {
        val statusCode = when (status) {
            DownloadStatus.NONE -> 0
            DownloadStatus.DOWNLOADING -> 1
            DownloadStatus.COMPLETED -> 2
            DownloadStatus.FAILED -> 3
        }
        if (isAutoCached) {
            localBookmarkDao.updateAutoCachedDownloadStatus(id, statusCode)
        } else {
            localBookmarkDao.updateLocalBookmarkDownloadStatus(id, statusCode, isAutoCached = false)
        }
    }

    suspend fun getBookmarkContent(id: String, ownerId: String): ProcessedContent {
        val cacheKey = cacheKey(ownerId, id)
        val contentPath = "bookmark/$cacheKey/content.html"
        foregroundAccessCounts.getAndUpdate { counts ->
            counts + (cacheKey to ((counts[cacheKey] ?: 0) + 1))
        }
        try {
            fileManager.streamDataFile(contentPath)?.let { cached ->
                // Reading an auto-cached article must not pin it as a manual cache entry.
                failedInSession.getAndUpdate { it - cacheKey }
                return processContent(cached.decodeToString())
            }

            updateForegroundStatus(cacheKey, DownloadStatus.DOWNLOADING)
            val content = try {
                processContent(apiService.getBookmarkRawContent(id))
            } catch (error: CancellationException) {
                throw error
            } catch (error: Exception) {
                println("[CollectionBackgroundDomain] API request failed $id: ${error.message}")
                failedInSession.getAndUpdate { it + cacheKey }
                updateForegroundStatus(cacheKey, DownloadStatus.FAILED)
                val errorInfo = when (error) {
                    is AppError.ApiException.HttpError -> "Error code: ${error.code}" to error.message
                    else -> "Network error" to (error.message ?: "Unknown error")
                }
                return ProcessedContent(
                    SlaxConfig.DETAIL_ERROR_TEMPLATE
                        .replace("{{TITLE}}", "<center>Failed to load content</center>")
                        .replace("{{REASON}}", "<center>${errorInfo.first}</center>")
                        .replace("{{DETAIL}}", "<center>${errorInfo.second}</center>"),
                    emptyList(),
                )
            }

            try {
                withContext(Dispatchers.IO) {
                    fileManager.writeDataFile(contentPath, content.html.encodeToByteArray())
                    updateBookmarkStatus(cacheKey, DownloadStatus.COMPLETED, isAutoCached = false)
                }
                failedInSession.getAndUpdate { it - cacheKey }
            } catch (error: CancellationException) {
                throw error
            } catch (error: Exception) {
                println("[CollectionBackgroundDomain] cache write failed $id: ${error.message}")
                failedInSession.getAndUpdate { it + cacheKey }
                updateForegroundStatus(cacheKey, DownloadStatus.FAILED)
            }
            return content
        } finally {
            foregroundAccessCounts.getAndUpdate { counts ->
                val remaining = (counts[cacheKey] ?: 1) - 1
                if (remaining <= 0) counts - cacheKey else counts + (cacheKey to remaining)
            }
        }
    }

    private suspend fun updateForegroundStatus(cacheKey: String, status: DownloadStatus) {
        try {
            updateBookmarkStatus(cacheKey, status, isAutoCached = false)
        } catch (error: CancellationException) {
            throw error
        } catch (error: Exception) {
            println("[CollectionBackgroundDomain] failed to persist $status status: ${error.message}")
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

    suspend fun cleanup() {
        lifecycleMutex.lock()
        try {
            val scope = workerScope
            downloadQueue?.cancel()
            downloadQueue = null
            workerScope = null
            scope?.coroutineContext?.get(Job)?.cancelAndJoin()
            inQueue.value = emptySet()
            failedInSession.value = emptySet()
            foregroundAccessCounts.value = emptyMap()
            isCleaningUp.value = false
        } finally {
            lifecycleMutex.unlock()
        }
    }

    companion object {
        private const val CACHE_KEY_PREFIX = "collection-"
        private val HTTPS_REGEX = Regex("^https://")
        private val HTTP_REGEX = Regex("^http://")

        fun cacheKey(ownerId: String, bookmarkId: String): String =
            "$CACHE_KEY_PREFIX$ownerId-$bookmarkId"

        fun isCollectionCacheKey(key: String): Boolean = key.startsWith(CACHE_KEY_PREFIX)
    }
}
