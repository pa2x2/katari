package tachiyomi.data.statistics

import eu.kanade.tachiyomi.source.entry.EntryType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import tachiyomi.data.DatabaseHandler
import tachiyomi.domain.statistics.model.StatisticsActivityBucket
import tachiyomi.domain.statistics.model.StatisticsActivitySnapshot
import tachiyomi.domain.statistics.model.StatisticsCompletionBucket
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
            subscribeTopEntries(profileId, startLocalDate),
            handler.subscribeToOneOrNull { activityQueries.getStatisticsEpoch(profileId) },
        ) { activity, completions, topEntries, trackingStartedAt ->
            StatisticsActivitySnapshot(
                profileId = profileId,
                trackingStartedAtEpochMillis = trackingStartedAt,
                activity = activity,
                completions = completions,
                topEntries = topEntries,
            )
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
}
