package tachiyomi.domain.history.model.activity

data class HistoryActivitySnapshot(
    val sessions: List<HistoryActivitySessionSnapshot>,
    val completions: List<HistoryCompletionSnapshot>,
)

data class HistoryActivitySessionSnapshot(
    val sessionId: String,
    val startedAtEpochMillis: Long,
    val endedAtEpochMillis: Long,
    val durationMillis: Long,
    val lastSequence: Long,
    val segments: List<HistoryActivitySegmentSnapshot>,
)

data class HistoryActivitySegmentSnapshot(
    val chapterId: Long?,
    val localDate: String,
    val timeZoneId: String,
    val startedAtEpochMillis: Long,
    val endedAtEpochMillis: Long,
    val durationMillis: Long,
)

data class HistoryCompletionSnapshot(
    val eventId: String,
    val chapterId: Long?,
    val sessionId: String?,
    val occurredAtEpochMillis: Long,
    val localDate: String,
    val timeZoneId: String,
    val cause: HistoryCompletionCause,
)
