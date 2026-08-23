package tachiyomi.domain.history.model.activity

enum class HistoryCompletionCause(val storageValue: String) {
    CONSUMPTION("consumption"),
    MANUAL("manual"),
}

data class HistoryCompletionUpdate(
    val eventId: String,
    val entryId: Long,
    val chapterId: Long?,
    val sessionId: String?,
    val occurredAtEpochMillis: Long,
    val localDate: String,
    val timeZoneId: String,
    val cause: HistoryCompletionCause,
)
