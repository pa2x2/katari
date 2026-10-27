package tachiyomi.data.history.activity

import app.cash.sqldelight.async.coroutines.awaitAsOneOrNull
import tachiyomi.data.DatabaseHandler
import tachiyomi.domain.history.model.activity.HistoryActivityUpdate
import java.util.Date

/** Owns the atomic compatibility write shared by History and the detailed Statistics ledger. */
class HistoryActivityRecorder(
    private val handler: DatabaseHandler,
) {
    suspend fun record(update: HistoryActivityUpdate): Boolean {
        if (update.durationMillis <= 0L) return false
        val segments = splitActivityByLocalDay(
            startedAtEpochMillis = update.startedAtEpochMillis,
            endedAtEpochMillis = update.endedAtEpochMillis,
            durationMillis = update.durationMillis,
            timeZoneId = update.timeZoneId,
        )
        if (segments.isEmpty()) return false

        return handler.await(inTransaction = true) {
            val checkpoint = activityQueries.getActivitySessionCheckpoint(update.sessionId).awaitAsOneOrNull()
            if (checkpoint != null && checkpoint.entry_id != update.entryId) return@await false
            if (checkpoint != null && checkpoint.last_sequence >= update.sequence) return@await false

            if (checkpoint == null) {
                activityQueries.insertActivitySession(
                    sessionId = update.sessionId,
                    entryId = update.entryId,
                    startedAt = update.startedAtEpochMillis,
                    endedAt = update.endedAtEpochMillis,
                )
            }
            activityQueries.ensureStatisticsEpochForEntry(
                startedAt = update.startedAtEpochMillis,
                entryId = update.entryId,
            )
            segments.forEach { segment ->
                activityQueries.upsertActivitySegment(
                    sessionId = update.sessionId,
                    chapterId = update.chapterId,
                    localDate = segment.localDate,
                    timeZoneId = update.timeZoneId,
                    startedAt = segment.startedAtEpochMillis,
                    endedAt = segment.endedAtEpochMillis,
                    duration = segment.durationMillis,
                )
            }
            activityQueries.updateActivitySession(
                startedAt = update.startedAtEpochMillis,
                endedAt = update.endedAtEpochMillis,
                duration = update.durationMillis,
                sequence = update.sequence,
                sessionId = update.sessionId,
            )

            val aggregateEntryId = chaptersQueries.getChapterById(update.chapterId).awaitAsOneOrNull()?.entry_id
                ?: return@await true
            historyQueries.upsertUpdate(
                readAt = Date(update.endedAtEpochMillis),
                time_read = update.durationMillis,
                chapterId = update.chapterId,
            )
            historyQueries.upsertInsert(
                entryId = aggregateEntryId,
                chapterId = update.chapterId,
                readAt = Date(update.endedAtEpochMillis),
                time_read = update.durationMillis,
            )
            true
        }
    }
}
