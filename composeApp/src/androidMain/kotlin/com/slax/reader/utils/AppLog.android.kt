package com.slax.reader.utils

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import org.koin.core.context.GlobalContext
import java.io.File

actual fun platformLogDirectory(): String {
    val context = GlobalContext.get().get<Context>()
    return File(context.filesDir, "logs").absolutePath
}

actual fun shareLogFile(path: String) {
    val context = GlobalContext.get().get<Context>()
    val file = File(path)
    val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_STREAM, uri)
        putExtra(Intent.EXTRA_SUBJECT, file.name)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    context.startActivity(
        Intent.createChooser(intent, "Share log").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    )
}
