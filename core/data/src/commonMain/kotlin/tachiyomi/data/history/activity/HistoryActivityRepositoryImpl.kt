package tachiyomi.data.history.activity

import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import eu.kanade.tachiyomi.source.entry.EntryType
import tachiyomi.data.DatabaseHandler
import tachiyomi.domain.entry.model.EntryCover
import tachiyomi.domain.history.model.activity.HistoryActivityCompletion
import tachiyomi.domain.history.model.activity.HistoryActivityPage
import tachiyomi.domain.history.model.activity.HistoryActivityScope
import tachiyomi.domain.history.model.activity.HistoryActivitySegmentDetail
import tachiyomi.domain.history.model.activity.HistoryActivitySessionDetail
import tachiyomi.domain.history.repository.HistoryActivityRepository
import tachiyomi.domain.statistics.service.StatisticsActivityPolicy

@Inject
@SingleIn(AppScope::class)
class HistoryActivityRepositoryImpl(
    private val handler: DatabaseHandler,
) : HistoryActivityRepository {

    override suspend fun getActivityPage(
        profileId: Long,
        startLocalDate: String,
        endLocalDate: String,
        scope: HistoryActivityScope,
        dayOffset: Long,
        dayLimit: Long,
    ): HistoryActivityPage {
        require(dayOffset >= 0L) { "Activity page offset cannot be negative" }
        require(dayLimit > 0L) { "Activity page limit must be positive" }
        val minimumDuration = StatisticsActivityPolicy.MINIMUM_SESSION_DURATION_MILLIS

        val days = handler.awaitList {
            when (scope) {
                HistoryActivityScope.All -> activityQueries.getActivityDetailDays(
                    profileId = profileId,
                    minimumSessionDurationMillis = minimumDuration,
                    startLocalDate = startLocalDate,
                    endLocalDate = endLocalDate,
                    limit = dayLimit + 1L,
                    offset = dayOffset,
                )
                is HistoryActivityScope.Type -> activityQueries.getActivityDetailDaysByType(
                    profileId = profileId,
                    type = scope.type.name.lowercase(),
                    minimumSessionDurationMillis = minimumDuration,
                    startLocalDate = startLocalDate,
                    endLocalDate = endLocalDate,
                    limit = dayLimit + 1L,
                    offset = dayOffset,
                )
                is HistoryActivityScope.Entries -> activityQueries.getActivityDetailDaysByEntries(
                    profileId = profileId,
                    entryIds = scope.entryIds,
                    minimumSessionDurationMillis = minimumDuration,
                    startLocalDate = startLocalDate,
                    endLocalDate = endLocalDate,
                    limit = dayLimit + 1L,
                    offset = dayOffset,
                )
            }
        }
        val pageDays = days.take(dayLimit.toInt())
        if (pageDays.isEmpty()) return HistoryActivityPage(emptyList(), emptyList(), hasMore = false)
        // Days are listed newest first, so the page covers every day from its last to its first.
        val firstLocalDate = pageDays.last()
        val lastLocalDate = pageDays.first()

        val sessions = handler.awaitList {
            val mapper = {
                    sessionId: String,
                    entryId: Long,
                    typeName: String,
                    title: String,
                    thumbnailUrl: String?,
                    source: Long,
                    favorite: Boolean,
                    coverLastModified: Long,
                ->
                SessionRow(
                    sessionId = sessionId,
                    entryId = entryId,
                    entryType = EntryType.valueOf(typeName.uppercase()),
                    entryTitle = title,
                    coverData = EntryCover(
                        entryId = entryId,
                        sourceId = source,
                        isFavorite = favorite,
                        url = thumbnailUrl,
                        lastModified = coverLastModified,
                    ),
                )
            }
            when (scope) {
                HistoryActivityScope.All -> activityQueries.getActivityDetailSessions(
                    profileId = profileId,
                    minimumSessionDurationMillis = minimumDuration,
                    startLocalDate = startLocalDate,
                    endLocalDate = endLocalDate,
                    firstLocalDate = firstLocalDate,
                    lastLocalDate = lastLocalDate,
                    mapper = mapper,
                )
                is HistoryActivityScope.Type -> activityQueries.getActivityDetailSessionsByType(
                    profileId = profileId,
                    type = scope.type.name.lowercase(),
                    minimumSessionDurationMillis = minimumDuration,
                    startLocalDate = startLocalDate,
                    endLocalDate = endLocalDate,
                    firstLocalDate = firstLocalDate,
                    lastLocalDate = lastLocalDate,
                    mapper = mapper,
                )
                is HistoryActivityScope.Entries -> activityQueries.getActivityDetailSessionsByEntries(
                    profileId = profileId,
                    entryIds = scope.entryIds,
                    minimumSessionDurationMillis = minimumDuration,
                    startLocalDate = startLocalDate,
                    endLocalDate = endLocalDate,
                    firstLocalDate = firstLocalDate,
                    lastLocalDate = lastLocalDate,
                    mapper = mapper,
                )
            }
        }
        val sessionIds = sessions.map(SessionRow::sessionId)
        val segmentsBySession = handler.awaitList {
            activityQueries.getActivityDetailSegments(
                sessionIds = sessionIds,
                firstLocalDate = firstLocalDate,
                lastLocalDate = lastLocalDate,
            ) { sessionId, chapterId, chapterTitle, localDate, timeZoneId, startedAt, endedAt, duration ->
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
        val completionsBySession = handler.awaitList {
            activityQueries.getActivityDetailCompletions(
                sessionIds = sessionIds,
                firstLocalDate = firstLocalDate,
                lastLocalDate = lastLocalDate,
            ) { sessionId, chapterId, localDate ->
                sessionId to HistoryActivityCompletion(chapterId = checkNotNull(chapterId), localDate = localDate)
            }
        }.groupBy({ it.first.orEmpty() }, { it.second })

        return HistoryActivityPage(
            sessions = sessions.map { row ->
                HistoryActivitySessionDetail(
                    sessionId = row.sessionId,
                    entryId = row.entryId,
                    entryType = row.entryType,
                    entryTitle = row.entryTitle,
                    coverData = row.coverData,
                    segments = segmentsBySession[row.sessionId].orEmpty(),
                    completions = completionsBySession[row.sessionId].orEmpty(),
                )
            },
            localDates = pageDays,
            hasMore = days.size > pageDays.size,
        )
    }

    private data class SessionRow(
        val sessionId: String,
        val entryId: Long,
        val entryType: EntryType,
        val entryTitle: String,
        val coverData: EntryCover,
    )
}
