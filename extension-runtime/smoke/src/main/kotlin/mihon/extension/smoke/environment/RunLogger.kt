package mihon.extension.smoke.environment

import logcat.LogPriority
import logcat.LogcatLogger
import java.io.PrintWriter
import java.nio.file.Files
import java.nio.file.Path
import java.time.LocalTime

/**
 * The host's and the extensions' log, written to [file] rather than the terminal, where the run's progress goes and
 * hundreds of extensions' stack traces would bury it.
 */
internal class RunLogger(file: Path, private val minPriority: LogPriority) : LogcatLogger {
    private val writer = PrintWriter(Files.newBufferedWriter(file), true)

    override fun isLoggable(priority: LogPriority, tag: String): Boolean =
        priority.priorityInt >= minPriority.priorityInt

    override fun log(priority: LogPriority, tag: String, message: String) {
        synchronized(writer) {
            writer.println("${LocalTime.now()} ${priority.name.first()}/$tag: $message")
        }
    }
}
