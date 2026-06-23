package com.slax.reader.utils

import android.graphics.BitmapFactory

actual fun decodeImageSize(bytes: ByteArray): ImageSize? {
    val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    BitmapFactory.decodeByteArray(bytes, 0, bytes.size, options)
    if (options.outWidth <= 0 || options.outHeight <= 0) return null
    return ImageSize(options.outWidth, options.outHeight)
}
