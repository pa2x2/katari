package tachiyomi.data.history.activity

import tachiyomi.data.DatabaseHandler
import tachiyomi.domain.history.model.activity.HistoryCompletionUpdate

class HistoryCompletionRecorder(
    private val handler: DatabaseHandler,
) {
    suspend fun record(update: HistoryCompletionUpdate) {
        recordAll(listOf(update))
    }

    suspend fun recordAll(updates: List<HistoryCompletionUpdate>) {
        if (updates.isEmpty()) return
        handler.await(inTransaction = true) {
            val first = updates.first()
            activityQueries.ensureStatisticsEpochForEntry(
                startedAt = first.occurredAtEpochMillis,
                entryId = first.entryId,
            )
            updates.forEach { update ->
                require(update.entryId == first.entryId) { "Completion batch must belong to one entry" }
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
}
