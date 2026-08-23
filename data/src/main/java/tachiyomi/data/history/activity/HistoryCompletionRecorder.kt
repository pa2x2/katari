package tachiyomi.data.history.activity

import tachiyomi.data.DatabaseHandler
import tachiyomi.domain.history.model.activity.HistoryCompletionUpdate

class HistoryCompletionRecorder(
    private val handler: DatabaseHandler,
) {
    suspend fun record(update: HistoryCompletionUpdate) {
        handler.await(inTransaction = true) {
            activityQueries.ensureStatisticsEpochForEntry(
                startedAt = update.occurredAtEpochMillis,
                entryId = update.entryId,
            )
            activityQueries.insertCompletionEvent(
                eventId = update.eventId,
                entryId = update.entryId,
                chapterId = update.chapterId,
                sessionId = update.sessionId,
                occurredAt = update.occurredAtEpochMillis,
                localDate = update.localDate,
                timeZoneId = update.timeZoneId,
                cause = update.cause.storageValue,
            )
        }
    }
}
