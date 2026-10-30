package eu.kanade.tachiyomi.crash

import mihon.core.common.platform.PlatformFacility

/**
 * Opens the crash screen for a crash the app can't go on from, in a process of its own that outlives the crashing one.
 * The crashing process must not outlive the crash: on Android the handler that runs after the presenter ends it, and a
 * platform that has no such handler ends it in the presenter.
 */
@PlatformFacility
fun interface CrashPresenter {
    /** Opens the crash screen for [report], the exception as [UncaughtCrashHandling.encode] wrote it. */
    fun present(report: String)
}
