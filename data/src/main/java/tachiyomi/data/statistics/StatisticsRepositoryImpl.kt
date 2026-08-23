package tachiyomi.data.statistics

import eu.kanade.tachiyomi.source.entry.EntryType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import tachiyomi.data.DatabaseHandler
import tachiyomi.domain.statistics.model.StatisticsActivityBucket
import tachiyomi.domain.statistics.model.StatisticsActivitySnapshot
import tachiyomi.domain.statistics.model.StatisticsCompletionBucket
import tachiyomi.domain.statistics.model.StatisticsEarlierActivity
import tachiyomi.domain.statistics.model.StatisticsSessionSummary
import tachiyomi.domain.statistics.model.StatisticsTopEntry
import tachiyomi.domain.statistics.repository.StatisticsRepository

class StatisticsRepositoryImpl(
    private val handler: DatabaseHandler,
) : StatisticsRepository {
    override fun subscribeActivity(
        profileId: Long,
        startLocalDate: String?,
    ): Flow<StatisticsActivitySnapshot> {
        return combine(
            subscribeActivityRows(profileId, startLocalDate),
            subscribeCompletions(profileId, startLocalDate),
            combine(
                subscribeTopEntries(profileId, startLocalDate),
                subscribeSessionSummaries(profileId, startLocalDate),
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
        return combine(legacy, detailed) { legacyRows, detailedRows ->
            val detailedByType = detailedRows.toMap()
            legacyRows.mapNotNull { (type, duration) ->
                (duration - (detailedByType[type] ?: 0L))
                    .coerceAtLeast(0L)
                    .takeIf { it > 0L }
                    ?.let { StatisticsEarlierActivity(type, it) }
            }
        }
    }

    private fun subscribeActivityRows(
        profileId: Long,
        startLocalDate: String?,
    ): Flow<List<StatisticsActivityBucket>> = handler.subscribeToList {
        val mapper = { type: String, localDate: String, duration: Long? ->
            StatisticsActivityBucket(
                type = EntryType.valueOf(type.uppercase()),
                localDate = localDate,
                durationMillis = duration ?: 0L,
            )
        }
        if (startLocalDate == null) {
            statisticsViewQueries.activityTotals(profileId, mapper)
        } else {
            statisticsViewQueries.activityTotalsSince(profileId, startLocalDate, mapper)
        }
    }

    private fun subscribeCompletions(
        profileId: Long,
        startLocalDate: String?,
    ): Flow<List<StatisticsCompletionBucket>> = handler.subscribeToList {
        val mapper = { type: String, localDate: String, count: Long ->
            StatisticsCompletionBucket(
                type = EntryType.valueOf(type.uppercase()),
                localDate = localDate,
                count = count,
            )
        }
        if (startLocalDate == null) {
            statisticsViewQueries.completionTotals(profileId, mapper)
        } else {
            statisticsViewQueries.completionTotalsSince(profileId, startLocalDate, mapper)
        }
    }

    private fun subscribeTopEntries(
        profileId: Long,
        startLocalDate: String?,
    ): Flow<List<StatisticsTopEntry>> = handler.subscribeToList {
        val mapper = { entryId: Long, type: String, title: String, duration: Long? ->
            StatisticsTopEntry(
                entryId = entryId,
                type = EntryType.valueOf(type.uppercase()),
                title = title,
                durationMillis = duration ?: 0L,
            )
        }
        if (startLocalDate == null) {
            statisticsViewQueries.topActivityEntries(profileId, mapper)
        } else {
            statisticsViewQueries.topActivityEntriesSince(profileId, startLocalDate, mapper)
        }
    }

    private fun subscribeSessionSummaries(
        profileId: Long,
        startLocalDate: String?,
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
        if (startLocalDate == null) {
            statisticsViewQueries.sessionSummaries(profileId, mapper)
        } else {
            statisticsViewQueries.sessionSummariesSince(profileId, startLocalDate, mapper)
        }
    }
}
