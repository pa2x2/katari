package app.katari.telemetry

import eu.kanade.tachiyomi.core.security.PrivacyPreferences
import kotlinx.coroutines.CoroutineScope

/**
 * Crash reports and usage statistics, sent to PostHog. Builds made without -Pinclude-telemetry have a Telemetry of the
 * same shape that sends nothing.
 */
object Telemetry {
    /** Whether the build sends anything, and so whether the app offers the switches for it. */
    val isIncluded: Boolean = true

    private val consent = TelemetryConsent()

    /** Starts sending what the user's choices in [preferences] allow, following them for as long as [scope] lives. */
    fun start(host: TelemetryHost, preferences: PrivacyPreferences, scope: CoroutineScope) {
        consent.follow(preferences, scope)
        PostHogClient.start(host, consent, scope)
    }

    /** Reports a failure the app caught and survived, under the same switch as crashes. */
    fun recordException(throwable: Throwable) {
        PostHogClient.captureException(throwable)
    }
}

/** PostHog's library on each platform: Android has its own, the desktop app the multiplatform one. */
internal expect object PostHogClient {
    fun start(host: TelemetryHost, consent: TelemetryConsent, scope: CoroutineScope)

    fun captureException(throwable: Throwable)
}
