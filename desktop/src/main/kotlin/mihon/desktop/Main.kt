package mihon.desktop

import androidx.compose.ui.window.application
import coil3.SingletonImageLoader
import dev.zacsweers.metro.createGraph
import kotlinx.coroutines.runBlocking
import logcat.LogPriority
import logcat.LogcatLogger
import mihon.app.ui.buildinfo.AppBuildType
import mihon.desktop.di.DesktopGraph
import mihon.desktop.home.HomeWindowContent
import mihon.desktop.logging.StandardErrorLogger
import mihon.desktop.window.KatariWindow

fun main() {
    val graph = createGraph<DesktopGraph>()
    // The same levels Android's launch picks.
    val minLogPriority = when {
        graph.networkPreferences.verboseLogging.get() -> LogPriority.VERBOSE
        graph.appBuild.type == AppBuildType.Debug -> LogPriority.DEBUG
        else -> LogPriority.INFO
    }
    LogcatLogger.install()
    LogcatLogger.loggers += StandardErrorLogger(minLogPriority)
    SingletonImageLoader.setSafe { graph.imageLoader }
    // As Android's launch does before its first screen: the default profile exists, older preferences are moved and
    // the profile last open is active again, so the window opens in that profile's theme.
    val startup = runBlocking { graph.profileManager.loadStartupSnapshot() }

    application {
        KatariWindow(graph) {
            HomeWindowContent(startup)
        }
    }
}
