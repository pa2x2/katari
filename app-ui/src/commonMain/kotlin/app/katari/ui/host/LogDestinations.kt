package app.katari.ui.host

import app.katari.core.common.platform.PlatformFacility
import logcat.LogPriority
import logcat.LogcatLogger

/** Where the platform keeps the app's log, such as Android's logcat or a file on desktop. */
@PlatformFacility
interface LogDestinations {
    /** The loggers that write the app's log, each dropping what is below [minPriority]. */
    fun loggers(minPriority: LogPriority): List<LogcatLogger>
}
