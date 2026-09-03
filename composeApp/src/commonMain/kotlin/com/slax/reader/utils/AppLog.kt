package com.slax.reader.utils

import co.touchlab.kermit.LogWriter
import co.touchlab.kermit.Logger
import co.touchlab.kermit.Severity
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch
import okio.FileSystem
import okio.Path
import okio.Path.Companion.toPath
import okio.SYSTEM
import okio.buffer
import okio.use
import kotlin.time.Clock
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

enum class LogProcess(val filePrefix: String) {
    APP("slax-app"),
    SHARE_EXTENSION("slax-share"),
}

data class AppLogFile(
    val name: String,
    val size: Long,
    val lastModifiedAtMillis: Long?,
)

// Kermit provides the cross-platform logging API; this writer keeps disk I/O off callers.
object AppLog {
    private const val TAG = "SlaxReader"
    private var fileWriter: AsyncFileLogWriter? = null

    fun initialize(process: LogProcess = LogProcess.APP) {
        if (fileWriter != null) return
        val writer = runCatching { AsyncFileLogWriter(process) }.getOrNull() ?: return
        fileWriter = writer
        Logger.setLogWriters(writer)
        i("logger initialized process=${process.name}")
    }

    fun d(message: String) = logSafely { Logger.d(tag = TAG) { redactLogMessage(message) } }
    fun i(message: String) = logSafely { Logger.i(tag = TAG) { redactLogMessage(message) } }
    fun w(message: String, throwable: Throwable? = null) =
        logSafely { Logger.w(throwable = throwable, tag = TAG) { redactLogMessage(message) } }

    fun e(message: String, throwable: Throwable? = null) =
        logSafely { Logger.e(throwable = throwable, tag = TAG) { redactLogMessage(message) } }

    suspend fun flush() {
        fileWriter?.flush()
    }

    fun listFiles(): List<AppLogFile> = LogFiles.list()
    fun readFile(name: String): String = LogFiles.read(name)
    fun shareFile(name: String) {
        runCatching { shareLogFile(LogFiles.path(name).toString()) }
            .onFailure { e("log share failed file=$name error=${it.message}", it) }
    }

    private inline fun logSafely(block: () -> Unit) {
        runCatching(block)
    }
}

private sealed interface LogCommand {
    data class Line(val value: String) : LogCommand
    data class Flush(val done: CompletableDeferred<Unit>) : LogCommand
}

private class AsyncFileLogWriter(process: LogProcess) : LogWriter() {
    private val commands = Channel<LogCommand>(capacity = 2_048)
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val filePrefix = process.filePrefix

    init {
        LogFiles.ensureDirectory()
        scope.launch {
            for (command in commands) {
                when (command) {
                    is LogCommand.Line -> runCatching { append(command.value) }
                    is LogCommand.Flush -> {
                        // Each append closes its buffered sink, so reaching this command means all prior writes are durable.
                        command.done.complete(Unit)
                    }
                }
            }
        }
    }

    @OptIn(ExperimentalTime::class)
    override fun log(severity: Severity, message: String, tag: String, throwable: Throwable?) {
        val throwableText = throwable?.stackTraceToString()?.let { "\n$it" }.orEmpty()
        val line = buildString {
            append(Clock.System.now())
            append(' ')
            append(severity.name.uppercase())
            append(" [")
            append(tag)
            append("] ")
            append(redactLogMessage(message + throwableText))
            append('\n')
        }
        commands.trySend(LogCommand.Line(line))
    }

    suspend fun flush() {
        val done = CompletableDeferred<Unit>()
        if (commands.trySend(LogCommand.Flush(done)).isFailure) {
            // Avoid blocking callers if the bounded queue is saturated.
            return
        }
        done.await()
    }

    private fun append(line: String) {
        val file = currentFile()
        val lineSize = line.encodeToByteArray().size
        val currentSize = LogFiles.fileSystem.metadataOrNull(file)?.size ?: 0L
        if (currentSize + lineSize > MAX_LOG_FILE_BYTES) rotate(file)
        LogFiles.fileSystem.appendingSink(file).buffer().use { sink ->
            sink.writeUtf8(line)
        }
    }

    @OptIn(ExperimentalTime::class)
    private fun currentFile(): Path {
        val utcDate = Clock.System.now().toString().take(10)
        return LogFiles.directory / "$filePrefix-$utcDate.log"
    }

