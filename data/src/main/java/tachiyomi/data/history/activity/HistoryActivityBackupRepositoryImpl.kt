package tachiyomi.data.history.activity

import app.cash.sqldelight.async.coroutines.awaitAsOneOrNull
import tachiyomi.data.DatabaseHandler
import tachiyomi.domain.history.model.activity.HistoryActivitySegmentSnapshot
import tachiyomi.domain.history.model.activity.HistoryActivitySessionSnapshot
import tachiyomi.domain.history.model.activity.HistoryActivitySnapshot
import tachiyomi.domain.history.model.activity.HistoryCompletionCause
import tachiyomi.domain.history.model.activity.HistoryCompletionSnapshot
import tachiyomi.domain.history.repository.HistoryActivityBackupRepository

class HistoryActivityBackupRepositoryImpl(
    private val handler: DatabaseHandler,
) : HistoryActivityBackupRepository {

    override suspend fun getActivityByEntryId(entryId: Long): HistoryActivitySnapshot {
        val segmentsBySession = handler.awaitList {
            activityQueries.getActivitySegmentsByEntryId(entryId) {
                    sessionId,
                    chapterId,
                    localDate,
                    timeZoneId,
                    startedAt,
                    endedAt,
                    duration,
                ->
                sessionId to HistoryActivitySegmentSnapshot(
                    chapterId = chapterId,
                    localDate = localDate,
                    timeZoneId = timeZoneId,
                    startedAtEpochMillis = startedAt,
                    endedAtEpochMillis = endedAt,
                    durationMillis = duration,
                )
            }
        }.groupBy({ it.first }, { it.second })
        val sessions = handler.awaitList {
            activityQueries.getActivitySessionsByEntryId(entryId) {
                    sessionId,
                    startedAt,
                    endedAt,
                    duration,
                    lastSequence,
                ->
                HistoryActivitySessionSnapshot(
                    sessionId = sessionId,
                    startedAtEpochMillis = startedAt,
                    endedAtEpochMillis = endedAt,
                    durationMillis = duration,
                    lastSequence = lastSequence,
                    segments = segmentsBySession[sessionId].orEmpty(),
                )
            }
        }
        val completions = handler.awaitList {
            activityQueries.getCompletionEventsByEntryId(entryId) {
                    eventId,
                    chapterId,
                    sessionId,
                    occurredAt,
                    localDate,
                    timeZoneId,
                    cause,
                ->
                HistoryCompletionSnapshot(
                    eventId = eventId,
                    chapterId = chapterId,
                    sessionId = sessionId,
                    occurredAtEpochMillis = occurredAt,
                    localDate = localDate,
                    timeZoneId = timeZoneId,
                    cause = HistoryCompletionCause.entries.first { it.storageValue == cause },
                )
            }
        }
        return HistoryActivitySnapshot(sessions, completions)
    }

    override suspend fun restoreActivity(entryId: Long, snapshot: HistoryActivitySnapshot) {
        handler.await(inTransaction = true) {
            val validSessionIds = mutableSetOf<String>()
            snapshot.sessions.filter(HistoryActivitySessionSnapshot::isValidForRestore).forEach { session ->
                activityQueries.restoreActivitySession(
                    sessionId = session.sessionId,
                    entryId = entryId,
                    startedAt = session.startedAtEpochMillis,
                    endedAt = session.endedAtEpochMillis,
                    duration = session.durationMillis,
                    lastSequence = session.lastSequence,
                )
                val restored = activityQueries.getActivitySessionCheckpoint(session.sessionId).awaitAsOneOrNull()
                if (restored?.entry_id != entryId) return@forEach
                validSessionIds += session.sessionId
                session.segments.filter(HistoryActivitySegmentSnapshot::isValidForRestore).forEach { segment ->
                    activityQueries.restoreActivitySegment(
                        sessionId = session.sessionId,
                        chapterId = segment.chapterId,
                        localDate = segment.localDate,
                        timeZoneId = segment.timeZoneId,
                        startedAt = segment.startedAtEpochMillis,
                        endedAt = segment.endedAtEpochMillis,
                        duration = segment.durationMillis,
                    )
                    activityQueries.insertActivitySegmentIfAbsent(
                        sessionId = session.sessionId,
                        chapterId = segment.chapterId,
                        localDate = segment.localDate,
                        timeZoneId = segment.timeZoneId,
                        startedAt = segment.startedAtEpochMillis,
                        endedAt = segment.endedAtEpochMillis,
                        duration = segment.durationMillis,
                    )
                }
            }
            snapshot.completions.filter(HistoryCompletionSnapshot::isValidForRestore).forEach { completion ->
                activityQueries.insertCompletionEvent(
                    eventId = completion.eventId,
                    entryId = entryId,
                    chapterId = completion.chapterId,
                    sessionId = completion.sessionId?.takeIf(validSessionIds::contains),
                    occurredAt = completion.occurredAtEpochMillis,
                    localDate = completion.localDate,
                    timeZoneId = completion.timeZoneId,
                    cause = completion.cause.storageValue,
                )
            }
        }
    }

    override suspend fun getStatisticsEpoch(profileId: Long): Long? {
        return handler.awaitOneOrNull { activityQueries.getStatisticsEpoch(profileId) }
    }

    override suspend fun restoreStatisticsEpoch(profileId: Long, startedAtEpochMillis: Long) {
        if (startedAtEpochMillis < 0L) return
        handler.await { activityQueries.restoreStatisticsEpoch(profileId, startedAtEpochMillis) }
    }
}

private fun HistoryActivitySessionSnapshot.isValidForRestore(): Boolean {
    return sessionId.isNotBlank() &&
        startedAtEpochMillis >= 0L &&
        endedAtEpochMillis >= startedAtEpochMillis &&
        durationMillis >= 0L &&
        lastSequence >= -1L
}

private fun HistoryActivitySegmentSnapshot.isValidForRestore(): Boolean {
    return localDate.length == 10 &&
        timeZoneId.isNotBlank() &&
        startedAtEpochMillis >= 0L &&
        endedAtEpochMillis >= startedAtEpochMillis &&
        durationMillis > 0L
}

private fun HistoryCompletionSnapshot.isValidForRestore(): Boolean {
    return eventId.isNotBlank() &&
        occurredAtEpochMillis >= 0L &&
        localDate.length == 10 &&
        timeZoneId.isNotBlank()
}
