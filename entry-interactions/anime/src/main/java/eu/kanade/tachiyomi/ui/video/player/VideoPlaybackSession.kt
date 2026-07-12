package eu.kanade.tachiyomi.ui.video.player

import tachiyomi.domain.entry.model.PlaybackState
import tachiyomi.domain.history.model.HistoryUpdate
import java.util.Date

internal class VideoPlaybackSession(
    private val entryId: Long,
    private val chapterId: Long,
    private val now: () -> Long = System::currentTimeMillis,
) {

    private var savedPositionMs: Long = 0L

    fun restore(positionMs: Long) {
        savedPositionMs = positionMs.coerceAtLeast(0L)
    }

    fun snapshot(positionMs: Long, durationMs: Long): Snapshot {
        val safePositionMs = positionMs.coerceAtLeast(0L)
        val safeDurationMs = durationMs.coerceAtLeast(0L)
        val completed = safeDurationMs > 0L && safePositionMs * 100 >= safeDurationMs * COMPLETION_PERCENTAGE
        val watchedDelta = (safePositionMs - savedPositionMs).coerceAtLeast(0L)
        val timestamp = now()

        savedPositionMs = safePositionMs

        return Snapshot(
            playbackState = PlaybackState(
                entryId = entryId,
                chapterId = chapterId,
                positionMs = safePositionMs,
                durationMs = safeDurationMs,
                completed = completed,
                lastWatchedAt = timestamp,
            ),
            historyUpdate = watchedDelta.takeIf { it > 0L }?.let {
                HistoryUpdate(
                    chapterId = chapterId,
                    readAt = Date(timestamp),
                    sessionReadDuration = it,
                )
            },
        )
    }

    data class Snapshot(
        val playbackState: PlaybackState,
        val historyUpdate: HistoryUpdate?,
    )

    private companion object {
        const val COMPLETION_PERCENTAGE = 90L
    }
}
