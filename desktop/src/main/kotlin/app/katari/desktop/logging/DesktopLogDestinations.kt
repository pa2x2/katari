package app.katari.desktop.logging

import app.katari.ui.host.LogDestinations
import dev.zacsweers.metro.Inject
import logcat.LogPriority
import logcat.LogcatLogger

/** Standard error, for a terminal or the session's journal, and the log file that "Dump logs" saves. */
@Inject
class DesktopLogDestinations(private val files: DesktopLogFiles) : LogDestinations {
    override fun loggers(minPriority: LogPriority): List<LogcatLogger> =
        listOf(StandardErrorLogger(minPriority), FileLogger(files, minPriority))
}
