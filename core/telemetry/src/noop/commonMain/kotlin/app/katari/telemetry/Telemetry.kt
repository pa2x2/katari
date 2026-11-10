package app.katari.telemetry

import app.katari.telemetry.usage.AppStateSource
import app.katari.telemetry.usage.ReadingSession
import eu.kanade.tachiyomi.core.security.PrivacyPreferences
import kotlinx.coroutines.CoroutineScope
import tachiyomi.core.common.preference.PreferenceStore

/** The telemetry of builds made without -Pinclude-telemetry, the FOSS build among them: it sends nothing. */
object Telemetry {
    val isIncluded: Boolean = false

    fun start(
        host: TelemetryHost,
        preferences: PrivacyPreferences,
        store: PreferenceStore,
        appState: AppStateSource,
        scope: CoroutineScope,
    ) = Unit

    fun setShownScreen(screen: String, entryType: String?, density: Float, windowWidth: String) = Unit

    fun reportReadingSession(session: ReadingSession) = Unit

    fun recordException(throwable: Throwable) = Unit
}
