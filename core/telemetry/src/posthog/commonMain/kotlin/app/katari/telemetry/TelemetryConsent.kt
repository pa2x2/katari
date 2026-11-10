package app.katari.telemetry

import eu.kanade.tachiyomi.core.security.PrivacyPreferences
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlin.concurrent.Volatile

/**
 * Which of PostHog's events the user lets out: its exception events, from crashes and from failures the app reports,
 * under the crash reports switch, and every other event, the usage statistics, under the analytics switch. PostHog
 * asks as each event is queued, so a switch turned off stops what is not yet queued. Nothing is let out before
 * [follow] has read the switches.
 */
internal class TelemetryConsent {
    @Volatile
    private var crashReports = false

    @Volatile
    var usageStatistics = false
        private set

    val allowsAnything: Boolean get() = crashReports || usageStatistics

    /**
     * Reads the switches and follows them for as long as [scope] lives. [onChange] runs on the calling thread before
     * this returns, then again, from [scope], with each change, possibly to the same values as before.
     */
    fun follow(preferences: PrivacyPreferences, scope: CoroutineScope, onChange: () -> Unit) {
        crashReports = preferences.crashReports.get()
        usageStatistics = preferences.analytics.get()
        onChange()
        combine(preferences.crashReports.changes(), preferences.analytics.changes()) { crashReports, analytics ->
            this.crashReports = crashReports
            usageStatistics = analytics
        }
            .onEach { onChange() }
            .launchIn(scope)
    }

    fun allows(event: String): Boolean = if (event == EXCEPTION_EVENT) crashReports else usageStatistics
}

private const val EXCEPTION_EVENT = "\$exception"
