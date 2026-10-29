package mihon.app.logging

import dev.zacsweers.metro.Inject
import logcat.AndroidLogcatLogger
import logcat.LogPriority
import logcat.LogcatLogger
import mihon.app.ui.host.LogDestinations

/** Android's logcat, which "Dump logs" reads back. */
@Inject
class LogcatLogDestinations : LogDestinations {
    override fun loggers(minPriority: LogPriority): List<LogcatLogger> = listOf(AndroidLogcatLogger(minPriority))
}
