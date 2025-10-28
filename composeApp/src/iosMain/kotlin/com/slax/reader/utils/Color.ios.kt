package com.slax.reader.utils

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import platform.UIKit.UIColor

fun Color.toUIColor(): UIColor {
    val argb = this.toArgb()
    return UIColor(
        red = ((argb shr 16) and 0xFF) / 255.0,
        green = ((argb shr 8) and 0xFF) / 255.0,
        blue = (argb and 0xFF) / 255.0,
        alpha = ((argb shr 24) and 0xFF) / 255.0
    )
}
