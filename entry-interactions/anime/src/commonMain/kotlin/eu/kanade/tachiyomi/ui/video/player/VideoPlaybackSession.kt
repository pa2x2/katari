package eu.kanade.tachiyomi.ui.video.player

import mihon.entry.interactions.anime.state.animeProgressState
import mihon.entry.interactions.anime.state.positionMs
import mihon.entry.interactions.media.session.EntryMediaSessionActivity
import mihon.entry.interactions.media.session.EntryMediaSessionActivitySession
import tachiyomi.domain.entry.model.EntryProgressState

internal class VideoPlaybackSession(
    private val entryId: Long,
    private val chapterId: Long,
    private val resourceKey: String,
    private val activitySession: EntryMediaSessionActivitySession = EntryMediaSessionActivitySession(),
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

    fun snapshot(positionMs: Long, durationMs: Long, activeDurationMs: Long = 0L): Snapshot {
        val safePositionMs = positionMs.coerceAtLeast(0L)
        val safeDurationMs = durationMs.coerceAtLeast(0L)
        val completed = safeDurationMs > 0L && safePositionMs * 100 >= safeDurationMs * COMPLETION_PERCENTAGE
        val completedNow = completed && !savedCompleted
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
            activity = activeDurationMs.takeIf { it > 0L }?.let { activitySession.record(it, timestamp) },
            completedNow = completedNow,
        )
    }

    data class Snapshot(
        val progressState: EntryProgressState,
        val activity: EntryMediaSessionActivity?,
        val completedNow: Boolean,
    )

    private companion object {
        const val COMPLETION_PERCENTAGE = 90L
    }
}
