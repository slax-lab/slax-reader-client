package com.slax.reader.data.file

actual fun getFileManager(): FileManager = IOSFileManager()

class IOSFileManager : FileManager {
    override suspend fun calculateSize(path: String, onProgress: (currentSize: Long, filesCount: Int) -> Unit): Long {
        TODO("Not yet implemented")
    }

    override suspend fun clearWebviewCache(): Boolean {
        TODO("Not yet implemented")
    }

    override suspend fun delete(path: String): Boolean {
        TODO("Not yet implemented")
    }

    override suspend fun getWebviewCacheSize(): Long {
        TODO("Not yet implemented")
    }

    override suspend fun readStream(path: String, reader: suspend (InputStream) -> Unit): Long {
        TODO("Not yet implemented")
    }

    override suspend fun write(path: String, data: ByteArray): Long {
        TODO("Not yet implemented")
    }

    override suspend fun writeStream(path: String, writer: suspend (OutputStream) -> Unit): Long {
        TODO("Not yet implemented")
    }
}