    private fun rotate(file: Path) {
        for (index in MAX_ROTATED_FILES downTo 1) {
            val source = if (index == 1) file else "${file}.${index - 1}".toPath()
            val target = "${file}.$index".toPath()
            if (!LogFiles.fileSystem.exists(source)) continue
            if (LogFiles.fileSystem.exists(target)) LogFiles.fileSystem.delete(target)
            LogFiles.fileSystem.atomicMove(source, target)
        }
    }

    private companion object {
        const val MAX_LOG_FILE_BYTES = 2L * 1_024 * 1_024
        const val MAX_ROTATED_FILES = 3
    }
}

private object LogFiles {
    val fileSystem: FileSystem = FileSystem.SYSTEM
    val directory: Path by lazy { platformLogDirectory().toPath() }

    fun ensureDirectory() {
        fileSystem.createDirectories(directory)
        pruneExpiredFiles()
    }

    fun list(): List<AppLogFile> {
        ensureDirectory()
        return fileSystem.list(directory)
            .mapNotNull { path ->
                val metadata = fileSystem.metadataOrNull(path) ?: return@mapNotNull null
                if (!metadata.isRegularFile || !path.name.startsWith("slax-") || !path.name.contains(".log")) {
                    return@mapNotNull null
                }
                AppLogFile(path.name, metadata.size ?: 0L, metadata.lastModifiedAtMillis)
            }
            .sortedWith(compareByDescending<AppLogFile> { it.lastModifiedAtMillis ?: 0L }.thenBy { it.name })
    }

    fun path(name: String): Path {
        require(name.isNotBlank() && '/' !in name && '\\' !in name) { "Invalid log file name" }
        val path = directory / name
        require(fileSystem.metadataOrNull(path)?.isRegularFile == true) { "Log file not found" }
        return path
    }

    fun read(name: String): String = fileSystem.source(path(name)).buffer().use { it.readUtf8() }

    @OptIn(ExperimentalTime::class)
    private fun pruneExpiredFiles() {
        val nowMillis = Clock.System.now().toEpochMilliseconds()
        fileSystem.list(directory).forEach { path ->
            val metadata = fileSystem.metadataOrNull(path) ?: return@forEach
            if (
                metadata.isRegularFile &&
                path.name.startsWith("slax-") &&
                path.name.contains(".log") &&
                isExpiredLogFile(path.name, metadata.lastModifiedAtMillis, nowMillis)
            ) {
                runCatching { fileSystem.delete(path) }
            }
        }
    }
}

private const val DAY_MILLIS = 24L * 60 * 60 * 1_000
private const val LOG_RETENTION_MILLIS = 7L * DAY_MILLIS
private val DATED_LOG_FILE = Regex("^slax-(?:app|share)-(\\d{4}-\\d{2}-\\d{2})\\.log(?:\\.\\d+)?$")

@OptIn(ExperimentalTime::class)
internal fun isExpiredLogFile(name: String, lastModifiedAtMillis: Long?, nowMillis: Long): Boolean {
    val fileDate = DATED_LOG_FILE.matchEntire(name)?.groupValues?.get(1)
    if (fileDate != null) {
        val today = Instant.fromEpochMilliseconds(nowMillis).toString().take(10)
        val todayStart = Instant.parse("${today}T00:00:00Z").toEpochMilliseconds()
        val fileDateStart = runCatching { Instant.parse("${fileDate}T00:00:00Z").toEpochMilliseconds() }.getOrNull()
        if (fileDateStart != null) return fileDateStart < todayStart - 6L * DAY_MILLIS
    }

    return lastModifiedAtMillis != null && lastModifiedAtMillis < nowMillis - LOG_RETENTION_MILLIS
}

internal fun redactLogMessage(message: String): String {
    return message
        .replace(Regex("(?i)(authorization\\s*[:=]\\s*)(?:bearer\\s+)?([^\\s,;]+)"), "${'$'}1<redacted>")
        .replace(Regex("(?i)(bearer\\s+)[A-Za-z0-9._~+/=-]+"), "${'$'}1<redacted>")
        .replace(Regex("(?i)((?:access_?token|refresh_?token|token)\\s*[:=]\\s*)([^\\s,;&]+)"), "${'$'}1<redacted>")
        .replace(Regex("(?i)(cookie\\s*[:=]\\s*)([^\\r\\n]+)"), "${'$'}1<redacted>")
        .replace(Regex("(?i)(https?://[^\\s?]+)\\?[^\\s]+"), "${'$'}1?<redacted>")
}

expect fun platformLogDirectory(): String
expect fun shareLogFile(path: String)
