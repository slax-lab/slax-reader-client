package com.slax.reader.ui.sidebar.compenents

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.DefaultStrokeLineCap
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.slax.reader.domain.coordinator.AppSyncState
import com.slax.reader.utils.i18n

@Composable
fun SyncStatusBar(state: AppSyncState) {
    println("[watch][UI] SyncStatusBar recomposed")

    val syncStateText = when (state) {
        is AppSyncState.Uploading -> "sync_uploading".i18n()
        is AppSyncState.Downloading -> "sync_downloading".i18n()
        is AppSyncState.NoNetwork -> "sync_no_network".i18n()
        is AppSyncState.Connecting -> "sync_connecting".i18n()
        is AppSyncState.Error -> state.message
        else -> ""
    }

    Surface(
        modifier = Modifier
            .fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        color = Color(0xFFE8EBED),
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            horizontalAlignment = Alignment.Start
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                val style = TextStyle(
                    fontSize = 11.sp,
                    lineHeight = 15.sp,
                    color = Color(0xFF333333)
                )

                Text(
                    text = syncStateText,
                    style = style
                )

                // 只在下载时显示具体进度百分比
                if (state is AppSyncState.Downloading && state.progress != null) {
                    Text(
                        text = "${(state.progress * 100).toInt()}%",
                        style = style
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            if (state is AppSyncState.Downloading && state.progress != null) {
                LinearProgressIndicator(
                    strokeCap = DefaultStrokeLineCap,
                    progress = { state.progress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(5.dp)
                        .clip(RoundedCornerShape(2.5.dp)),
                    color = Color(0xFF16B998),
                    trackColor = Color(0x140F1419)
                )
            }
            if (state is AppSyncState.Connecting || state is AppSyncState.Uploading || state is AppSyncState.Connecting) {
                LinearProgressIndicator(
                    strokeCap = DefaultStrokeLineCap,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(5.dp)
                        .clip(RoundedCornerShape(2.5.dp)),
                    color = Color(0xFF16B998),
                    trackColor = Color(0x140F1419)
                )
            }
        }
    }
}