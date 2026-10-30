package mihon.desktop

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.window.application
import coil3.SingletonImageLoader
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.runBlocking
import mihon.app.ui.host.AppProcessStart
import mihon.desktop.application.instance.InstanceClaim
import mihon.desktop.application.instance.InstanceRequest
import mihon.desktop.application.instance.SingleInstance
import mihon.desktop.application.instance.systemSingleInstance
import mihon.desktop.crash.CrashReportArgument
import mihon.desktop.crash.CrashReportWindowContent
import mihon.desktop.crash.RelaunchCrashPresenter
import mihon.desktop.di.DesktopGraph
import mihon.desktop.di.createDesktopGraph
import mihon.desktop.extension.prepareExtensionHost
import mihon.desktop.extension.startExtensionHost
import mihon.desktop.home.HomeWindowContent
import mihon.desktop.platform.DesktopOperatingSystem
import mihon.desktop.storage.DesktopDirectories
import mihon.desktop.storage.systemDesktopDirectories
import mihon.desktop.window.KatariWindow
import java.nio.file.Path
import java.util.concurrent.Executor
import javax.swing.SwingUtilities
import kotlin.system.exitProcess

fun main(arguments: Array<String>) {
    val operatingSystem = DesktopOperatingSystem.current()
    val directories = systemDesktopDirectories(operatingSystem)
    val crashReport = CrashReportArgument.reportIn(arguments)
    val graph = if (crashReport == null) {
        // As on Android, the crash screen's run installs no crash handling, which would answer a crash of the crash
        // screen by opening it again
        AppProcessStart.installCrashHandling(RelaunchCrashPresenter(directories))
        val instance = claimInstance(systemSingleInstance(operatingSystem, directories))
        createGraph(directories).also { graph ->
            startApp(graph, instance)
            // Before a restart's launch, which would otherwise hand its request to this ending process
            instance.stopAnswering()
        }
    } else {
        // Claims nothing: the instance that crashed may not have ended when the crash screen opens, and a restart from
        // the crash screen is a normal launch, which claims the instance then
        createGraph(directories).also { graph -> startCrashReport(graph, crashReport) }
    }
    graph.appRestart.relaunchIfRequested()
    exitProcess(0)
}

/**
 * This process's claim to be the app's running instance. A later launch ends here, without building the graph or
 * opening any of the app's files, once the running instance has its request.
 */
private fun claimInstance(singleInstance: SingleInstance): InstanceClaim.Running =
    when (val claim = singleInstance.claim()) {
        is InstanceClaim.Running -> claim
        InstanceClaim.HandedOver -> exitProcess(0)
        InstanceClaim.Unanswered -> {
            // The log starts with the graph, which a launch that ends here never builds
            System.err.println("Katari is already running but didn't answer; not starting a second instance")
            exitProcess(1)
        }
    }

private fun createGraph(directories: DesktopDirectories): DesktopGraph {
    val graph = createDesktopGraph(directories).also(AppProcessStart::registerProfilePreferenceOwners)
    // Before any window composes, so every window, the crash screen's too, is in the app's language
    graph.preferenceAppLocaleStore.applyAtStart()
    return graph
}

private fun startApp(graph: DesktopGraph, instance: InstanceClaim.Running) {
    AppProcessStart.installLogging(graph)
    AppProcessStart.runAppUpgradeMigrations(graph)
    // Before anything starts AWT, which fixes the window toolkit for the run
    graph.startDisplayBackend()
    SingletonImageLoader.setSafe { graph.imageLoader }
    // As Android's launch does before its first screen: the default profile exists, older preferences are moved and
    // the profile last open is active again, so the window opens in that profile's theme.
    val startup = runBlocking { graph.profileManager.loadStartupSnapshot() }
    graph.startExtensionHost()
    // Desktop's counterpart of WorkManager, which Android starts before the app's own start-up: the pending work
    // reads the profile and its sources, so it resumes once both are ready, before the first window opens
    graph.persistedWorkResume.start()
    // The warmups build the source manager, which needs the extension host
    AppProcessStart.scheduleWarmups(Executor(SwingUtilities::invokeLater)) { graph }
    runWindow(graph, laterLaunches = instance.requests) { HomeWindowContent(startup) }
}

/** Crash-report mode: the window shows only the crash screen, and nothing resumes the app's work. */
private fun startCrashReport(graph: DesktopGraph, report: Path) {
    AppProcessStart.startCrashReport(graph)
    graph.startDisplayBackend()
    // The debug info lists the extensions that misbehave, which loads them once logs are dumped
    graph.prepareExtensionHost()
    runWindow(graph, laterLaunches = emptyFlow()) { CrashReportWindowContent(report, graph.appRestart) }
}

private fun DesktopGraph.startDisplayBackend() {
    displayBackend.start(preferNativeWayland = displayBackendPreferences.useNativeWayland.get())
}

/**
 * Shows [content] in the app's window until the window closes or the app is asked to restart, bringing the window
 * forward for each of [laterLaunches].
 */
private fun runWindow(graph: DesktopGraph, laterLaunches: Flow<InstanceRequest>, content: @Composable () -> Unit) {
    application(exitProcessOnExit = false) {
        val restartRequested by graph.appRestart.isRequested.collectAsState()
        LaunchedEffect(restartRequested) {
            if (restartRequested) exitApplication()
        }
        KatariWindow(graph, laterLaunches, content)
    }
}
