package app.katari.ui.host

import app.katari.core.metro.StartupWarmup
import app.katari.telemetry.Telemetry
import app.katari.telemetry.TelemetryHost
import app.katari.ui.buildinfo.AppBuildType
import app.katari.upgrade.Migrator
import app.katari.upgrade.migrations.migrations
import eu.kanade.tachiyomi.crash.CrashPresenter
import eu.kanade.tachiyomi.crash.UncaughtCrashHandling
import kotlinx.coroutines.launch
import logcat.LogPriority
import logcat.LogcatLogger
import tachiyomi.core.common.preference.Preference
import tachiyomi.core.common.util.system.logcat
import java.util.concurrent.Executor

/**
 * The start of the app's process, in the phases every platform's host runs around its own steps, each in the order its
 * platform needs. A process that only shows the crash screen runs [registerProfilePreferenceOwners] and
 * [startCrashReport] alone.
 */
object AppProcessStart {
    /**
     * Shows the crash screen through [presenter] when an exception goes uncaught. Installed first, so a crash anywhere
     * after is reported; it reads nothing from the graph, which Android builds only after it.
     */
    @HostEntryPoint
    fun installCrashHandling(presenter: CrashPresenter) {
        UncaughtCrashHandling.install(presenter)
    }

    /**
     * Has [executor] build what the first screens need and run every [StartupWarmup], so they are ready by the time a
     * screen asks: Android's main thread, or a thread of the desktop's own. [graph] is only read when the executor runs
     * them.
     */
    @HostEntryPoint
    fun scheduleWarmups(executor: Executor, graph: () -> AppProcessGraph) {
        executor.execute {
            val startGraph = graph()
            startGraph.networkHelper
            startGraph.sourceManager
            startGraph.database
            startGraph.startupWarmups.forEach(StartupWarmup::warmUp)
        }
    }

    /**
     * Starts sending the crash reports and usage statistics the user allows, in builds that carry telemetry. A crash
     * before it goes unreported, so it runs as soon as the graph is built.
     */
    @HostEntryPoint
    fun startTelemetry(graph: AppProcessGraph, host: TelemetryHost) {
        Telemetry.start(host, graph.privacyPreferences, graph.appScope)
    }

    /** Run as the graph is built, before anything else reads from it, since anything may copy or back up a profile. */
    @HostEntryPoint
    fun registerProfilePreferenceOwners(graph: AppProcessGraph) {
        graph.profilePreferenceOwners
    }

    /** Sends the log to the platform's [AppProcessGraph.logDestinations]; once a process, however often it's called. */
    @HostEntryPoint
    fun installLogging(graph: AppProcessGraph) {
        if (LogcatLogger.isInstalled) return
        val minLogPriority = when {
            graph.networkPreferences.verboseLogging.get() -> LogPriority.VERBOSE
            graph.appBuild.type == AppBuildType.Debug -> LogPriority.DEBUG
            else -> LogPriority.INFO
        }
        LogcatLogger.install()
        LogcatLogger.loggers += graph.logDestinations.loggers(minLogPriority)
    }

    /**
     * Starts the migrations from the version the app last ran as to this one, in the background; the home screen waits
     * for them. A first start runs only the migrations that always run, since there is nothing older to migrate.
     */
    @HostEntryPoint
    fun runAppUpgradeMigrations(graph: AppProcessGraph) {
        val versionCode = graph.appBuild.versionCode
        val preference = graph.preferenceStore.getInt(Preference.appStateKey("last_version_code"), 0)
        logcat { "Migration from ${preference.get()} to $versionCode" }
        Migrator.initialize(
            old = preference.get(),
            new = versionCode,
            migrations = migrations,
            graph = graph,
            onMigrationComplete = {
                logcat { "Updating last version to $versionCode" }
                preference.set(versionCode)
            },
        )
    }

    /**
     * Resumes capturing edits for sync once the upgrade migrations have run, so their writes are never edits, and
     * then starts what makes sync run by itself and what tells of it by notification. On a device that doesn't sync,
     * none of them does anything.
     */
    @HostEntryPoint
    fun startSync(graph: AppProcessGraph) {
        Migrator.scope.launch {
            Migrator.await()
            graph.syncCapture.resume()
            graph.syncTriggers.start()
            graph.syncNotices.start()
        }
    }

    /**
     * What a process that only shows the crash screen starts: the log, so a failure of the crash screen is still
     * logged, and nothing that resumes the app's work, which the crashed process may have left half done. It installs
     * no crash handling, which would answer a crash of the crash screen by opening it again.
     */
    @HostEntryPoint
    fun startCrashReport(graph: AppProcessGraph) {
        installLogging(graph)
    }
}
