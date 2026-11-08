package app.katari.android.logging

import app.katari.ui.host.LogDestinations
import dev.zacsweers.metro.Inject
import logcat.AndroidLogcatLogger
import logcat.LogPriority
import logcat.LogcatLogger

/** Android's logcat, which "Dump logs" reads back. */
@Inject
class LogcatLogDestinations : LogDestinations {
    override fun loggers(minPriority: LogPriority): List<LogcatLogger> = listOf(AndroidLogcatLogger(minPriority))
}
