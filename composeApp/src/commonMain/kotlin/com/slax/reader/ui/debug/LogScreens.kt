package com.slax.reader.ui.debug

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.slax.reader.utils.AppLog
import com.slax.reader.utils.AppLogFile
import com.slax.reader.utils.i18n
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.withContext
import org.jetbrains.compose.resources.painterResource
import slax_reader_client.composeapp.generated.resources.Res
import slax_reader_client.composeapp.generated.resources.ic_sm_back
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LogListScreen(
    onBackClick: () -> Unit,
    onFileClick: (String) -> Unit,
) {
    var files by remember { mutableStateOf<List<AppLogFile>>(emptyList()) }
    LaunchedEffect(Unit) {
        AppLog.flush()
        files = withContext(Dispatchers.IO) { runCatching { AppLog.listFiles() }.getOrElse { emptyList() } }
    }

    DebugScaffold(title = "Logs", onBackClick = onBackClick) { modifier ->
        LazyColumn(
            modifier = modifier.fillMaxSize().padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item { Spacer(Modifier.height(4.dp)) }
            if (files.isEmpty()) {
                item { Text("No log files", color = Color(0xFF666666)) }
            }
            items(files, key = { it.name }) { file ->
                SectionCard(title = file.name) {
                    Text(
                        text = "${formatCacheSize(file.size)}  ${formatLogTime(file.lastModifiedAtMillis)}",
                        color = Color(0xFF666666),
                        fontFamily = FontFamily.Monospace,
                        fontSize = 12.sp,
                    )
                    Spacer(Modifier.height(10.dp))
                    Button(
                        onClick = { onFileClick(file.name) },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0F1419)),
                    ) { Text("Open") }
                }
            }
            item { Spacer(Modifier.height(16.dp)) }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LogDetailScreen(fileName: String, onBackClick: () -> Unit) {
    var content by remember(fileName) { mutableStateOf("Loading...") }
    LaunchedEffect(fileName) {
        AppLog.flush()
        content = withContext(Dispatchers.IO) {
            runCatching { AppLog.readFile(fileName) }.getOrElse { "Unable to read log: ${it.message}" }
        }
    }

    DebugScaffold(title = fileName, onBackClick = onBackClick) { modifier ->
        Column(modifier = modifier.fillMaxSize().padding(16.dp)) {
            Button(
                onClick = { AppLog.shareFile(fileName) },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0F1419)),
            ) { Text("Share") }
            Spacer(Modifier.height(12.dp))
            Text(
                text = content,
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .horizontalScroll(rememberScrollState()),
                fontFamily = FontFamily.Monospace,
                fontSize = 11.sp,
                color = Color(0xFF0F1419),
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DebugScaffold(
    title: String,
    onBackClick: () -> Unit,
    content: @Composable (Modifier) -> Unit,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(title) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            painter = painterResource(Res.drawable.ic_sm_back),
                            contentDescription = "btn_back".i18n(),
                            tint = Color.Unspecified,
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFFF5F5F3)),
            )
        },
        containerColor = Color(0xFFF5F5F3),
    ) { padding -> content(Modifier.padding(padding)) }
}

@OptIn(ExperimentalTime::class)
private fun formatLogTime(epochMillis: Long?): String {
    return epochMillis?.let { Instant.fromEpochMilliseconds(it).toString() } ?: "Unknown time"
}
