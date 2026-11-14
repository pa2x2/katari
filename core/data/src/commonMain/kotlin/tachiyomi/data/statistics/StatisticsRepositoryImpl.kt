package tachiyomi.data.statistics

import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import eu.kanade.tachiyomi.source.entry.EntryType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import tachiyomi.data.DatabaseHandler
import tachiyomi.domain.entry.model.EntryCover
import tachiyomi.domain.statistics.model.StatisticsActivityBucket
import tachiyomi.domain.statistics.model.StatisticsActivitySegment
import tachiyomi.domain.statistics.model.StatisticsActivitySnapshot
import tachiyomi.domain.statistics.model.StatisticsActivityTimeline
import tachiyomi.domain.statistics.model.StatisticsCompletionBucket
import tachiyomi.domain.statistics.model.StatisticsEarlierActivity
import tachiyomi.domain.statistics.model.StatisticsEarlierActivityDetails
import tachiyomi.domain.statistics.model.StatisticsSessionSummary
import tachiyomi.domain.statistics.model.StatisticsTopEntry
import tachiyomi.domain.statistics.model.StatisticsWindowActivity
import tachiyomi.domain.statistics.repository.StatisticsRepository
import tachiyomi.domain.statistics.service.StatisticsActivityPolicy

@Inject
@SingleIn(AppScope::class)
class StatisticsRepositoryImpl(
    private val handler: DatabaseHandler,
) : StatisticsRepository {
    fun subscribeActivity(
        profileId: Long,
        startLocalDate: String?,
    ): Flow<StatisticsActivitySnapshot> = subscribeActivity(profileId, startLocalDate, null)

    override suspend fun getEarlierActivityDetails(
        profileId: Long,
        type: EntryType?,
        limit: Long,
    ): StatisticsEarlierActivityDetails {
        val totals = getEarlierActivity(profileId).let { rows ->
            if (type == null) rows else rows.filter { it.type == type }
        }
        val topEntries = handler.awaitList {
            earlierActivityViewQueries.earlierActivityEntries(
                profileId = profileId,
                type = type?.name?.lowercase(),
                limit = limit,
            ) { entryId, entryType, title, duration, thumbnailUrl, source, favorite, coverLastModified ->
                topEntry(
                    entryId = entryId,
                    type = entryType,
                    title = title,
                    duration = duration,
                    thumbnailUrl = thumbnailUrl,
                    source = source,
                    favorite = favorite,
                    coverLastModified = coverLastModified,
                    completionCount = 0L,
                )
            }
        }
        val trackingStartedAt = handler.awaitOneOrNull { activityQueries.getStatisticsEpoch(profileId) }
        return StatisticsEarlierActivityDetails(
            totals = totals,
            topEntries = topEntries,
            trackingStartedAtEpochMillis = trackingStartedAt,
        )
    }

    override suspend fun getTopEntriesPage(
        profileId: Long,
        type: EntryType?,
        startLocalDate: String?,
        endLocalDate: String,
        offset: Long,
        limit: Long,
    ): List<StatisticsTopEntry> {
        require(offset >= 0L) { "Top entries offset cannot be negative" }
        require(limit > 0L) { "Top entries limit must be positive" }
        return handler.awaitList {
            statisticsViewQueries.topActivityEntriesPage(
                profileId = profileId,
                type = type?.name?.lowercase(),
                minimumSessionDurationMillis = StatisticsActivityPolicy.MINIMUM_SESSION_DURATION_MILLIS,
                startLocalDate = startLocalDate,
                endLocalDate = endLocalDate,
                limit = limit,
                offset = offset,
                mapper = ::topEntry,
            )
        }
    }

    override fun subscribeActivity(
        profileId: Long,
        startLocalDate: String?,
        endLocalDate: String?,
    ): Flow<StatisticsActivitySnapshot> {
        return combine(
            subscribeActivityRows(profileId, startLocalDate, endLocalDate),
            subscribeCompletions(profileId, startLocalDate, endLocalDate),
            combine(
                subscribeTopEntries(profileId, startLocalDate, endLocalDate),
                subscribeSessionSummaries(profileId, startLocalDate, endLocalDate),
                subscribeSegments(profileId, startLocalDate, endLocalDate),
                ::Triple,
            ),
            handler.subscribeToOneOrNull { activityQueries.getStatisticsEpoch(profileId) },
            subscribeEarlierActivity(profileId),
        ) { activity, completions, (topEntries, sessions, segments), trackingStartedAt, earlierActivity ->
            StatisticsActivitySnapshot(
                profileId = profileId,
                trackingStartedAtEpochMillis = trackingStartedAt,
                activity = activity,
                completions = completions,
                topEntries = topEntries,
                sessions = sessions,
                earlierActivity = earlierActivity,
                segments = segments,
            )
        }
    }

    override fun subscribeEntriesActivity(
        entryIds: List<Long>,
        startLocalDate: String,
        endLocalDate: String,
    ): Flow<StatisticsWindowActivity> {
        val minimumDuration = StatisticsActivityPolicy.MINIMUM_SESSION_DURATION_MILLIS
        return combine(
            handler.subscribeToList {
                statisticsViewQueries.activityTotalsOfEntriesInWindow(
                    entryIds = entryIds,
                    minimumSessionDurationMillis = minimumDuration,
                    startLocalDate = startLocalDate,
                    endLocalDate = endLocalDate,
                    mapper = ::activityBucket,
                )
            },
            handler.subscribeToList {
                statisticsViewQueries.completionTotalsOfEntriesInWindow(
                    entryIds = entryIds,
                    startLocalDate = startLocalDate,
                    endLocalDate = endLocalDate,
                    mapper = ::completionBucket,
                )
            },
            handler.subscribeToOne {
                statisticsViewQueries.sessionCountOfEntriesInWindow(
                    entryIds = entryIds,
                    minimumSessionDurationMillis = minimumDuration,
                    startLocalDate = startLocalDate,
                    endLocalDate = endLocalDate,
                )
            },
            handler.subscribeToList {
                statisticsViewQueries.activitySegmentsOfEntriesInWindow(
                    entryIds = entryIds,
                    minimumSessionDurationMillis = minimumDuration,
                    startLocalDate = startLocalDate,
                    endLocalDate = endLocalDate,
                    mapper = ::activitySegment,
                )
            },
            ::StatisticsWindowActivity,
        )
    }

    override fun subscribeActivityTimeline(
        profileId: Long,
        startLocalDate: String?,
        endLocalDate: String,
    ): Flow<StatisticsActivityTimeline> = combine(
        subscribeActivityRows(profileId, startLocalDate, endLocalDate),
        subscribeCompletions(profileId, startLocalDate, endLocalDate),
        ::StatisticsActivityTimeline,
    )

    private fun subscribeEarlierActivity(profileId: Long): Flow<List<StatisticsEarlierActivity>> =
        handler.subscribeToList { earlierActivityViewQueries.earlierActivityByType(profileId, ::earlierActivity) }

    private suspend fun getEarlierActivity(profileId: Long): List<StatisticsEarlierActivity> =
        handler.awaitList { earlierActivityViewQueries.earlierActivityByType(profileId, ::earlierActivity) }

    private fun subscribeActivityRows(
        profileId: Long,
        startLocalDate: String?,
        endLocalDate: String?,
    ): Flow<List<StatisticsActivityBucket>> = handler.subscribeToList {
        statisticsViewQueries.activityTotalsInWindow(
            profileId = profileId,
            minimumSessionDurationMillis = StatisticsActivityPolicy.MINIMUM_SESSION_DURATION_MILLIS,
            startLocalDate = startLocalDate,
            endLocalDate = endLocalDate,
            mapper = ::activityBucket,
        )
    }

    private fun subscribeCompletions(
        profileId: Long,
        startLocalDate: String?,
        endLocalDate: String?,
    ): Flow<List<StatisticsCompletionBucket>> = handler.subscribeToList {
        statisticsViewQueries.completionTotalsInWindow(profileId, startLocalDate, endLocalDate, ::completionBucket)
    }

    private fun subscribeTopEntries(
        profileId: Long,
        startLocalDate: String?,
        endLocalDate: String?,
    ): Flow<List<StatisticsTopEntry>> = handler.subscribeToList {
        statisticsViewQueries.topActivityEntriesInWindow(
            profileId = profileId,
            minimumSessionDurationMillis = StatisticsActivityPolicy.MINIMUM_SESSION_DURATION_MILLIS,
            startLocalDate = startLocalDate,
            endLocalDate = endLocalDate,
            mapper = ::topEntry,
        )
    }

    private fun subscribeSegments(
        profileId: Long,
        startLocalDate: String?,
        endLocalDate: String?,
    ): Flow<List<StatisticsActivitySegment>> = handler.subscribeToList {
        statisticsViewQueries.activitySegmentsInWindow(
            profileId = profileId,
            minimumSessionDurationMillis = StatisticsActivityPolicy.MINIMUM_SESSION_DURATION_MILLIS,
            startLocalDate = startLocalDate,
            endLocalDate = endLocalDate,
            mapper = ::activitySegment,
        )
    }

    private fun subscribeSessionSummaries(
        profileId: Long,
        startLocalDate: String?,
        endLocalDate: String?,
    ): Flow<List<StatisticsSessionSummary>> = handler.subscribeToList {
        val mapper = {
                type: String,
                sessionCount: Long,
                averageDuration: Long?,
                longestDuration: Long?,
            ->
            StatisticsSessionSummary(
                type = EntryType.valueOf(type.uppercase()),
                sessionCount = sessionCount,
                averageDurationMillis = averageDuration ?: 0L,
                longestDurationMillis = longestDuration ?: 0L,
            )
        }
        statisticsViewQueries.sessionSummariesInWindow(
            profileId = profileId,
            minimumSessionDurationMillis = StatisticsActivityPolicy.MINIMUM_SESSION_DURATION_MILLIS,
            startLocalDate = startLocalDate,
            endLocalDate = endLocalDate,
            mapper = mapper,
        )
    }
}

