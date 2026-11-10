package app.katari.telemetry.usage

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import tachiyomi.core.common.preference.Preference
import tachiyomi.core.common.preference.PreferenceStore
import java.time.LocalDate

/**
 * The "Daily state" event: sent at the first use of each day, with the [AppState] [source] reads then and the screens
 * and window widths seen since the previous one. A day without use sends nothing, so the events also count the days
 * the app was used. What it gathers is kept in [store], so a process ending doesn't lose it, and only while the user
 * allows usage statistics: [forget] drops it when they stop.
 */
internal class DailyStateReport(
    store: PreferenceStore,
    private val source: AppStateSource,
    private val scope: CoroutineScope,
    private val send: (properties: Map<String, Any>) -> Unit,
) {
    private val reportedDay = store.getLong(Preference.appStateKey("telemetry_daily_state_day"), NEVER)
    private val screens = store.getStringSet(Preference.appStateKey("telemetry_screens_visited"))
    private val windowWidths = store.getStringSet(Preference.appStateKey("telemetry_window_widths"))
    private val reporting = Mutex()

    fun noteShown(screen: String, windowWidth: String?) {
        if (screen !in screens.get()) screens.set(screens.get() + screen)
        if (windowWidth != null &&
            windowWidth !in windowWidths.get()
        ) {
            windowWidths.set(windowWidths.get() + windowWidth)
        }
    }

    /** Sends the day's report unless it went out already; cheap enough to call on every use. */
    fun reportIfDue() {
        if (reportedDay.get() == LocalDate.now().toEpochDay()) return
        scope.launch {
            reporting.withLock {
                val day = LocalDate.now().toEpochDay()
                if (reportedDay.get() == day) return@withLock
                val state = source.read()
                val sentScreens = screens.get()
                val sentWidths = windowWidths.get()
                send(properties(state, sentScreens, sentWidths))
                // Less what was sent rather than emptied, keeping what was shown while it went out for the next one
                screens.set(screens.get() - sentScreens)
                windowWidths.set(windowWidths.get() - sentWidths)
                reportedDay.set(day)
            }
        }
    }

    fun forget() {
        screens.delete()
        windowWidths.delete()
        reportedDay.delete()
    }

    companion object {
        const val EVENT = "Daily state"

        fun properties(state: AppState, screens: Set<String>, windowWidths: Set<String>): Map<String, Any> = buildMap {
            state.libraryEntries.forEach { (entryType, count) -> put("library_$entryType", count) }
            state.extensions.forEach { (kind, count) -> put("extensions_${kind.replace('-', '_')}", count) }
            put("profiles", state.profiles)
            put("sync", state.sync.name.lowercase())
            state.translation?.let { put("translation", it) }
            state.readAloud?.let { put("read_aloud", it) }
            state.textRecognition?.let { put("text_recognition", it) }
            put("screens_visited", screens.sorted())
            put("window_widths", windowWidths.sorted())
        }

        private const val NEVER = Long.MIN_VALUE
    }
}
