package com.slax.reader.utils

import okio.FileSystem

expect fun platformFileSystem(): FileSystem

expect val dataDirectoryPath: String

expect val cacheDirectoryPath: String