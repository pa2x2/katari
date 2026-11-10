package app.katari.entry.interactions.media.session

import kotlin.time.Duration
import kotlin.time.TimeSource

/**
 * Times what a reader records as read. It runs only while the reader is in front: resumed, which on the desktop means
 * its window has focus. Each call that takes time off it hands back the time read since the last one, for the reader
 * to record on the chapter it showed; a clock that is not running hands back nothing, so a chapter change or a
 * checkpoint while the reader is behind another window records nothing and starts nothing.
 */
class ReadingClock {
    private var startedAt: TimeSource.Monotonic.ValueTimeMark? = null

    /** The reader came to the front. A running clock keeps the time it has. */
    @Synchronized
    fun start() {
        if (startedAt == null) startedAt = TimeSource.Monotonic.markNow()
    }

    /** The reader left the front: the time read since the last call, after which the clock stands still. */
    @Synchronized
    fun stop(): Duration {
        val started = startedAt ?: return Duration.ZERO
        startedAt = null
        return started.elapsedNow()
    }

    /** The time read since the last call, the clock running on if it runs: at a checkpoint or a change of chapter. */
    @Synchronized
    fun lap(): Duration {
        val started = startedAt ?: return Duration.ZERO
        val now = TimeSource.Monotonic.markNow()
        startedAt = now
        return now - started
    }
}
