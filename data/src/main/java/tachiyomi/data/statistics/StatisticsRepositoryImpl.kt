package tachiyomi.data.statistics

import eu.kanade.tachiyomi.source.entry.EntryType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import tachiyomi.data.DatabaseHandler
import tachiyomi.domain.statistics.model.StatisticsActivityBucket
import tachiyomi.domain.statistics.model.StatisticsActivitySnapshot
import tachiyomi.domain.statistics.model.StatisticsActivityTimeline
import tachiyomi.domain.statistics.model.StatisticsCompletionBucket
import tachiyomi.domain.statistics.model.StatisticsEarlierActivity
import tachiyomi.domain.statistics.model.StatisticsEarlierActivityDetails
import tachiyomi.domain.statistics.model.StatisticsSessionSummary
import tachiyomi.domain.statistics.model.StatisticsTopEntry
import tachiyomi.domain.statistics.repository.StatisticsRepository

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
            statisticsViewQueries.earlierActivityEntries(
                profileId = profileId,
                type = type?.name?.lowercase(),
                limit = limit,
            ) { entryId, entryType, title, duration ->
                StatisticsTopEntry(
                    entryId = entryId,
                    type = EntryType.valueOf(entryType.uppercase()),
                    title = title,
                    durationMillis = duration ?: 0L,
                )
            }
        }
        return StatisticsEarlierActivityDetails(totals = totals, topEntries = topEntries)
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
                ::Pair,
            ),
            handler.subscribeToOneOrNull { activityQueries.getStatisticsEpoch(profileId) },
            subscribeEarlierActivity(profileId),
        ) { activity, completions, (topEntries, sessions), trackingStartedAt, earlierActivity ->
            StatisticsActivitySnapshot(
                profileId = profileId,
                trackingStartedAtEpochMillis = trackingStartedAt,
                activity = activity,
                completions = completions,
                topEntries = topEntries,
                sessions = sessions,
                earlierActivity = earlierActivity,
            )
        }
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

    private fun subscribeEarlierActivity(profileId: Long): Flow<List<StatisticsEarlierActivity>> {
        val legacy = handler.subscribeToList {
            historyQueries.getReadDurationByType(profileId) { type, duration ->
                EntryType.valueOf(type.uppercase()) to duration
            }
        }
        val detailed = handler.subscribeToList {
            statisticsViewQueries.detailedLifetimeDurationByType(profileId) { type, duration ->
                EntryType.valueOf(type.uppercase()) to (duration ?: 0L)
            }
        }
        return combine(legacy, detailed, ::calculateEarlierActivity)
    }

    private suspend fun getEarlierActivity(profileId: Long): List<StatisticsEarlierActivity> {
        val legacyRows = handler.awaitList {
            historyQueries.getReadDurationByType(profileId) { type, duration ->
                EntryType.valueOf(type.uppercase()) to duration
            }
        }
        val detailedRows = handler.awaitList {
            statisticsViewQueries.detailedLifetimeDurationByType(profileId) { type, duration ->
                EntryType.valueOf(type.uppercase()) to (duration ?: 0L)
            }
        }
        return calculateEarlierActivity(legacyRows, detailedRows)
    }

    private fun subscribeActivityRows(
        profileId: Long,
        startLocalDate: String?,
        endLocalDate: String?,
    ): Flow<List<StatisticsActivityBucket>> = handler.subscribeToList {
        val mapper = { type: String, localDate: String, duration: Long? ->
            StatisticsActivityBucket(
                type = EntryType.valueOf(type.uppercase()),
                localDate = localDate,
                durationMillis = duration ?: 0L,
            )
        }
        statisticsViewQueries.activityTotalsInWindow(profileId, startLocalDate, endLocalDate, mapper)
    }

    private fun subscribeCompletions(
        profileId: Long,
        startLocalDate: String?,
        endLocalDate: String?,
    ): Flow<List<StatisticsCompletionBucket>> = handler.subscribeToList {
        val mapper = { type: String, localDate: String, count: Long ->
            StatisticsCompletionBucket(
                type = EntryType.valueOf(type.uppercase()),
                localDate = localDate,
                count = count,
            )
        }
        statisticsViewQueries.completionTotalsInWindow(profileId, startLocalDate, endLocalDate, mapper)
    }

    private fun subscribeTopEntries(
        profileId: Long,
        startLocalDate: String?,
        endLocalDate: String?,
    ): Flow<List<StatisticsTopEntry>> = handler.subscribeToList {
        val mapper = { entryId: Long, type: String, title: String, duration: Long? ->
            StatisticsTopEntry(
                entryId = entryId,
                type = EntryType.valueOf(type.uppercase()),
                title = title,
                durationMillis = duration ?: 0L,
            )
        }
        statisticsViewQueries.topActivityEntriesInWindow(profileId, startLocalDate, endLocalDate, mapper)
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
        statisticsViewQueries.sessionSummariesInWindow(profileId, startLocalDate, endLocalDate, mapper)
    }
}

private fun calculateEarlierActivity(
    legacyRows: List<Pair<EntryType, Long>>,
    detailedRows: List<Pair<EntryType, Long>>,
): List<StatisticsEarlierActivity> {
    val detailedByType = detailedRows.toMap()
    return legacyRows.mapNotNull { (type, duration) ->
        (duration - (detailedByType[type] ?: 0L))
            .coerceAtLeast(0L)
            .takeIf { it > 0L }
            ?.let { StatisticsEarlierActivity(type, it) }
    }
}
