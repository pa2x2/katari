package mihon.desktop.application

import logcat.LogPriority
import mihon.desktop.crash.CrashReportArgument
import tachiyomi.core.common.util.system.logcat
import java.io.IOException
import java.nio.file.Path

/**
 * Starts the app in a new process the way this one was started, from its AppImage or the build tool alike,
 * without this launch's arguments: the links and files it opened are open already, and crash-report mode's is added
 * where it is wanted.
 */
internal object AppRelaunch {
    /** Starts it in crash-report mode for [crashReport], otherwise as a normal start, even from crash-report mode. */
    fun start(crashReport: Path? = null) {
        val launch = appImageLaunch() ?: thisProcessLaunch() ?: run {
            logcat(LogPriority.ERROR) { "Can't restart: the system doesn't tell this process's command" }
            return
        }
        try {
            ProcessBuilder(launch + listOfNotNull(crashReport?.let(CrashReportArgument::of))).inheritIO().start()
        } catch (e: IOException) {
            logcat(LogPriority.ERROR, e) { "Can't restart" }
        }
    }

    /**
     * The AppImage this process runs from, which its runtime names in `APPIMAGE`. The Java command inside it won't do:
     * its files are mounted only while the AppImage's first process lives, which ends as this one does.
     */
    private fun appImageLaunch(): List<String>? = System.getenv("APPIMAGE")?.takeIf { it.isNotEmpty() }?.let(::listOf)
}
