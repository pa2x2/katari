package eu.kanade.tachiyomi.ui.video.player

/** Accumulates active wall-clock playback, excluding paused and buffering intervals. */
internal class VideoActivePlaybackClock(
    private val elapsedRealtime: () -> Long = { System.nanoTime() / 1_000_000L },
) {
    private var activeStartedAt: Long? = null
    private var accumulatedMillis: Long = 0L

    @Synchronized
    fun setActive(active: Boolean) {
        val now = elapsedRealtime()
        if (active) {
            if (activeStartedAt == null) activeStartedAt = now
        } else {
            accumulateUntil(now)
            activeStartedAt = null
        }
    }

    @Synchronized
    fun checkpoint(): Long {
        val now = elapsedRealtime()
        accumulateUntil(now)
        if (activeStartedAt != null) activeStartedAt = now
        return accumulatedMillis.also { accumulatedMillis = 0L }
    }

    private fun accumulateUntil(now: Long) {
        val startedAt = activeStartedAt ?: return
        accumulatedMillis += (now - startedAt).coerceAtLeast(0L)
    }
}
