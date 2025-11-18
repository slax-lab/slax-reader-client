package com.slax.reader.utils

import android.content.Context
import okio.FileSystem
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

actual fun platformFileSystem(): FileSystem {
    return FileSystem.SYSTEM
}

actual val cacheDirectoryPath: String
    get() = object : KoinComponent {
        val context: Context by inject()
    }.context.cacheDir.absolutePath

actual val dataDirectoryPath: String
    get() = object : KoinComponent {
        val context: Context by inject()
    }.context.dataDir.absolutePath