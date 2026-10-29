package mihon.desktop

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.window.application
import coil3.SingletonImageLoader
import dev.zacsweers.metro.createGraphFactory
import kotlinx.coroutines.runBlocking
import mihon.app.ui.host.AppProcessStart
import mihon.desktop.crash.CrashReportArgument
import mihon.desktop.crash.CrashReportWindowContent
import mihon.desktop.crash.RelaunchCrashPresenter
import mihon.desktop.di.DesktopGraph
import mihon.desktop.extension.prepareExtensionHost
import mihon.desktop.extension.startExtensionHost
import mihon.desktop.home.HomeWindowContent
import mihon.desktop.platform.DesktopOperatingSystem
import mihon.desktop.storage.systemDesktopDirectories
import mihon.desktop.window.KatariWindow
import java.nio.file.Path
import java.util.concurrent.Executor
import javax.swing.SwingUtilities
import kotlin.system.exitProcess

fun main(arguments: Array<String>) {
    val directories = systemDesktopDirectories(DesktopOperatingSystem.current())
    val crashReport = CrashReportArgument.reportIn(arguments)
    // As on Android, the crash screen's run installs no crash handling, which would answer a crash of the crash screen
    // by opening it again
    if (crashReport == null) AppProcessStart.installCrashHandling(RelaunchCrashPresenter(directories))
    val graph = createGraphFactory<DesktopGraph.Factory>()
        .create(directories)
        .also(AppProcessStart::registerProfilePreferenceOwners)
    if (crashReport == null) startApp(graph) else startCrashReport(graph, crashReport)
    graph.appRestart.relaunchIfRequested()
    exitProcess(0)
}

private fun startApp(graph: DesktopGraph) {
    AppProcessStart.installLogging(graph)
    AppProcessStart.runAppUpgradeMigrations(graph)
    // Before anything starts AWT, which fixes the window toolkit for the run
    graph.startDisplayBackend()
    SingletonImageLoader.setSafe { graph.imageLoader }
    // As Android's launch does before its first screen: the default profile exists, older preferences are moved and
    // the profile last open is active again, so the window opens in that profile's theme.
    val startup = runBlocking { graph.profileManager.loadStartupSnapshot() }
    graph.startExtensionHost()
    // The warmups build the source manager, which needs the extension host
    AppProcessStart.scheduleWarmups(Executor(SwingUtilities::invokeLater)) { graph }
    runWindow(graph) { HomeWindowContent(startup) }
}

/** Crash-report mode: the window shows only the crash screen, and nothing resumes the app's work. */
private fun startCrashReport(graph: DesktopGraph, report: Path) {
    AppProcessStart.startCrashReport(graph)
    graph.startDisplayBackend()
    // The debug info lists the extensions that misbehave, which loads them once logs are dumped
    graph.prepareExtensionHost()
    runWindow(graph) { CrashReportWindowContent(report, graph.appRestart) }
}

private fun DesktopGraph.startDisplayBackend() {
    displayBackend.start(preferNativeWayland = displayBackendPreferences.useNativeWayland.get())
}

/** Shows [content] in the app's window until the window closes or the app is asked to restart. */
private fun runWindow(graph: DesktopGraph, content: @Composable () -> Unit) {
    application(exitProcessOnExit = false) {
        val restartRequested by graph.appRestart.isRequested.collectAsState()
        LaunchedEffect(restartRequested) {
            if (restartRequested) exitApplication()
        }
        KatariWindow(graph, content)
    }
}
