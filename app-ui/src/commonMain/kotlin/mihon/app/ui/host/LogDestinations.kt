package mihon.app.ui.host

import logcat.LogPriority
import logcat.LogcatLogger

/** Where the platform keeps the app's log, such as Android's logcat or a file on desktop. */
interface LogDestinations {
    /** The loggers that write the app's log, each dropping what is below [minPriority]. */
    fun loggers(minPriority: LogPriority): List<LogcatLogger>
}
