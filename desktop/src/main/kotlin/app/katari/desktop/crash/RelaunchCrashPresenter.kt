package app.katari.desktop.crash

import app.katari.desktop.application.AppRelaunch
import app.katari.desktop.storage.DesktopDirectories
import eu.kanade.tachiyomi.crash.CrashPresenter
import logcat.LogPriority
import tachiyomi.core.common.util.system.logcat
import java.nio.file.Files
import kotlin.system.exitProcess

/**
 * Writes the report to the state directory and starts the app again in crash-report mode, whose window shows only the
 * crash screen; a window that crashed may not draw again. Then it ends this process, since nothing else on desktop
 * ends it after an uncaught exception.
 */
class RelaunchCrashPresenter(
    private val directories: DesktopDirectories,
    private val relaunch: AppRelaunch,
) : CrashPresenter {
    override fun present(report: String) {
        try {
            val file = directories.state.resolve(REPORT_FILE_NAME)
            Files.writeString(file, report)
            relaunch.start(crashReport = file)
        } catch (e: Exception) {
            logcat(LogPriority.ERROR, e) { "Can't open the crash screen" }
        } finally {
            exitProcess(EXIT_STATUS)
        }
    }

    private companion object {
        const val REPORT_FILE_NAME = "crash-report.txt"

        const val EXIT_STATUS = 1
    }
}
