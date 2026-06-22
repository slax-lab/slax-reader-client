package com.slax.reader.utils

import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import org.koin.core.context.GlobalContext

actual fun shareContent(title: String, text: String, url: String, imageBytes: ByteArray?) {
    val context = GlobalContext.get().get<Context>()

    val sendIntent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_SUBJECT, title)
        putExtra(Intent.EXTRA_TEXT, text)
    }

    val chooser = Intent.createChooser(sendIntent, title).apply {
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }
    context.startActivity(chooser)
}
