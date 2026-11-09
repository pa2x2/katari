package app.katari.telemetry

import eu.kanade.tachiyomi.core.security.PrivacyPreferences
import kotlinx.coroutines.CoroutineScope
import kotlin.concurrent.Volatile

/**
 * Crash reports and usage statistics, sent to PostHog. Builds made without -Pinclude-telemetry have a Telemetry of the
 * same shape that sends nothing.
 *
 * PostHog asks its servers for the project's settings each time it is set up, opted out or not, so it is set up only
 * once the user allows something to be sent. After that, opting out stops what it sends and the handlers it installs.
 */
object Telemetry {
    /** Whether the build sends anything, and so whether the app offers the switches for it. */
    val isIncluded: Boolean = true

    private val consent = TelemetryConsent()

    // Kept here rather than registered with PostHog, which stores what is registered and would carry the screen of an
    // earlier run into the next one's first events
    @Volatile
    private var shownScreen: ShownScreen? = null

    // Written on the thread that calls start, then only by the consent's collector, which start launches after it
    private var sending = false
    private var postHogSetUp = false

    /** Starts sending what the user's choices in [preferences] allow, following them for as long as [scope] lives. */
    fun start(host: TelemetryHost, preferences: PrivacyPreferences, scope: CoroutineScope) {
        if (!host.isKatariRelease) return
        consent.follow(preferences, scope) { allowsAnything -> send(allowsAnything, host, scope) }
    }

    /** Names what the app shows from now on; see [ShownScreen]. */
    fun setShownScreen(screen: String, entryType: String?, density: Float) {
        shownScreen = ShownScreen(screen, entryType, density)
    }

    /** Reports a failure the app caught and survived, under the same switch as crashes. */
    fun recordException(throwable: Throwable) {
        PostHogClient.captureException(throwable)
    }

    private fun send(allowsAnything: Boolean, host: TelemetryHost, scope: CoroutineScope) {
        if (allowsAnything == sending) return
        sending = allowsAnything
        when {
            !allowsAnything -> PostHogClient.optOut()

            postHogSetUp -> PostHogClient.optIn()

            else -> {
                PostHogClient.setUp(host, ::addedProperties, scope)
                postHogSetUp = true
            }
        }
    }

    /** What [event] carries besides PostHog's own properties; null where the user's switches keep it in. */
    private fun addedProperties(event: String): Map<String, Any>? {
        if (!consent.allows(event)) return null
        return shownScreen?.properties.orEmpty()
    }
}

/** PostHog's library on each platform: Android has its own, the desktop app the multiplatform one. */
internal expect object PostHogClient {
    /**
     * Sets PostHog up opted in. Each event goes out with the properties [addedProperties] gives it added to its own, and
     * not at all where it gives none.
     */
    fun setUp(host: TelemetryHost, addedProperties: (event: String) -> Map<String, Any>?, scope: CoroutineScope)

    fun optIn()

    fun optOut()

    fun captureException(throwable: Throwable)
}
