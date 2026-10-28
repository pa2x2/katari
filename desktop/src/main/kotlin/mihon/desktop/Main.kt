package mihon.desktop

import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
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
import kotlin.system.exitProcess

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
    // Before anything starts AWT, which fixes the window toolkit for the run
    graph.displayBackend.start(preferNativeWayland = graph.displayBackendPreferences.useNativeWayland.get())
    SingletonImageLoader.setSafe { graph.imageLoader }
    // As Android's launch does before its first screen: the default profile exists, older preferences are moved and
    // the profile last open is active again, so the window opens in that profile's theme.
    val startup = runBlocking { graph.profileManager.loadStartupSnapshot() }

    application(exitProcessOnExit = false) {
        val restartRequested by graph.appRestart.isRequested.collectAsState()
        LaunchedEffect(restartRequested) {
            if (restartRequested) exitApplication()
        }
        KatariWindow(graph) {
            HomeWindowContent(startup)
        }
    }
    graph.appRestart.relaunchIfRequested()
    exitProcess(0)
}
