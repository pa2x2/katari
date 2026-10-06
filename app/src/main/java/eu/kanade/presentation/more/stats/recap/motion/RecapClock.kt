package eu.kanade.presentation.more.stats.recap.motion

import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf

/**
 * The time recap motion follows, advanced by the story player from frame times. Compose animations would stretch with
 * the system animator scale, but entrances are timed against a page's fixed length, so the recap keeps its own pace;
 * only with animations removed does it stand still, entrances finished.
 *
 * @param still whether animations are removed.
 */
@Stable
internal class RecapClock(val still: Boolean) {
    /** Milliseconds the current page has played, not counting pauses. */
    var pageMillis by mutableLongStateOf(0L)
        private set

    /** Milliseconds the story has played, for motion that carries on from page to page. Stays at 0 when [still]. */
    var storyMillis by mutableLongStateOf(0L)
        private set

    /** Whether the page's entrance was cut short, as for sharing a page before it finished. */
    var entranceSkipped by mutableStateOf(still)
        private set

    fun advance(millis: Long) {
        pageMillis += millis
        if (!still) storyMillis += millis
    }

    fun restartPage() {
        pageMillis = 0L
        entranceSkipped = still
    }

    fun skipEntrance() {
        entranceSkipped = true
    }

    companion object {
        /** For a page shown outside a story: finished and still. */
        val Still = RecapClock(still = true)
    }
}

internal val LocalRecapClock = staticCompositionLocalOf { RecapClock.Still }
