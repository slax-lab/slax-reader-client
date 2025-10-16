package com.slax.reader.data.file

import com.slax.reader.utils.cacheDirectoryPath
import com.slax.reader.utils.dataDirectoryPath
import okio.FileSystem
import okio.Path
import okio.Path.Companion.toPath

data class FileInfo(
    val name: String,
    val path: String,
    val isDirectory: Boolean,
    val size: Long
)

data class DirectorySize(
    val totalBytes: Long,
    val fileCount: Int,
    val dirCount: Int,
    val formatted: String
)

class FileManager(val fileSystem: FileSystem) {

    init {
        mkdir("$dataDirectoryPath/bookmark")
        mkdir("$dataDirectoryPath/image")
        mkdir("$dataDirectoryPath/other")
    }

    fun deleteDataFile(fileName: String): Boolean {
        return try {
            val path = "$dataDirectoryPath/$fileName".toPath()
            fileSystem.delete(path)
            true
        } catch (e: Exception) {
            println("删除文件失败: ${e.message}")
            false
        }
    }

    fun streamDataFile(fileName: String): ByteArray? {
        return try {
            val path = "$dataDirectoryPath/$fileName".toPath()
            fileSystem.read(path) {
                readByteArray()
            }
        } catch (e: Exception) {
            println("读取文件失败: ${e.message}")
            null
        }
    }
    
    fun writeDataFile(fileName: String, data: ByteArray) {
        val path = "$dataDirectoryPath/$fileName".toPath()
        fileSystem.write(path) {
            write(data)
        }
    }

    fun mkdir(dirPath: String) {
        val path = dirPath.toPath()
        fileSystem.createDirectories(path, false)
    }

    fun scanDirectory(dirPath: String): List<FileInfo> {
        val result = mutableListOf<FileInfo>()

        fun scan(path: Path) {
            try {
                fileSystem.list(path).forEach { childPath ->
                    val metadata = fileSystem.metadataOrNull(childPath)
                    val isDir = metadata?.isDirectory ?: false

                    result.add(
                        FileInfo(
                            name = childPath.name,
                            path = childPath.toString(),
                            isDirectory = isDir,
                            size = metadata?.size ?: 0L
                        )
                    )

                    if (isDir) scan(childPath)
                }
            } catch (e: Exception) {
                println("扫描失败: ${e.message}")
            }
        }

        scan(dirPath.toPath())
        return result
    }

    fun calculateDirectorySize(dirPath: String): DirectorySize {
        var totalSize = 0L
        var fileCount = 0
        var dirCount = 0

        fun calculate(path: Path) {
            try {
                fileSystem.list(path).forEach { childPath ->
                    val metadata = fileSystem.metadataOrNull(childPath)
                    if (metadata != null) {
                        if (metadata.isDirectory) {
                            dirCount++
                            calculate(childPath)
                        } else {
                            fileCount++
                            totalSize += metadata.size ?: 0L
                        }
                    }
                }
            } catch (e: Exception) {
                println("计算大小失败: ${e.message}")
            }
        }

        calculate(dirPath.toPath())

        return DirectorySize(
            totalBytes = totalSize,
            fileCount = fileCount,
            dirCount = dirCount,
            formatted = formatBytes(totalSize)
        )
    }

    fun clearCache(): Boolean {
        return try {
            val cachePath = cacheDirectoryPath.toPath()
            fileSystem.list(cachePath).forEach { child ->
                deleteRecursively(child)
            }
            true
        } catch (e: Exception) {
            println("清空缓存失败: ${e.message}")
            false
        }
    }

    private fun deleteRecursively(path: Path) {
        val metadata = fileSystem.metadataOrNull(path)
        if (metadata?.isDirectory == true) {
            fileSystem.list(path).forEach { deleteRecursively(it) }
        }
        fileSystem.delete(path)
    }

    private fun formatBytes(bytes: Long): String {
        return when {
            bytes >= 1024L * 1024 * 1024 ->
                "${bytes / (1024.0 * 1024 * 1024)} GB"

            bytes >= 1024L * 1024 ->
                "${bytes / (1024.0 * 1024)} MB"

            bytes >= 1024L ->
                "${bytes / 1024.0} KB"

            else -> "$bytes B"
        }
    }
}