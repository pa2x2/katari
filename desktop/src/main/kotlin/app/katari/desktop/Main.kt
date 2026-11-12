package app.katari.desktop

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.window.application
import app.katari.core.common.platform.DesktopOperatingSystem
import app.katari.desktop.application.AppRelaunch
import app.katari.desktop.application.DesktopBuildInfo
import app.katari.desktop.application.installation.systemAppInstallation
import app.katari.desktop.application.instance.InstanceClaim
import app.katari.desktop.application.instance.InstanceRequest
import app.katari.desktop.application.instance.SingleInstance
import app.katari.desktop.application.instance.systemSingleInstance
import app.katari.desktop.application.launch.LaunchArguments
import app.katari.desktop.application.launch.LaunchLinks
import app.katari.desktop.application.useCNumberFormat
import app.katari.desktop.crash.CrashReportArgument
import app.katari.desktop.crash.CrashReportWindowContent
import app.katari.desktop.crash.RelaunchCrashPresenter
import app.katari.desktop.di.DesktopGraph
import app.katari.desktop.di.createDesktopGraph
import app.katari.desktop.extension.prepareExtensionHost
import app.katari.desktop.extension.startExtensionHost
import app.katari.desktop.home.HomeWindowContent
import app.katari.desktop.player.Host
import app.katari.desktop.security.signin.SignInCheckProcess
import app.katari.desktop.security.signin.systemSignInCheck
import app.katari.desktop.storage.DesktopDirectories
import app.katari.desktop.storage.systemDesktopDirectories
import app.katari.desktop.update.DesktopAppUpdatePrompt
import app.katari.desktop.window.KatariWindow
import app.katari.desktop.window.WindowThreadFreezes
import app.katari.telemetry.TelemetryHost
import app.katari.ui.host.AppProcessStart
import coil3.SingletonImageLoader
import eu.kanade.presentation.webview.setup.WebViewSetupRequests
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.runBlocking
import java.nio.file.Path
import kotlin.concurrent.thread
import kotlin.system.exitProcess

fun main(arguments: Array<String>) {
    val operatingSystem = DesktopOperatingSystem.current()
    useCNumberFormat(operatingSystem)
    // Before anything of the app's: the check only talks to the system and to the app that started it
    SignInCheckProcess.configurationIn(arguments)?.let { configuration ->
        SignInCheckProcess.run(systemSignInCheck(operatingSystem), configuration)
    }
    val directories = systemDesktopDirectories(operatingSystem)
    val crashReport = CrashReportArgument.reportIn(arguments)
    val graph = if (crashReport == null) {
        // As on Android, the crash screen's run installs no crash handling, which would answer a crash of the crash
        // screen by opening it again
        AppProcessStart.installCrashHandling(
            RelaunchCrashPresenter(directories, AppRelaunch(systemAppInstallation(operatingSystem))),
        )
        val launch = LaunchArguments.read(arguments.asList(), workingDirectory = Path.of("").toAbsolutePath())
        val instance = claimInstance(systemSingleInstance(operatingSystem, directories), launch)
        createGraph(directories).also { graph ->
            startApp(graph, instance, launch)
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
 * opening any of the app's files, once the running instance has its request and [launch]'s links.
 */
private fun claimInstance(singleInstance: SingleInstance, launch: LaunchArguments): InstanceClaim.Running {
    val claim = singleInstance.claim(launch.links)
    if (claim is InstanceClaim.Running) return claim
    // The log starts with the graph, which a launch that ends here never builds
    launch.ignored.forEach { System.err.println("Katari ignores the argument $it") }
    if (claim == InstanceClaim.HandedOver) exitProcess(0)
    System.err.println("Katari is already running but didn't answer; not starting a second instance")
    exitProcess(1)
}

private fun createGraph(directories: DesktopDirectories): DesktopGraph {
    val graph = createDesktopGraph(directories).also(AppProcessStart::registerProfilePreferenceOwners)
    // Before any window composes, so every window, the crash screen's too, is in the app's language
    graph.preferenceAppLocaleStore.applyAtStart()
    return graph
}

private fun startApp(graph: DesktopGraph, instance: InstanceClaim.Running, launch: LaunchArguments) {
    AppProcessStart.installLogging(graph)
    AppProcessStart.startTelemetry(graph, TelemetryHost(DesktopBuildInfo.VERSION_NAME))
    launch.logIgnored()
    AppProcessStart.runAppUpgradeMigrations(graph)
    AppProcessStart.startSync(graph)
    // Before anything starts AWT, which fixes the window toolkit for the run
    graph.displayBackend.start()
    SingletonImageLoader.setSafe { graph.imageLoader }
    // As Android's launch does before its first screen: the default profile exists, older preferences are moved and
    // the profile last open is active again, so the window opens in that profile's theme.
    val startup = runBlocking { graph.profileManager.loadStartupSnapshot() }
    graph.startExtensionHost()
    // As Android takes away a dead process's ongoing notifications: work an earlier run left showing ended with it
    graph.desktopNotifications.resume()
    // Desktop's counterpart of WorkManager, which Android starts before the app's own start-up: the pending work
    // reads the profile and its sources, so it resumes once both are ready, before the first window opens
    graph.persistedWorkResume.start()
    graph.profileRelock.start()
    // The warmups build the source manager, which needs the extension host. On a thread of their own, as the window
    // thread would hold back the window and every frame after it until they are done
    AppProcessStart.scheduleWarmups({ warmups -> thread(name = "Startup warmups") { warmups.run() } }) { graph }
    graph.classDataArchive.start()
    graph.chromiumUpkeep.start()
    graph.appTray.start()
    runWindow(graph, laterLaunches = instance.requests) {
        HomeWindowContent(startup, graph.activeProfileLock, graph.homeDestinations) { navigator ->
            graph.windowAnimePlayer.Host(navigator)
            DesktopAppUpdatePrompt(graph.appInstallation, graph.appUpdateController, graph.onboardingPreferences)
            WebViewSetupRequests(graph.webViewSetup)
        }
        // They wait in the destinations until the home has started, as a later launch's do
        LaunchedEffect(Unit) { LaunchLinks.open(launch.links, graph.homeDestinations) }
        // Once the window is up: the watch posts to the window thread, which would start AWT before the display backend
        LaunchedEffect(Unit) { WindowThreadFreezes.watch() }
    }
    graph.classDataArchive.stop()
    graph.appTray.close()
    // The work they show ends with the app
    graph.desktopNotifications.close()
    // Before a restart's launch, whose web view would find Chromium's profile still in use
    graph.jcefRuntime.shutdown()
}

/** Crash-report mode: the window shows only the crash screen, and nothing resumes the app's work. */
private fun startCrashReport(graph: DesktopGraph, report: Path) {
    AppProcessStart.startCrashReport(graph)
    graph.displayBackend.start()
    // The debug info lists the extensions that misbehave, which loads them once logs are dumped
    graph.prepareExtensionHost()
    runWindow(graph, laterLaunches = emptyFlow()) { CrashReportWindowContent(report, graph.appRestart) }
}

/**
 * Shows [content] in the app's window until the window closes or the app is asked to restart, bringing the window
 * forward and opening what they open for each of [laterLaunches].
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
