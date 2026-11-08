package tachiyomi.domain.history.model.activity

/**
 * One idempotent checkpoint from a logical reader or player session.
 *
 * [sequence] is scoped to [sessionId]. Replaying an already persisted sequence is a no-op, which lets lifecycle
 * flushes and backup restore retry safely without inflating either History or Statistics.
 */
data class HistoryActivityUpdate(
    val entryId: Long,
    val chapterId: Long,
    val sessionId: String,
    val sequence: Long,
    val startedAtEpochMillis: Long,
    val endedAtEpochMillis: Long,
    val durationMillis: Long,
    val timeZoneId: String,
) {
    init {
        require(entryId >= 0L) { "Activity entry ID cannot be negative" }
        require(chapterId >= 0L) { "Activity chapter ID cannot be negative" }
        require(sessionId.isNotBlank()) { "Activity session ID cannot be blank" }
        require(sequence >= 0L) { "Activity checkpoint sequence cannot be negative" }
        require(startedAtEpochMillis >= 0L) { "Activity start time cannot be negative" }
        require(endedAtEpochMillis >= startedAtEpochMillis) { "Activity cannot end before it starts" }
        require(durationMillis >= 0L) { "Activity duration cannot be negative" }
        require(timeZoneId.isNotBlank()) { "Activity time zone cannot be blank" }
    }
}
