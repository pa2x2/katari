package mihon.desktop.crash

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import eu.kanade.presentation.crash.CrashScreen
import eu.kanade.tachiyomi.crash.UncaughtCrashHandling
import mihon.desktop.application.AppRestart
import java.io.IOException
import java.nio.file.Files
import java.nio.file.Path

/**
 * The window's content in crash-report mode: only the crash screen for the exception in [report], which offers the
 * logs and a restart into a normal start.
 */
@Composable
fun CrashReportWindowContent(report: Path, appRestart: AppRestart) {
    val exception = remember(report) { UncaughtCrashHandling.decode(readReport(report)) }
    CrashScreen(exception = exception, onRestartClick = appRestart::request)
}

private fun readReport(report: Path): String? = try {
    Files.readString(report)
} catch (_: IOException) {
    null
}
