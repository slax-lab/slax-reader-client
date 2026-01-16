package com.slax.reader.domain.sync

import app.slax.reader.SlaxConfig
import com.slax.reader.const.AppError
import com.slax.reader.data.database.dao.BookmarkDao
import com.slax.reader.data.database.dao.LocalBookmarkDao
import com.slax.reader.data.database.model.InboxListBookmarkItem
import com.slax.reader.data.file.FileManager
import com.slax.reader.data.network.ApiService
import com.slax.reader.utils.parseInstant
import kotlinx.atomicfu.atomic
import kotlinx.atomicfu.getAndUpdate
import kotlinx.atomicfu.update
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.flatMapMerge
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlin.collections.mapOf
import kotlin.time.Clock
import kotlin.time.Duration.Companion.days
import kotlin.time.ExperimentalTime

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

class BackgroundDomain(
    private val bookmarkDao: BookmarkDao,
    private val localBookmarkDao: LocalBookmarkDao,
    private val fileManager: FileManager,
    private val apiService: ApiService,
) {
    private val successStatus = "success"
    private var workerScope: CoroutineScope? = null

    // max size 5G
    private val maximumSize = 5L * 1024 * 1024 * 1024

    // max save cache 30day
    private val longestSaveDuration = 30L * 24 * 60 * 60 * 1000

    // auto download recent 3 days
    private val recentDownloadDuration = 3.days

    // max download concurrent
    private val maxDownloadConcurrent = 3

    private val inQueue = atomic(setOf<String>())

    private var downloadQueue: Channel<TaskItem>? = null

    @OptIn(ExperimentalTime::class)
    private fun shouldDownload(bookmark: InboxListBookmarkItem): Boolean {
        if (bookmark.metadataStatus != successStatus) return false

        val updateAt = parseInstant(bookmark.createdAt)
        val now = Clock.System.now()
        val age = now - updateAt

        if (age > recentDownloadDuration) return false

        return inQueue.getAndUpdate { currentSet ->
            if (bookmark.id in currentSet) currentSet else currentSet + bookmark.id
        }.let { oldSet -> bookmark.id !in oldSet }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    fun startup() {
        workerScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
        downloadQueue = Channel(100)

        workerScope!!.launch {
            bookmarkDao.watchUserBookmarkList().collect { bookmarkList ->
                println("[BackgroundDomain] Collect triggered! Received ${bookmarkList.size} bookmarks")
                val filtered = bookmarkList.filter { shouldDownload(it) }
                println("[BackgroundDomain] Filtered to ${filtered.size} bookmarks for download")

                filtered.forEach { bookmark ->
                    println("[BackgroundDomain] Adding download task for: ${bookmark.id}")
                    val task = TaskItem(
                        bookmarkId = bookmark.id,
                        updatedAt = bookmark.updatedAt,
                        type = TaskType.DOWNLOAD_METADATA
                    )
                    val sent = downloadQueue!!.trySend(task).isSuccess
                    if (!sent) {
                        inQueue.update { currentSet -> currentSet - bookmark.id }
                        println("Download queue full, dropping task for bookmark ${bookmark.id}")
                    }
                }
            }
        }

        workerScope!!.launch {
            downloadQueue!!.receiveAsFlow()
                .flatMapMerge(maxDownloadConcurrent) { task ->
                    flow {
                        processTask(task)
                        emit(Unit)
                    }
                }.collect {
                    println("[BackgroundDomain] Task processing completed")
                }
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
        } catch (e: Exception) {
            println("Task failed for bookmark ${task.bookmarkId}: ${e.message}")
            e.printStackTrace()
        }
    }

    @OptIn(ExperimentalTime::class)
    suspend fun downloadBookmarkItem(item: TaskItem) {
        val bookmarkDir = "bookmark/${item.bookmarkId}"
        val contentInfo = "$bookmarkDir/content.html"

        try {
            val fileInfo = fileManager.getDataFileInfo(contentInfo)
            val needDownload = if (fileInfo == null) {
                // 文件不存在，需要下载
                println("[BackgroundDomain] File doesn't exist, need download")
                true
            } else {
                // 文件存在，比较修改时间
                val fileModifiedTime = fileInfo.lastModifiedAtMillis ?: 0L
                val bookmarkUpdatedTime = parseInstant(item.updatedAt).toEpochMilliseconds()
                // bookmark 更新了,需要重新下载
                val needUpdate = bookmarkUpdatedTime > fileModifiedTime
                println("[BackgroundDomain] File exists, need update: $needUpdate")
                needUpdate
            }

            if (!needDownload) return

            updateBookmarkStatus(item.bookmarkId, DownloadStatus.DOWNLOADING)

            val response = apiService.getBookmarkRawContent(item.bookmarkId)

            fileManager.writeDataFile(contentInfo, response.encodeToByteArray())

            println("[BackgroundDomain] Download successful!")
            updateBookmarkStatus(item.bookmarkId, DownloadStatus.COMPLETED)

        } catch (e: Exception) {
            println("下载失败 ${item.bookmarkId}: ${e.message}")
            e.printStackTrace()
            updateBookmarkStatus(item.bookmarkId, DownloadStatus.FAILED)
        } finally {
            inQueue.getAndUpdate { it - item.bookmarkId }
        }
    }

    fun cleanup() {
        downloadQueue?.cancel()
        downloadQueue = null
        workerScope?.cancel()
        workerScope = null
    }

    private suspend fun updateBookmarkStatus(id: String, status: DownloadStatus) {
        val statusCode = when (status) {
            DownloadStatus.NONE -> 0
            DownloadStatus.DOWNLOADING -> 1
            DownloadStatus.COMPLETED -> 2
            DownloadStatus.FAILED -> 3
        }
        try {
            localBookmarkDao.updateLocalBookmarkDownloadStatus(id, statusCode)
        } catch (e: Exception) {
            println("[BackgroundDomain] 更新下载状态到数据库失败: ${e.message}")
        }
    }

    @OptIn(ExperimentalTime::class)
    suspend fun getBookmarkContent(id: String): String {
        val bookmarkDir = "bookmark/$id"
        val contentPath = "$bookmarkDir/content.html"

        val existingContent = fileManager.streamDataFile(contentPath)

        if (existingContent != null) {
            return existingContent.decodeToString()
        }

        inQueue.getAndUpdate { it + id }

        try {
            withContext(Dispatchers.IO) {
                updateBookmarkStatus(id, DownloadStatus.DOWNLOADING)
            }

            val response = apiService.getBookmarkRawContent(id)
            CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
                try {
                    fileManager.writeDataFile(contentPath, response.encodeToByteArray())
                    updateBookmarkStatus(id, DownloadStatus.COMPLETED)
                } catch (e: Exception) {
                    println("后台写入失败 $id: ${e.message}")
                    updateBookmarkStatus(id, DownloadStatus.FAILED)
                }
            }

            return response
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
            inQueue.getAndUpdate { it - id }
        }
    }
}