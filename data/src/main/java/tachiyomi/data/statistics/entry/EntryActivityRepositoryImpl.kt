package tachiyomi.data.statistics.entry

import eu.kanade.tachiyomi.source.entry.EntryType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import tachiyomi.data.DatabaseHandler
import tachiyomi.domain.statistics.entry.EntryActivityLastRead
import tachiyomi.domain.statistics.entry.EntryActivityMonth
import tachiyomi.domain.statistics.entry.EntryActivityRepository
import tachiyomi.domain.statistics.entry.EntryActivitySummary
import tachiyomi.domain.statistics.entry.EntryChapterDuration
import tachiyomi.domain.statistics.entry.EntryChapterPace
import tachiyomi.domain.statistics.service.StatisticsActivityPolicy

class EntryActivityRepositoryImpl(
    private val handler: DatabaseHandler,
) : EntryActivityRepository {

    override fun subscribeSummary(entryIds: List<Long>, monthsSinceLocalDate: String): Flow<EntryActivitySummary> {
        val sessions = handler.subscribeToOne {
            entry_activityQueries.entrySessionTotals(
                minimumSessionDurationMillis = StatisticsActivityPolicy.MINIMUM_SESSION_DURATION_MILLIS,
                entryIds = entryIds,
            )
        }
        val legacyDuration = handler.subscribeToOne { entry_activityQueries.entryLegacyDuration(entryIds) }
        val lastRead = handler.subscribeToOneOrNull {
            entry_activityQueries.entryLastRead(entryIds) { name, lastRead ->
                EntryActivityLastRead(
                    chapterName = name,
                    atEpochMillis = checkNotNull(lastRead) { "The query only returns read chapters" },
                )
            }
        }
        val dates = combine(
            handler.subscribeToOne { entry_activityQueries.entryFirstRead(entryIds) },
            handler.subscribeToOne { entry_activityQueries.entryLastCompletion(entryIds) },
            handler.subscribeToOne { entry_activityQueries.entryFirstActivityDate(entryIds) },
            ::Triple,
        )
        val months = handler.subscribeToList {
            entry_activityQueries.entryMonthlyDurations(entryIds, monthsSinceLocalDate) { month, duration ->
                EntryActivityMonth(month = month, durationMillis = duration ?: 0L)
            }
        }
        return combine(
            sessions,
            legacyDuration,
            lastRead,
            dates,
            months,
        ) { sessionTotals, legacy, last, (firstRead, lastCompletion, firstActivityDate), monthly ->
            // Every timed second also reaches the legacy per-chapter total, so whatever the legacy total holds beyond
            // the sessions was read before sessions were recorded.
            val timed = sessionTotals.duration ?: 0L
            val untracked = (legacy - timed).coerceAtLeast(0L)
            EntryActivitySummary(
                totalDurationMillis = timed + untracked,
                untrackedDurationMillis = untracked,
                sessionCount = sessionTotals.qualifying_count ?: 0L,
                sessionDurationMillis = sessionTotals.qualifying_duration ?: 0L,
                startedAtEpochMillis = listOfNotNull(sessionTotals.first_started_at, firstRead.first_read).minOrNull(),
                lastRead = last,
                lastCompletionAtEpochMillis = lastCompletion.occurred_at,
                firstActivityLocalDate = firstActivityDate.local_date,
                monthlyDurations = monthly,
            )
        }
    }

    override fun subscribeChapterDurations(entryIds: List<Long>): Flow<List<EntryChapterDuration>> {
        return handler.subscribeToList {
            entry_activityQueries.entryChapterDurations(entryIds) { chapterId, read, duration ->
                EntryChapterDuration(chapterId = chapterId, read = read, durationMillis = duration)
            }
        }
    }

    override suspend fun getTypeChapterPace(profileId: Long, type: EntryType): EntryChapterPace? {
        return handler.awaitOneOrNull {
            entry_activityQueries.typeChapterDurationMedian(profileId, type.name.lowercase()) { duration, count ->
                EntryChapterPace(medianMillis = duration, sampleCount = count)
            }
        }
    }
}
