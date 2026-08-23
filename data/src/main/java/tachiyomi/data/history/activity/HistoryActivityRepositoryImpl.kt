package tachiyomi.data.history.activity

import eu.kanade.tachiyomi.source.entry.EntryType
import tachiyomi.data.DatabaseHandler
import tachiyomi.domain.history.model.activity.HistoryActivityPage
import tachiyomi.domain.history.model.activity.HistoryActivitySegmentDetail
import tachiyomi.domain.history.model.activity.HistoryActivitySessionDetail
import tachiyomi.domain.history.repository.HistoryActivityRepository

class HistoryActivityRepositoryImpl(
    private val handler: DatabaseHandler,
) : HistoryActivityRepository {

    override suspend fun getActivityPage(
        profileId: Long,
        startLocalDate: String,
        endLocalDate: String,
        type: EntryType?,
        offset: Long,
        limit: Long,
    ): HistoryActivityPage {
        require(offset >= 0L) { "Activity page offset cannot be negative" }
        require(limit > 0L) { "Activity page limit must be positive" }

        val rows = handler.awaitList {
            val mapper = {
                    sessionId: String,
                    entryId: Long,
                    typeName: String,
                    title: String,
                    localDate: String?,
                    startedAt: Long?,
                    endedAt: Long?,
                    duration: Long?,
                    completionCount: Long,
                ->
                SessionRow(
                    sessionId = sessionId,
                    entryId = entryId,
                    entryType = EntryType.valueOf(typeName.uppercase()),
                    entryTitle = title,
                    localDate = checkNotNull(localDate),
                    startedAtEpochMillis = checkNotNull(startedAt),
                    endedAtEpochMillis = checkNotNull(endedAt),
                    durationMillis = duration ?: 0L,
                    completionCount = completionCount,
                )
            }
            if (type == null) {
                activityQueries.getActivityDetailSessions(
                    startLocalDate = startLocalDate,
                    endLocalDate = endLocalDate,
                    profileId = profileId,
                    limit = limit + 1L,
                    offset = offset,
                    mapper = mapper,
                )
            } else {
                activityQueries.getActivityDetailSessionsByType(
                    startLocalDate = startLocalDate,
                    endLocalDate = endLocalDate,
                    profileId = profileId,
                    type = type.name.lowercase(),
                    limit = limit + 1L,
                    offset = offset,
                    mapper = mapper,
                )
            }
        }
        val pageRows = rows.take(limit.toInt())
        val segmentsBySession = if (pageRows.isEmpty()) {
            emptyMap()
        } else {
            handler.awaitList {
                activityQueries.getActivityDetailSegments(
                    sessionIds = pageRows.map(SessionRow::sessionId),
                    startLocalDate = startLocalDate,
                    endLocalDate = endLocalDate,
                ) {
                        sessionId,
                        chapterId,
                        chapterTitle,
                        localDate,
                        timeZoneId,
                        startedAt,
                        endedAt,
                        duration,
                    ->
                    sessionId to HistoryActivitySegmentDetail(
                        chapterId = chapterId,
                        chapterTitle = chapterTitle,
                        localDate = localDate,
                        timeZoneId = timeZoneId,
                        startedAtEpochMillis = startedAt,
                        endedAtEpochMillis = endedAt,
                        durationMillis = duration,
                    )
                }
            }.groupBy({ it.first }, { it.second })
        }

        return HistoryActivityPage(
            sessions = pageRows.map { row ->
                HistoryActivitySessionDetail(
                    sessionId = row.sessionId,
                    entryId = row.entryId,
                    entryType = row.entryType,
                    entryTitle = row.entryTitle,
                    localDate = row.localDate,
                    startedAtEpochMillis = row.startedAtEpochMillis,
                    endedAtEpochMillis = row.endedAtEpochMillis,
                    durationMillis = row.durationMillis,
                    completionCount = row.completionCount,
                    segments = segmentsBySession[row.sessionId].orEmpty(),
                )
            },
            hasMore = rows.size > pageRows.size,
        )
    }

    private data class SessionRow(
        val sessionId: String,
        val entryId: Long,
        val entryType: EntryType,
        val entryTitle: String,
        val localDate: String,
        val startedAtEpochMillis: Long,
        val endedAtEpochMillis: Long,
        val durationMillis: Long,
        val completionCount: Long,
    )
}
