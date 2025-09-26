package com.slax.reader.data.file

interface FileManager {
    suspend fun write(path: String, data: ByteArray): Long

    suspend fun writeStream(path: String, writer: suspend (OutputStream) -> Unit): Long

    suspend fun delete(path: String): Boolean

    suspend fun readStream(path: String, reader: suspend (InputStream) -> Unit): Long

    suspend fun calculateSize(
        path: String,
        onProgress: (currentSize: Long, filesCount: Int) -> Unit
    ): Long

    suspend fun getWebviewCacheSize(): Long

    suspend fun clearWebviewCache(): Boolean
}

interface InputStream {
    suspend fun read(buffer: ByteArray): Int
    suspend fun close()
}

interface OutputStream {
    suspend fun write(buffer: ByteArray): Long
    suspend fun close()
}

expect fun getFileManager(): FileManager