package mihon.desktop.logging

import logcat.LogPriority
import logcat.LogcatLogger

/**
 * Writes the app's log to standard error, where a terminal or the session's journal picks it up. Priorities below
 * [minPriority] are dropped, as Android's logger drops them.
 */
class StandardErrorLogger(private val minPriority: LogPriority) : LogcatLogger {
    override fun isLoggable(priority: LogPriority, tag: String): Boolean =
        priority.priorityInt >= minPriority.priorityInt

    override fun log(priority: LogPriority, tag: String, message: String) {
        System.err.println("${priority.name.first()}/$tag: $message")
    }
}
