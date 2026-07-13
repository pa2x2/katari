package eu.kanade.tachiyomi.ui.video.player

import mihon.entry.interactions.anime.animeProgressState
import mihon.entry.interactions.anime.positionMs
import tachiyomi.domain.entry.model.EntryProgressState
import tachiyomi.domain.history.model.HistoryUpdate
import java.util.Date

internal class VideoPlaybackSession(
    private val entryId: Long,
    private val chapterId: Long,
    private val resourceKey: String,
    private val now: () -> Long = System::currentTimeMillis,
) {

    private var savedPositionMs: Long = 0L
    private var savedCompleted: Boolean = false
    private var completionUpdatedAt: Long = 0L

    fun restore(state: EntryProgressState?) {
        savedPositionMs = state?.positionMs ?: 0L
        savedCompleted = state?.completed ?: false
        completionUpdatedAt = state?.completionUpdatedAt ?: 0L
    }

    fun restore(positionMs: Long) {
        savedPositionMs = positionMs.coerceAtLeast(0L)
    }

    fun snapshot(positionMs: Long, durationMs: Long): Snapshot {
        val safePositionMs = positionMs.coerceAtLeast(0L)
        val safeDurationMs = durationMs.coerceAtLeast(0L)
        val completed = safeDurationMs > 0L && safePositionMs * 100 >= safeDurationMs * COMPLETION_PERCENTAGE
        val watchedDelta = (safePositionMs - savedPositionMs).coerceAtLeast(0L)
        val timestamp = now()
        if (completed != savedCompleted) {
            completionUpdatedAt = timestamp
        }

        savedPositionMs = safePositionMs
        savedCompleted = completed

        return Snapshot(
            progressState = animeProgressState(
                entryId = entryId,
                chapterId = chapterId,
                resourceKey = resourceKey,
                positionMs = safePositionMs,
                durationMs = safeDurationMs,
                completed = completed,
                locatorUpdatedAt = timestamp,
                completionUpdatedAt = completionUpdatedAt,
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
        val progressState: EntryProgressState,
        val historyUpdate: HistoryUpdate?,
    )

    private companion object {
        const val COMPLETION_PERCENTAGE = 90L
    }
}
