package com.slax.reader.utils

import okio.FileSystem
import platform.Foundation.NSApplicationSupportDirectory
import platform.Foundation.NSCachesDirectory
import platform.Foundation.NSSearchPathForDirectoriesInDomains
import platform.Foundation.NSUserDomainMask

actual fun platformFileSystem(): FileSystem {
    return FileSystem.SYSTEM
}

actual val dataDirectoryPath = NSSearchPathForDirectoriesInDomains(
    NSApplicationSupportDirectory,
    NSUserDomainMask,
    true
).firstOrNull() as? String
    ?: throw IllegalStateException("无法获取 Data 目录")

actual val cacheDirectoryPath = NSSearchPathForDirectoriesInDomains(
    NSCachesDirectory,
    NSUserDomainMask,
    true
).firstOrNull() as? String
    ?: throw IllegalStateException("无法获取 Caches 目录")