package com.slax.reader.data.file

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

// TODO 需要处理Long返回值问题

actual fun getFileManager(): FileManager = AndroidFileManager()

class AndroidFileManager : FileManager {

    override suspend fun write(path: String, data: ByteArray): Long =
        withContext(Dispatchers.IO) {
            try {
                File(path).writeBytes(data)
                0
            } catch (e: Exception) {
                println(e)
                0
            }
        }

    override suspend fun writeStream(
        path: String,
        writer: suspend (OutputStream) -> Unit
    ): Long = withContext(Dispatchers.IO) {
        try {
            val stream = AndroidOutputStream(File(path).outputStream())
            writer(stream)
            stream.close()
            0
        } catch (e: Exception) {
            0
        }
    }

    override suspend fun delete(path: String): Boolean =
        withContext(Dispatchers.IO) {
            File(path).deleteRecursively()
        }

    override suspend fun readStream(
        path: String,
        reader: suspend (InputStream) -> Unit
    ): Long = withContext(Dispatchers.IO) {
        try {
            val stream = AndroidInputStream(File(path).inputStream())
            reader(stream)
            stream.close()
            0
        } catch (e: Exception) {
            0
        }
    }

    override suspend fun clearWebviewCache(): Boolean {
        TODO("Not yet implemented")
    }

    override suspend fun getWebviewCacheSize(): Long {
        TODO("Not yet implemented")
    }

    override suspend fun calculateSize(
        path: String,
        onProgress: (currentSize: Long, filesCount: Int) -> Unit
    ): Long = withContext(Dispatchers.IO) {
        val file = File(path)
        var totalSize = 0L
        var filesCount = 0

        if (file.isFile) {
            totalSize = file.length()
            filesCount = 1
            onProgress(totalSize, filesCount)
        } else if (file.isDirectory) {
            file.walkTopDown().forEach { f ->
                if (f.isFile) {
                    totalSize += f.length()
                    filesCount++
                    onProgress(totalSize, filesCount)
                }
            }
        }

        totalSize
    }
}

class AndroidInputStream(
    private val stream: java.io.InputStream
) : InputStream {
    override suspend fun read(buffer: ByteArray): Int =
        withContext(Dispatchers.IO) {
            stream.read(buffer)
        }

    override suspend fun close() = withContext(Dispatchers.IO) {
        stream.close()
    }
}

class AndroidOutputStream(
    private val stream: java.io.OutputStream
) : OutputStream {
    override suspend fun write(buffer: ByteArray): Long =
        withContext(Dispatchers.IO) {
            stream.write(buffer)
            0
        }

    override suspend fun close() = withContext(Dispatchers.IO) {
        stream.close()
    }
}