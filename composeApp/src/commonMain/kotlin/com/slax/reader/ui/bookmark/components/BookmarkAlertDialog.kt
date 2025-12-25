package com.slax.reader.ui.bookmark.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.slax.reader.utils.i18n

@Composable
fun BookmarkAlertDialog(
    errText: String,
    backClickHandle: (() -> Unit)
) {
    AlertDialog(
        onDismissRequest = { backClickHandle() },
        title = { },
        text = {
            Column {
                Text(text = "load_bookmark_content_error".i18n())
                Spacer(modifier = Modifier.height(8.dp))
                Text(text = errText)
            }
        },
        confirmButton = {
            TextButton(onClick = { backClickHandle() }) {
                Text("btn_back".i18n())
            }
        }
    )
}