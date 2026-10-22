package eu.kanade.presentation.more.stats.recap.motion

import android.content.Context
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.util.system.animatorDurationScale

/**
 * The time recap motion follows, advanced by the story player from frame times. Compose animations would stretch with
 * the system animator scale, but entrances are timed against a page's fixed length, so the recap keeps its own pace;
 * only with animations removed does it stand still, entrances finished.
 *
 * @param still whether animations are removed.
 * @param slowdown how many times slower than real time the story plays; see [recapSlowdown].
 */
@Stable
internal class RecapClock(val still: Boolean, private val slowdown: Float = 1f) {
    /** Milliseconds the current page has played, not counting pauses. */
    var pageMillis by mutableLongStateOf(0L)
        private set

    /** Milliseconds the story has played, for motion that carries on from page to page. Stays at 0 when [still]. */
    var storyMillis by mutableLongStateOf(0L)
        private set

    /** Whether the page's entrance was cut short, as for sharing a page before it finished. */
    var entranceSkipped by mutableStateOf(still)
        private set

    // Story time not yet counted: slowed down, a frame can last less than a millisecond of the story.
    private var pendingNanos = 0.0

    fun advance(frameNanos: Long) {
        pendingNanos += frameNanos / slowdown
        val millis = (pendingNanos / NANOS_PER_MILLI).toLong()
        pendingNanos -= millis * NANOS_PER_MILLI
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

/**
 * 1, except in builds that turn on `recap_follows_animator_scale`, where the story stretches with the system animator
 * scale along with the Compose animations around it. The showcase captures with such a build: a device too slow to draw
 * the story at full rate records it slowed down, each frame then covering less of the story, and speeds the clip up
 * again.
 */
internal fun Context.recapSlowdown(): Float {
    if (!resources.getBoolean(R.bool.recap_follows_animator_scale)) return 1f
    return animatorDurationScale.takeIf { it > 0f } ?: 1f
}

private const val NANOS_PER_MILLI = 1_000_000.0

internal val LocalRecapClock = staticCompositionLocalOf { RecapClock.Still }
