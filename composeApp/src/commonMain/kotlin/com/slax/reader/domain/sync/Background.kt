package com.slax.reader.domain.sync

import app.slax.reader.SlaxConfig
import com.fleeksoft.ksoup.Ksoup
import com.slax.reader.const.AppError
import com.slax.reader.data.database.dao.BookmarkDao
import com.slax.reader.data.database.dao.LocalBookmarkDao
import com.slax.reader.data.database.model.isDownloaded
import com.slax.reader.data.file.FileManager
import com.slax.reader.data.network.ApiService
import com.slax.reader.data.network.MetricsType
import com.slax.reader.data.preferences.AppPreferences
import com.slax.reader.domain.image.ImageDownloadManager
import kotlinx.atomicfu.atomic
import kotlinx.atomicfu.getAndUpdate
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapMerge
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

enum class TaskType {
    DOWNLOAD_METADATA,
    CLEANUP
}

enum class DownloadStatus {
    NONE,
    DOWNLOADING,
    COMPLETED,
    FAILED
}

class TaskItem(
    val bookmarkId: String,
    val updatedAt: String,
    val type: TaskType
)

data class ProcessedContent(val html: String, val imageUrls: List<String>)

private data class PersonalCacheSnapshot(
    val bookmarks: List<com.slax.reader.data.database.model.InboxListBookmarkItem>,
    val localBookmarks: Map<String, com.slax.reader.data.database.model.LocalBookmarkInfo>,
    val cacheCount: Int,
)

