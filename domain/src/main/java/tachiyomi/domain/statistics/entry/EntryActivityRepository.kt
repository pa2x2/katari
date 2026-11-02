package tachiyomi.domain.statistics.entry

import kotlinx.coroutines.flow.Flow

interface EntryActivityRepository {
    /** @param monthsSinceLocalDate first day of the earliest month in [EntryActivitySummary.monthlyDurations]. */
    fun subscribeSummary(entryIds: List<Long>, monthsSinceLocalDate: String): Flow<EntryActivitySummary>

    fun subscribeChapterDurations(entryIds: List<Long>): Flow<List<EntryChapterDuration>>
}
