package app.katari.telemetry

import app.katari.telemetry.usage.AppStateSource
import app.katari.telemetry.usage.DailyStateReport
import app.katari.telemetry.usage.ReadingSession
import app.katari.telemetry.usage.ReadingSessionEvent
import eu.kanade.tachiyomi.core.security.PrivacyPreferences
import kotlinx.coroutines.CoroutineScope
import tachiyomi.core.common.preference.PreferenceStore
import kotlin.concurrent.Volatile

/**
 * Crash reports and usage statistics, sent to PostHog. Builds made without -Pinclude-telemetry have a Telemetry of the
 * same shape that sends nothing.
 *
 * PostHog asks its servers for the project's settings each time it is set up, opted out or not, so it is set up only
 * once the user allows something to be sent. After that, opting out stops what it sends and the handlers it installs.
 */
object Telemetry {
    /**
     * Whether the build sends anything, and so whether the app offers the switches for it: only a build given the
     * project's token does, which the release workflow alone passes. Any other, even of the release variant, would mix
     * its testing into the releases' data.
     */
    val isIncluded: Boolean = PostHogProjectToken.value != null

    private val consent = TelemetryConsent()

    // Kept here rather than registered with PostHog, which stores what is registered and would carry the screen of an
    // earlier run into the next one's first events
    @Volatile
    private var shownScreen: ShownScreen? = null

    // Set while the user allows usage statistics, the only time what the app shows is kept for it
    @Volatile
    private var dailyState: DailyStateReport? = null

    // Written on the thread that calls start, then only by the consent's collector, which start launches after it
    private var sending = false

    @Volatile
    private var postHogSetUp = false

    /**
     * Starts sending what the user's choices in [preferences] allow, following them for as long as [scope] lives.
     * [store] keeps what the day's report gathers, and [appState] reads the rest of it.
     */
    fun start(
        host: TelemetryHost,
        preferences: PrivacyPreferences,
        store: PreferenceStore,
        appState: AppStateSource,
        scope: CoroutineScope,
    ) {
        val token = PostHogProjectToken.value ?: return
        val report =
            DailyStateReport(store, appState, scope, send = { PostHogClient.capture(DailyStateReport.EVENT, it) })
        consent.follow(preferences, scope) {
            send(consent.allowsAnything, token, host, scope)
            if (consent.usageStatistics) {
                dailyState = report
                report.reportIfDue()
            } else {
                dailyState = null
                report.forget()
            }
        }
    }

    /**
     * Names what the app shows from now on; see [ShownScreen]. [windowWidth] is the window's size class, which only the
     * day's report carries.
     */
    fun setShownScreen(screen: String, entryType: String?, density: Float, windowWidth: String) {
        shownScreen = ShownScreen(screen, entryType, density)
        noteShown(screen, windowWidth)
    }

    fun reportReadingSession(session: ReadingSession) {
        if (!postHogSetUp) return
        PostHogClient.capture(ReadingSessionEvent.NAME, ReadingSessionEvent.properties(session))
    }

    /** Reports a failure the app caught and survived, under the same switch as crashes. */
    fun recordException(throwable: Throwable) {
        PostHogClient.captureException(throwable)
    }

    private fun noteShown(screen: String, windowWidth: String?) {
        dailyState?.run {
            noteShown(screen, windowWidth)
            reportIfDue()
        }
    }

    private fun send(allowsAnything: Boolean, token: String, host: TelemetryHost, scope: CoroutineScope) {
        if (allowsAnything == sending) return
        sending = allowsAnything
        when {
            !allowsAnything -> PostHogClient.optOut()

            postHogSetUp -> PostHogClient.optIn()

            else -> {
                PostHogClient.setUp(token, host, ::addedProperties, onActivityResumed = { noteShown(it, null) }, scope)
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
     * Sets PostHog up opted in, sending to the project [token] lets events into. Each event goes out with the
     * properties [addedProperties] gives it added to its own, and not at all where it gives none. [onActivityResumed]
     * hears the name of each activity that comes to the front, on a platform that has activities.
     */
    fun setUp(
        token: String,
        host: TelemetryHost,
        addedProperties: (event: String) -> Map<String, Any>?,
        onActivityResumed: (name: String) -> Unit,
        scope: CoroutineScope,
    )

    fun optIn()

    fun optOut()

    fun capture(event: String, properties: Map<String, Any>)

    fun captureException(throwable: Throwable)
}