class BackgroundDomain(
    private val bookmarkDao: BookmarkDao,
    private val localBookmarkDao: LocalBookmarkDao,
    private val fileManager: FileManager,
    private val apiService: ApiService,
    private val imageDownloadManager: ImageDownloadManager,
    private val appPreferences: AppPreferences,
) {
    private val successStatus = "success"
    private var workerScope: CoroutineScope? = null

    private val maxDownloadConcurrent = 3

    private val inQueue = atomic(setOf<String>())

    private var downloadQueue: Channel<TaskItem>? = null

    private val isCleaningUp = atomic(false)
    private val lifecycleMutex = Mutex()

    @OptIn(ExperimentalCoroutinesApi::class, FlowPreview::class)
    suspend fun startup() = lifecycleMutex.withLock {
        if (workerScope != null) return@withLock

        val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        val queue = Channel<TaskItem>(100)
        workerScope = scope
        downloadQueue = queue

        scope.launch {
            combine(
                bookmarkDao.watchUserBookmarkList(),
                localBookmarkDao.watchUserLocalBookmarkMap(),
                appPreferences.getCacheCount(),
            ) { bookmarks, localBookmarks, cacheCount ->
                PersonalCacheSnapshot(bookmarks, localBookmarks, cacheCount)
            }.collect { snapshot ->
                scheduleSnapshot(snapshot, queue)
            }
        }

        scope.launch {
            queue.receiveAsFlow()
                .flatMapMerge(maxDownloadConcurrent) { task ->
                    flow {
                        processTask(task)
                        emit(Unit)
                    }
                }.collect {
                    println("[BackgroundDomain] Task processing completed")
                }
        }

        scope.launch { apiService.sendMetrics(MetricsType.HEARTBEAT) }
    }

    private suspend fun scheduleSnapshot(snapshot: PersonalCacheSnapshot, queue: Channel<TaskItem>) {
        val cacheLimit = if (snapshot.cacheCount == -1) Int.MAX_VALUE else snapshot.cacheCount.coerceAtLeast(0)
        val currentQueue = inQueue.value
        val cacheWindowIds = mutableSetOf<String>()
        val toDownload = mutableListOf<TaskItem>()
        var windowCount = 0

        for (item in snapshot.bookmarks) {
            if (item.metadataStatus != successStatus) continue
            val local = snapshot.localBookmarks[item.id]
            if (local != null && !local.isAutoCached && local.isDownloaded()) continue
            if (windowCount >= cacheLimit) break

            windowCount++
            cacheWindowIds.add(item.id)
            if (item.id !in currentQueue && local?.isDownloaded() != true) {
                toDownload.add(TaskItem(item.id, item.updatedAt, TaskType.DOWNLOAD_METADATA))
            }
        }

        val activeIds = snapshot.bookmarks.asSequence().map { it.id }.toHashSet()
        val toCleanupIds = snapshot.localBookmarks.mapNotNull { (id, info) ->
            id.takeIf {
                !CollectionBackgroundDomain.isCollectionCacheKey(id) &&
                    id !in cacheWindowIds &&
                    id !in currentQueue &&
                    info.isAutoCached &&
                    info.isDownloaded()
            }
        }

        println("[BackgroundDomain] window=$windowCount, toDownload=${toDownload.size}, toCleanup=${toCleanupIds.size}")

        if (toCleanupIds.isNotEmpty()) cleanupOldCache(toCleanupIds, activeIds)

        for (task in toDownload) {
            val added = inQueue.getAndUpdate { it + task.bookmarkId }.let { task.bookmarkId !in it }
            if (added) queue.send(task)
        }
    }

    suspend fun processTask(task: TaskItem) {
        try {
            when (task.type) {
                TaskType.DOWNLOAD_METADATA -> {
                    downloadBookmarkItem(task)
                }

                TaskType.CLEANUP -> {}
            }
        } catch (error: CancellationException) {
            throw error
        } catch (error: Exception) {
            println("Task failed for bookmark ${task.bookmarkId}: ${error.message}")
            error.printStackTrace()
        }
    }

    suspend fun downloadBookmarkItem(item: TaskItem) {
        val contentPath = "bookmark/${item.bookmarkId}/content.html"

        try {
            updateBookmarkStatus(item.bookmarkId, DownloadStatus.DOWNLOADING)

            val response = apiService.getBookmarkRawContent(item.bookmarkId)
            val content = processContent(response)
            fileManager.writeDataFile(contentPath, content.html.encodeToByteArray())

            val downloadImages = appPreferences.getDownloadImages().first()
            if (downloadImages && content.imageUrls.isNotEmpty()) {
                println("[BackgroundDomain] 开始下载 ${content.imageUrls.size} 张图片")
                coroutineScope {
                    launch {
                        content.imageUrls.forEach { url ->
                            try {
                                imageDownloadManager.ensureCached(url, item.bookmarkId)
                            } catch (error: CancellationException) {
                                throw error
                            } catch (error: Exception) {
                                println("[BackgroundDomain] 图片缓存失败: $url, ${error.message}")
                            }
                        }

                    }
                }
                println("[BackgroundDomain] 图片下载完成")
            }

            updateBookmarkStatus(item.bookmarkId, DownloadStatus.COMPLETED)
        } catch (error: CancellationException) {
            throw error
        } catch (error: Exception) {
            println("下载失败 ${item.bookmarkId}: ${error.message}")
            error.printStackTrace()
            try {
                updateBookmarkStatus(item.bookmarkId, DownloadStatus.FAILED)
            } catch (statusError: CancellationException) {
                throw statusError
            } catch (statusError: Exception) {
                println("[BackgroundDomain] 更新失败状态失败: ${statusError.message}")
            }
        } finally {
            inQueue.getAndUpdate { it - item.bookmarkId }
        }
    }

    private suspend fun cleanupOldCache(ids: List<String>, activeIds: Set<String>) {
        if (!isCleaningUp.compareAndSet(expect = false, update = true)) return
        try {
            val evictedIds = mutableListOf<String>()
            val removedIds = mutableListOf<String>()
            ids.forEach { id ->
                if (id in inQueue.value) return@forEach
                if (!fileManager.deleteDataDirectory("bookmark/$id")) {
                    println("[BackgroundDomain] 删除文件夹失败 $id")
                    return@forEach
                }
                if (id in activeIds) evictedIds.add(id) else removedIds.add(id)
            }
            try {
                localBookmarkDao.batchResetDownloadStatus(
                    bookmarkIds = evictedIds,
                    downloadStatus = 0,
                    isAutoCached = true,
                )
                localBookmarkDao.deleteLocalBookmarkInfo(removedIds)
            } catch (error: CancellationException) {
                throw error
            } catch (error: Exception) {
                println("[BackgroundDomain] 更新缓存状态失败: ${error.message}")
            }
            println("[BackgroundDomain] 批量清理完成，共清理 ${evictedIds.size + removedIds.size} 个")
        } finally {
            isCleaningUp.value = false
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
            isCleaningUp.value = false
        } finally {
            lifecycleMutex.unlock()
        }
    }

    private suspend fun updateBookmarkStatus(id: String, status: DownloadStatus, isAutoCached: Boolean = true) {
        val statusCode = when (status) {
            DownloadStatus.NONE -> 0
            DownloadStatus.DOWNLOADING -> 1
            DownloadStatus.COMPLETED -> 2
            DownloadStatus.FAILED -> 3
        }
        try {
            if (isAutoCached) {
                localBookmarkDao.updateAutoCachedDownloadStatus(id, statusCode)
            } else {
                localBookmarkDao.updateLocalBookmarkDownloadStatus(id, statusCode, isAutoCached = false)
            }
        } catch (error: CancellationException) {
            throw error
        } catch (error: Exception) {
            println("[BackgroundDomain] 更新下载状态到数据库失败: ${error.message}")
            throw error
        }
    }

    private fun processImageUrl(url: String): String {
        return url.replace(HTTPS_REGEX, "slaxstatics://").replace(HTTP_REGEX, "slaxstatic://")
    }

    private fun processContent(html: String): ProcessedContent {
        return try {
            val doc = Ksoup.parse(html)
            doc.outputSettings().prettyPrint(false)
            val urls = mutableListOf<String>()
            doc.select("img").forEach { img ->
                val src = img.attr("src")
                if (src.isNotEmpty() && !src.startsWith("data:")) {
                    val converted = processImageUrl(src)
                    img.attr("src", converted)
                    urls.add(converted)
                }
            }
            ProcessedContent(doc.html(), urls)
        } catch (e: Exception) {
            println("[BackgroundDomain] HTML 处理失败: ${e.message}")
            ProcessedContent(html, emptyList())
        }
    }

    suspend fun getBookmarkContent(id: String): ProcessedContent {
        val bookmarkDir = "bookmark/$id"
        val contentPath = "$bookmarkDir/content.html"

        val existingContent = fileManager.streamDataFile(contentPath)

        if (existingContent != null) {
            val htmlContent = existingContent.decodeToString()
            return processContent(htmlContent)
        }

        inQueue.getAndUpdate { it + id }

        try {
            withContext(Dispatchers.IO) {
                updateBookmarkStatus(id, DownloadStatus.DOWNLOADING, isAutoCached = false)
            }

            val response = apiService.getBookmarkRawContent(id)

            val content = processContent(response)

            try {
                withContext(Dispatchers.IO) {
                    fileManager.writeDataFile(contentPath, content.html.encodeToByteArray())
                    updateBookmarkStatus(id, DownloadStatus.COMPLETED, isAutoCached = false)
                }
            } catch (error: CancellationException) {
                throw error
            } catch (error: Exception) {
                println("缓存写入失败 $id: ${error.message}")
                try {
                    updateBookmarkStatus(id, DownloadStatus.FAILED, isAutoCached = false)
                } catch (statusError: CancellationException) {
                    throw statusError
                } catch (statusError: Exception) {
                    println("[BackgroundDomain] 更新失败状态失败: ${statusError.message}")
                }
            }

            return content
        } catch (error: CancellationException) {
            throw error
        } catch (error: Exception) {
            println("API 调用失败 $id: ${error.message}")
            val errInfo = when (error) {
                is AppError.ApiException.HttpError -> mapOf(
                    "title" to "Error code: ${error.code}",
                    "message" to error.message
                )

                else -> mapOf(
                    "title" to "Network error",
                    "message" to (error.message ?: "Unknown error")
                )
            }
            val errorHtml = SlaxConfig.DETAIL_ERROR_TEMPLATE
                .replace("{{TITLE}}", "<center>Failed to load content</center>")
                .replace("{{REASON}}", "<center>${errInfo["title"]!!}</center>")
                .replace("{{DETAIL}}", "<center>${errInfo["message"]!!}</center>")
            return ProcessedContent(errorHtml, emptyList())
        } finally {
            inQueue.getAndUpdate { it - id }
        }
    }

    companion object {
        private val HTTPS_REGEX = Regex("^https://")
        private val HTTP_REGEX = Regex("^http://")
    }
}
