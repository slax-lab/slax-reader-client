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
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapMerge
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.receiveAsFlow

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
    val contentVersion: String,
    val type: TaskType
)

private data class ProcessedContent(val html: String, val imageUrls: List<String>)

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

    @OptIn(ExperimentalCoroutinesApi::class, FlowPreview::class)
    fun startup() {
        if (workerScope?.isActive == true) return
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        val queue = Channel<TaskItem>(100)
        workerScope = scope
        downloadQueue = queue

        scope.launch {
            val localBookmarkState = localBookmarkDao.watchUserLocalBookmarkMap()
            bookmarkDao.watchUserBookmarkList()
                .collect { bookmarkList ->
                    val cacheCountSetting = appPreferences.getCacheCount().first()
                    val recentDownloadCount = if (cacheCountSetting == -1) Int.MAX_VALUE else cacheCountSetting

                    val localMap = localBookmarkState.value
                    val currentQueue = inQueue.value

                    val cacheWindowIds = mutableSetOf<String>()
                    val toDownload = mutableListOf<TaskItem>()
                    var windowCount = 0

                    for (item in bookmarkList) {
                        if (item.metadataStatus != successStatus) continue

                        val local = localMap[item.id]

                        val isDownloaded = local?.isDownloaded() == true
                        val cachedVersion = local?.cachedVersion
                        val isStale = isDownloaded && cachedVersion != null && cachedVersion != item.createdAt

                        // 手动缓存且已是最新版本时跳过，不占缓存窗口
                        if (local != null && !local.isAutoCached && isDownloaded && !isStale) continue

                        if (windowCount >= recentDownloadCount) break
                        windowCount++
                        cacheWindowIds.add(item.id)

                        if (item.id !in currentQueue && (!isDownloaded || isStale)) {
                            toDownload.add(TaskItem(item.id, item.createdAt, TaskType.DOWNLOAD_METADATA))
                        }
                    }

                    val toCleanupIds = mutableListOf<String>()
                    for ((id, info) in localMap) {
                        // 不在缓存窗口内且不在处理队列中的自动缓存才清理，含超出窗口、已归档、已删除的书签
                        if (info.isAutoCached && info.downloadStatus == 2 && id !in cacheWindowIds && id !in currentQueue) {
                            toCleanupIds.add(id)
                        }
                    }

                    println("[BackgroundDomain] window=$windowCount, toDownload=${toDownload.size}, toCleanup=${toCleanupIds.size}")

                    if (toCleanupIds.isNotEmpty()) {
                        cleanupOldCache(toCleanupIds)
                    }

                    for (task in toDownload) {
                        val added = inQueue.getAndUpdate { it + task.bookmarkId }.let { task.bookmarkId !in it }
                        if (added) {
                            queue.send(task)
                        }
                    }
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

    suspend fun processTask(task: TaskItem) {
        try {
            when (task.type) {
                TaskType.DOWNLOAD_METADATA -> {
                    downloadBookmarkItem(task)
                }

                TaskType.CLEANUP -> {}
            }
        } catch (e: Exception) {
            println("Task failed for bookmark ${task.bookmarkId}: ${e.message}")
            e.printStackTrace()
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
                            } catch (e: Exception) {
                                println("[BackgroundDomain] 图片缓存失败: $url, ${e.message}")
                            }
                        }

                    }
                }
                println("[BackgroundDomain] 图片下载完成")
            }

            updateBookmarkStatus(item.bookmarkId, DownloadStatus.COMPLETED, cachedVersion = item.contentVersion)
        } catch (e: Exception) {
            println("下载失败 ${item.bookmarkId}: ${e.message}")
            e.printStackTrace()
            updateBookmarkStatus(item.bookmarkId, DownloadStatus.FAILED)
        } finally {
            inQueue.getAndUpdate { it - item.bookmarkId }
        }
    }

    private suspend fun cleanupOldCache(ids: List<String>) {
        if (!isCleaningUp.compareAndSet(expect = false, update = true)) return
        try {
            ids.forEach { id ->
                try {
                    fileManager.deleteDataDirectory("bookmark/$id")
                } catch (e: Exception) {
                    println("[BackgroundDomain] 删除文件夹失败 $id: ${e.message}")
                }
            }
            try {
                localBookmarkDao.batchResetDownloadStatus(ids)
            } catch (e: Exception) {
                println("[BackgroundDomain] 批量重置状态失败: ${e.message}")
            }
            println("[BackgroundDomain] 批量清理完成，共清理 ${ids.size} 个")
        } finally {
            isCleaningUp.value = false
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
    }

    private suspend fun updateBookmarkStatus(
        id: String,
        status: DownloadStatus,
        isAutoCached: Boolean = true,
        cachedVersion: String? = null,
    ) {
        val statusCode = when (status) {
            DownloadStatus.NONE -> 0
            DownloadStatus.DOWNLOADING -> 1
            DownloadStatus.COMPLETED -> 2
            DownloadStatus.FAILED -> 3
        }
        try {
            localBookmarkDao.updateLocalBookmarkDownloadStatus(id, statusCode, isAutoCached, cachedVersion)
        } catch (e: Exception) {
            println("[BackgroundDomain] 更新下载状态到数据库失败: ${e.message}")
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

    suspend fun getBookmarkContent(id: String): String {
        val bookmarkDir = "bookmark/$id"
        val contentPath = "$bookmarkDir/content.html"

        val existingContent = fileManager.streamDataFile(contentPath)

        if (existingContent != null) {
            val htmlContent = existingContent.decodeToString()
            return processContent(htmlContent).html
        }

        // 一次原子操作决定所有权：返回值为 true 表示本次调用成功抢占队列槽
        // 若后台已持有该 id，则本路径不加入 inQueue，也不在 finally 中移除
        val ownsQueueSlot = inQueue.getAndUpdate { it + id }.let { id !in it }
        // 异步写协程是否已成功启动；若已启动，inQueue 移除由协程的 finally 负责
        var asyncWriteLaunched = false

        try {
            if (ownsQueueSlot) {
                withContext(Dispatchers.IO) {
                    updateBookmarkStatus(id, DownloadStatus.DOWNLOADING, isAutoCached = false)
                }
            }

            val response = apiService.getBookmarkRawContent(id)
            val content = processContent(response)

            if (ownsQueueSlot) {
                val job = if (workerScope?.isActive == true) workerScope?.launch(Dispatchers.IO) {
                    try {
                        fileManager.writeDataFile(contentPath, content.html.encodeToByteArray())
                        val createdAt = bookmarkDao.getBookmarkCreatedAt(id)
                        updateBookmarkStatus(
                            id,
                            DownloadStatus.COMPLETED,
                            isAutoCached = false,
                            cachedVersion = createdAt,
                        )
                    } catch (_: Exception) {
                        updateBookmarkStatus(id, DownloadStatus.FAILED, isAutoCached = false)
                    } finally {
                        // 写入完成（或失败）后才释放队列槽，防止调度器在窗口期重复入队
                        inQueue.getAndUpdate { it - id }
                    }
                } else null
                asyncWriteLaunched = job != null
            }

            return content.html
        } catch (e: Exception) {
            println("API 调用失败 $id: ${e.message}")
            val errInfo = when (e) {
                is AppError.ApiException.HttpError -> mapOf(
                    "title" to "Error code: ${e.code}",
                    "message" to e.message
                )

                else -> mapOf(
                    "title" to "Network error",
                    "message" to (e.message ?: "Unknown error")
                )
            }
            return SlaxConfig.DETAIL_ERROR_TEMPLATE
                .replace("{{TITLE}}", "<center>Failed to load content</center>")
                .replace("{{REASON}}", "<center>${errInfo["title"]!!}</center>")
                .replace("{{DETAIL}}", "<center>${errInfo["message"]!!}</center>")
        } finally {
            // API 报错或 workerScope 为 null 时，异步写协程未启动，需在此兜底移除
            if (ownsQueueSlot && !asyncWriteLaunched) {
                inQueue.getAndUpdate { it - id }
            }
        }
    }

    companion object {
        private val HTTPS_REGEX = Regex("^https://")
        private val HTTP_REGEX = Regex("^http://")
    }
}
