package app.katari.desktop.logging

import logcat.LogPriority
import logcat.LogcatLogger
import java.io.IOException
import java.io.Writer
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.nio.file.StandardOpenOption
import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter
import kotlin.io.path.exists
import kotlin.io.path.fileSize

/**
 * Appends the app's log to [files], so a user who starts the app from the desktop's menu, with no terminal, has logs
 * to dump. Each line carries the time and the process, as two processes write to the file around a crash. Every line
 * is written through at once, since a crash may end the process before a buffer would. Priorities below [minPriority]
 * are dropped.
 */
class FileLogger(
    private val files: DesktopLogFiles,
    private val minPriority: LogPriority,
) : LogcatLogger {
    private val pid = ProcessHandle.current().pid()
    private var writer: Writer? = null
    private var written = 0L

    override fun isLoggable(priority: LogPriority, tag: String): Boolean =
        priority.priorityInt >= minPriority.priorityInt

    @Synchronized
    override fun log(priority: LogPriority, tag: String, message: String) {
        val line = "${OffsetDateTime.now().format(TIME_FORMAT)} $pid ${priority.name.first()}/$tag: $message\n"
        try {
            val out = writer ?: open()
            out.write(line)
            out.flush()
            written += line.length
            if (written > MAX_FILE_SIZE) {
                out.close()
                writer = null
            }
        } catch (e: IOException) {
            // The log has nowhere else to report this to
            System.err.println("Can't write the log file: $e")
            writer = null
        }
    }

    private fun open(): Writer {
        val current = files.current
        if (current.exists() && current.fileSize() > MAX_FILE_SIZE) {
            Files.move(current, files.previous, StandardCopyOption.REPLACE_EXISTING)
        }
        written = if (current.exists()) current.fileSize() else 0L
        return Files.newBufferedWriter(current, StandardOpenOption.CREATE, StandardOpenOption.APPEND).also {
            writer = it
        }
    }

    private companion object {
        // The log keeps at most two files of this size, the current one and the one before
        const val MAX_FILE_SIZE = 2L * 1024 * 1024

        val TIME_FORMAT: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS xx")
    }
}
