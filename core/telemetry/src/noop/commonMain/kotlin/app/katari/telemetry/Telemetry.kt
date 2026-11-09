package app.katari.telemetry

import eu.kanade.tachiyomi.core.security.PrivacyPreferences
import kotlinx.coroutines.CoroutineScope

/** The telemetry of builds made without -Pinclude-telemetry, the FOSS build among them: it sends nothing. */
object Telemetry {
    val isIncluded: Boolean = false

    fun start(host: TelemetryHost, preferences: PrivacyPreferences, scope: CoroutineScope) = Unit

    fun setShownScreen(screen: String, entryType: String?, density: Float) = Unit

    fun recordException(throwable: Throwable) = Unit
}
