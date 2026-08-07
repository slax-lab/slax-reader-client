package com.slax.reader.domain.cache

import com.slax.reader.data.database.dao.LocalBookmarkDao
import com.slax.reader.data.file.FileManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.withContext

enum class CacheCategory {
    ARTICLE,
    IMAGES,
    OTHER,
}

data class CacheUsage(
    val articleBytes: Long = 0L,
    val imageBytes: Long = 0L,
    val otherBytes: Long = 0L,
) {
    val totalBytes: Long
        get() = articleBytes + imageBytes + otherBytes

    fun bytes(category: CacheCategory): Long = when (category) {
        CacheCategory.ARTICLE -> articleBytes
        CacheCategory.IMAGES -> imageBytes
        CacheCategory.OTHER -> otherBytes
    }

    fun bytes(categories: Set<CacheCategory>): Long =
        categories.sumOf { category -> bytes(category) }

    val nonEmptyCategories: Set<CacheCategory>
        get() = CacheCategory.entries.filterTo(mutableSetOf()) { bytes(it) > 0L }
}

class CacheManager(
    private val fileManager: FileManager,
    private val localBookmarkDao: LocalBookmarkDao,
) {
    companion object {
        private const val BOOKMARK_DIR = "bookmark"
    }

    suspend fun clearableCacheUsage(): CacheUsage = withContext(Dispatchers.IO) {
        val ids = runCatching { localBookmarkDao.getManuallyCachedIds() }
            .getOrElse { error ->
                println("[CacheManager] failed to list manual cache: ${error.message}")
                emptyList()
            }
        measureCacheUsage(ids)
    }

    private suspend fun measureCacheUsage(ids: List<String>): CacheUsage {
        var articleBytes = 0L
        var imageBytes = 0L
        ids.forEach { id ->
            articleBytes += fileManager.getDataFileSize("$BOOKMARK_DIR/$id/content.html")
            imageBytes += fileManager.calculateDataDirectorySize("$BOOKMARK_DIR/$id/images").totalBytes
        }

        val otherBytes = runCatching { localBookmarkDao.getCacheOtherBytes() }
            .getOrElse { error ->
                println("[CacheManager] failed to measure other cache: ${error.message}")
                0L
            }

        return CacheUsage(articleBytes, imageBytes, otherBytes)
    }

    suspend fun clearCache(categories: Set<CacheCategory>): Long =
        withContext(NonCancellable + Dispatchers.IO) {
            val selected = categories.intersect(CacheCategory.entries.toSet())
            if (selected.isEmpty()) return@withContext 0L

            val ids = runCatching { localBookmarkDao.getManuallyCachedIds() }
                .getOrElse { error ->
                    println("[CacheManager] failed to list manual cache: ${error.message}")
                    emptyList()
                }
            val before = measureCacheUsage(ids)

            if (CacheCategory.ARTICLE in selected) {
                val clearedArticleIds = ids.filter { id ->
                    fileManager.deleteDataFile("$BOOKMARK_DIR/$id/content.html")
                }
                runCatching { localBookmarkDao.resetManualCacheArticleStatus(clearedArticleIds) }
                    .onFailure { error ->
                        println("[CacheManager] failed to reset article cache state: ${error.message}")
                    }
            }
            if (CacheCategory.IMAGES in selected) {
                ids.forEach { id ->
                    fileManager.deleteDataDirectory("$BOOKMARK_DIR/$id/images")
                }
            }

            if (CacheCategory.OTHER in selected) {
                runCatching { localBookmarkDao.clearCacheOtherFields() }
                    .onFailure { error ->
                        println("[CacheManager] failed to clear other cache fields: ${error.message}")
                    }
            }

            val emptyIds = ids.filter { id ->
                fileManager.getDataFileSize("$BOOKMARK_DIR/$id/content.html") == 0L &&
                    fileManager.calculateDataDirectorySize("$BOOKMARK_DIR/$id/images").totalBytes == 0L
            }
            runCatching { localBookmarkDao.deleteEmptyManualCacheInfo(emptyIds) }
                .onFailure { error ->
                    println("[CacheManager] failed to remove empty cache state: ${error.message}")
                }

            val after = measureCacheUsage(ids)
            (before.bytes(selected) - after.bytes(selected)).coerceAtLeast(0L)
        }
}
