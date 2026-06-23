package com.slax.reader.utils

data class ImageSize(val width: Int, val height: Int)

expect fun decodeImageSize(bytes: ByteArray): ImageSize?
