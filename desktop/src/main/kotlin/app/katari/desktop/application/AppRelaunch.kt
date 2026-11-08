package app.katari.desktop.application

import app.katari.desktop.application.installation.AppInstallation
import app.katari.desktop.crash.CrashReportArgument
import dev.zacsweers.metro.Inject
import logcat.LogPriority
import tachiyomi.core.common.util.system.logcat
import java.io.IOException
import java.nio.file.Path

/**
 * Starts the app in a new process the way its [installation] starts it, without this launch's arguments: the links and
 * files it opened are open already, and crash-report mode's is added where it is wanted.
 */
@Inject
class AppRelaunch(private val installation: AppInstallation) {
    /** Starts it in crash-report mode for [crashReport], otherwise as a normal start, even from crash-report mode. */
    fun start(crashReport: Path? = null) {
        val launch = installation.launch() ?: run {
            logcat(LogPriority.ERROR) { "Can't restart: the system doesn't tell this process's command" }
            return
        }
        try {
            ProcessBuilder(launch + listOfNotNull(crashReport?.let(CrashReportArgument::of))).inheritIO().start()
        } catch (e: IOException) {
            logcat(LogPriority.ERROR, e) { "Can't restart" }
        }
    }
}