private fun topEntry(
    entryId: Long,
    type: String,
    title: String,
    duration: Long?,
    thumbnailUrl: String?,
    source: Long,
    favorite: Boolean,
    coverLastModified: Long,
    completionCount: Long,
): StatisticsTopEntry = StatisticsTopEntry(
    entryId = entryId,
    type = EntryType.valueOf(type.uppercase()),
    title = title,
    durationMillis = duration ?: 0L,
    cover = EntryCover(
        entryId = entryId,
        sourceId = source,
        isFavorite = favorite,
        url = thumbnailUrl,
        lastModified = coverLastModified,
    ),
    completionCount = completionCount,
)

private fun earlierActivity(type: String, duration: Long?): StatisticsEarlierActivity =
    StatisticsEarlierActivity(
        type = EntryType.valueOf(type.uppercase()),
        durationMillis = duration ?: 0L,
    )

private fun activityBucket(type: String, localDate: String, duration: Long?): StatisticsActivityBucket =
    StatisticsActivityBucket(
        type = EntryType.valueOf(type.uppercase()),
        localDate = localDate,
        durationMillis = duration ?: 0L,
    )

private fun completionBucket(type: String, localDate: String, count: Long): StatisticsCompletionBucket =
    StatisticsCompletionBucket(
        type = EntryType.valueOf(type.uppercase()),
        localDate = localDate,
        count = count,
    )

private fun activitySegment(
    type: String,
    localDate: String,
    startedAt: Long,
    endedAt: Long,
    duration: Long,
    timeZoneId: String,
): StatisticsActivitySegment = StatisticsActivitySegment(
    type = EntryType.valueOf(type.uppercase()),
    localDate = localDate,
    startedAtEpochMillis = startedAt,
    endedAtEpochMillis = endedAt,
    durationMillis = duration,
    timeZoneId = timeZoneId,
)
