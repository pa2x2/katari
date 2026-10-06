package tachiyomi.data.statistics.recap

import app.cash.sqldelight.async.coroutines.awaitAsList
import app.cash.sqldelight.async.coroutines.awaitAsOneOrNull
import eu.kanade.tachiyomi.source.entry.EntryType
import kotlinx.coroutines.flow.Flow
import tachiyomi.data.DatabaseHandler
import tachiyomi.domain.entry.model.EntryCover
import tachiyomi.domain.statistics.recap.StatisticsRecapActivity
import tachiyomi.domain.statistics.recap.StatisticsRecapCompletion
import tachiyomi.domain.statistics.recap.StatisticsRecapEntry
import tachiyomi.domain.statistics.recap.StatisticsRecapPeriodTotals
import tachiyomi.domain.statistics.recap.StatisticsRecapRepository
import tachiyomi.domain.statistics.recap.StatisticsRecapSegment
import tachiyomi.domain.statistics.service.StatisticsActivityPolicy

class StatisticsRecapRepositoryImpl(
    private val handler: DatabaseHandler,
) : StatisticsRecapRepository {

    override suspend fun getActivity(
        profileId: Long,
        startLocalDate: String,
        endLocalDate: String,
    ): StatisticsRecapActivity = handler.await(inTransaction = true) {
        val minimum = StatisticsActivityPolicy.MINIMUM_SESSION_DURATION_MILLIS
        val queries = statisticsRecapQueries
        StatisticsRecapActivity(
            segments = queries.recapSegments(
                profileId,
                minimum,
                startLocalDate,
                endLocalDate,
            ) { entryId, itemNumber, localDate, startedAt, endedAt, duration, timeZoneId ->
                StatisticsRecapSegment(
                    entryId = entryId,
                    // Chapters store an unrecognised number as -1.
                    itemNumber = itemNumber?.takeIf { it >= 0 },
                    localDate = localDate,
                    startedAtEpochMillis = startedAt,
                    endedAtEpochMillis = endedAt,
                    durationMillis = duration,
                    timeZoneId = timeZoneId,
                )
            }.awaitAsList(),
            entries = queries.recapEntries(
                profileId,
                minimum,
                startLocalDate,
                endLocalDate,
            ) { id, type, title, genres, thumbnailUrl, source, favorite, coverLastModified ->
                StatisticsRecapEntry(
                    id = id,
                    type = EntryType.valueOf(type.uppercase()),
                    title = title,
                    cover = EntryCover(
                        entryId = id,
                        sourceId = source,
                        isFavorite = favorite,
                        url = thumbnailUrl,
                        lastModified = coverLastModified,
                    ),
                    genres = genres.orEmpty(),
                    sourceId = source,
                )
            }.awaitAsList(),
            completions = queries.recapCompletions(
                profileId,
                startLocalDate,
                endLocalDate,
            ) { entryId, type, localDate, count ->
                StatisticsRecapCompletion(
                    entryId = entryId,
                    type = EntryType.valueOf(type.uppercase()),
                    localDate = localDate,
                    count = count,
                )
            }.awaitAsList(),
            finishedEntryIds = queries.recapFinishedEntries(profileId, startLocalDate, endLocalDate)
                .awaitAsList()
                .toSet(),
            firstActiveDateByEntry = queries.recapFirstActiveDates(profileId, startLocalDate, endLocalDate)
                .awaitAsList()
                .associate { it.entry_id to it.first_date },
            profileFirstActiveDate = queries.recapProfileFirstActiveDate(profileId).awaitAsOneOrNull()?.first_date,
        )
    }

    override suspend fun getPeriodTotals(
        profileId: Long,
        startLocalDate: String,
        endLocalDate: String,
    ): StatisticsRecapPeriodTotals {
        val totals = handler.awaitList {
            statisticsRecapQueries.recapEntryTotals(
                profileId,
                StatisticsActivityPolicy.MINIMUM_SESSION_DURATION_MILLIS,
                startLocalDate,
                endLocalDate,
            )
        }
        return StatisticsRecapPeriodTotals(
            durationMillis = totals.sumOf { it.duration ?: 0L },
            topEntryId = totals.firstOrNull { (it.duration ?: 0L) > 0L }?.entry_id,
        )
    }

    override fun subscribeHasActivity(
        profileId: Long,
        startLocalDate: String,
        endLocalDate: String,
    ): Flow<Boolean> = handler.subscribeToOne {
        statisticsRecapQueries.recapHasActivity(
            profileId,
            StatisticsActivityPolicy.MINIMUM_SESSION_DURATION_MILLIS,
            startLocalDate,
            endLocalDate,
        )
    }

    override fun subscribeActiveMonths(profileId: Long): Flow<List<String>> = handler.subscribeToList {
        statisticsRecapQueries.recapActiveMonths(profileId, StatisticsActivityPolicy.MINIMUM_SESSION_DURATION_MILLIS)
    }
}
