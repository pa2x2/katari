package tachiyomi.data.history.activity

import app.cash.sqldelight.async.coroutines.awaitAsOne
import app.cash.sqldelight.async.coroutines.awaitAsOneOrNull
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import tachiyomi.data.DatabaseHandler
import tachiyomi.data.query.chunkedForSqlQuery
import tachiyomi.domain.history.model.activity.HistoryActivitySegmentSnapshot
import tachiyomi.domain.history.model.activity.HistoryActivitySessionSnapshot
import tachiyomi.domain.history.model.activity.HistoryActivitySnapshot
import tachiyomi.domain.history.model.activity.HistoryCompletionCause
import tachiyomi.domain.history.model.activity.HistoryCompletionSnapshot
import tachiyomi.domain.history.repository.HistoryActivityBackupRepository
import java.nio.charset.StandardCharsets
import java.util.UUID

@Inject
@SingleIn(AppScope::class)
class HistoryActivityBackupRepositoryImpl(
    private val handler: DatabaseHandler,
) : HistoryActivityBackupRepository {

    override suspend fun getActivityByEntryIds(entryIds: Collection<Long>): Map<Long, HistoryActivitySnapshot> {
        return buildMap {
            entryIds.distinct().chunkedForSqlQuery().forEach { chunk -> putAll(getActivityChunk(chunk)) }
        }
    }

    private suspend fun getActivityChunk(entryIds: List<Long>): Map<Long, HistoryActivitySnapshot> {
        val segmentsBySession = handler.awaitList {
            activityQueries.getActivitySegmentsByEntryIds(entryIds) {
                    _,
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
        val sessionsByEntry = handler.awaitList {
            activityQueries.getActivitySessionsByEntryIds(entryIds) {
                    entryId,
                    sessionId,
                    startedAt,
                    endedAt,
                    duration,
                    lastSequence,
                ->
                entryId to HistoryActivitySessionSnapshot(
                    sessionId = sessionId,
                    startedAtEpochMillis = startedAt,
                    endedAtEpochMillis = endedAt,
                    durationMillis = duration,
                    lastSequence = lastSequence,
                    segments = segmentsBySession[sessionId].orEmpty(),
                )
            }
        }.groupBy({ it.first }, { it.second })
        val completionsByEntry = handler.awaitList {
            activityQueries.getCompletionEventsByEntryIds(entryIds) {
                    entryId,
                    eventId,
                    chapterId,
                    sessionId,
                    occurredAt,
                    localDate,
                    timeZoneId,
                    cause,
                ->
                entryId to HistoryCompletionSnapshot(
                    eventId = eventId,
                    chapterId = chapterId,
                    sessionId = sessionId,
                    occurredAtEpochMillis = occurredAt,
                    localDate = localDate,
                    timeZoneId = timeZoneId,
                    cause = HistoryCompletionCause.entries.first { it.storageValue == cause },
                )
            }
        }.groupBy({ it.first }, { it.second })
        return (sessionsByEntry.keys + completionsByEntry.keys).associateWith { entryId ->
            HistoryActivitySnapshot(sessionsByEntry[entryId].orEmpty(), completionsByEntry[entryId].orEmpty())
        }
    }

    override suspend fun restoreActivity(entryId: Long, snapshot: HistoryActivitySnapshot) {
        handler.await(inTransaction = true) {
            val profile = activityQueries.getEntryProfile(entryId).awaitAsOne()

            // Session and event ids key their rows across every profile, and sync keys a session's record by its id
            // alone, but one backup restored into two profiles brings the same ids into both. The profile that gets
            // an id second stores the activity under an id of its own, made from the backup's, so restoring there
            // again still finds and merges it.
            fun String.inRestoredProfile(holderProfileId: Long?): String {
                if (holderProfileId == null || holderProfileId == profile._id) return this
                val identity = listOf(profile.uuid, this).joinToString(separator = "\u001f")
                return UUID.nameUUIDFromBytes(identity.toByteArray(StandardCharsets.UTF_8)).toString()
            }
            val restoredSessionIds = mutableMapOf<String, String>()
            snapshot.sessions.filter(HistoryActivitySessionSnapshot::isValidForRestore).forEach { session ->
                val sessionId = session.sessionId.inRestoredProfile(
                    activityQueries.getActivitySessionProfile(session.sessionId).awaitAsOneOrNull(),
                )
                activityQueries.restoreActivitySession(
                    sessionId = sessionId,
                    entryId = entryId,
                    startedAt = session.startedAtEpochMillis,
                    endedAt = session.endedAtEpochMillis,
                    duration = session.durationMillis,
                    lastSequence = session.lastSequence,
                )
                val restored = activityQueries.getActivitySessionCheckpoint(sessionId).awaitAsOneOrNull()
                if (restored?.entry_id != entryId) return@forEach
                restoredSessionIds[session.sessionId] = sessionId
                session.segments.filter(HistoryActivitySegmentSnapshot::isValidForRestore).forEach { segment ->
                    activityQueries.restoreActivitySegment(
                        sessionId = sessionId,
                        chapterId = segment.chapterId,
                        localDate = segment.localDate,
                        timeZoneId = segment.timeZoneId,
                        startedAt = segment.startedAtEpochMillis,
                        endedAt = segment.endedAtEpochMillis,
                        duration = segment.durationMillis,
                    )
                    activityQueries.insertActivitySegmentIfAbsent(
                        sessionId = sessionId,
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
                    eventId = completion.eventId.inRestoredProfile(
                        activityQueries.getCompletionEventProfile(completion.eventId).awaitAsOneOrNull(),
                    ),
                    entryId = entryId,
                    chapterId = completion.chapterId,
                    sessionId = completion.sessionId?.let(restoredSessionIds::get),
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
