package tachiyomi.domain.entry.model

data class PlaybackState(
    val entryId: Long,
    val chapterId: Long,
    val positionMs: Long,
    val durationMs: Long,
    val completed: Boolean,
    val lastWatchedAt: Long,
)
