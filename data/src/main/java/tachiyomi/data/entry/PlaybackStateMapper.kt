package tachiyomi.data.entry

import tachiyomi.domain.entry.model.PlaybackState

object PlaybackStateMapper {
    fun mapState(
        @Suppress("UNUSED_PARAMETER")
        id: Long,
        entryId: Long,
        chapterId: Long,
        positionMs: Long,
        durationMs: Long,
        completed: Boolean,
        lastWatchedAt: Long,
    ): PlaybackState = PlaybackState(
        entryId = entryId,
        chapterId = chapterId,
        positionMs = positionMs,
        durationMs = durationMs,
        completed = completed,
        lastWatchedAt = lastWatchedAt,
    )
}
