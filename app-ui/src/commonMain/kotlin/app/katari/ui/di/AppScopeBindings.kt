package app.katari.ui.di

import app.katari.core.metro.AppCoroutineScope
import app.katari.ui.buildinfo.AppBuild
import app.katari.ui.buildinfo.AppBuildType
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.BindingContainer
import dev.zacsweers.metro.ContributesTo
import dev.zacsweers.metro.Multibinds
import dev.zacsweers.metro.Provides
import dev.zacsweers.metro.SingleIn
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import logcat.LogPriority
import tachiyomi.core.common.util.system.logcat

@ContributesTo(AppScope::class)
@BindingContainer
interface AppScopeBindings {
    @Multibinds(allowEmpty = true)
    fun uncaughtErrorReporters(): Set<UncaughtErrorReporter>

    companion object {
        @Provides
        @SingleIn(AppScope::class)
        @AppCoroutineScope
        fun appCoroutineScope(build: AppBuild, reporters: Set<UncaughtErrorReporter>): CoroutineScope {
            // Work here outlives the screen that started it, so a failure would crash the app on whatever screen is
            // open by then. Debug builds still crash so the bug gets noticed.
            val handler = CoroutineExceptionHandler { _, throwable ->
                if (build.type == AppBuildType.Debug) {
                    val thread = Thread.currentThread()
                    thread.uncaughtExceptionHandler?.uncaughtException(thread, throwable)
                } else {
                    logcat(LogPriority.ERROR, throwable) { "Uncaught exception in the app scope" }
                    reporters.forEach { it.report(throwable) }
                }
            }
            return CoroutineScope(SupervisorJob() + Dispatchers.IO + handler)
        }
    }
}
