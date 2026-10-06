package tachiyomi.domain.statistics.entry

import eu.kanade.tachiyomi.source.entry.EntryType
import kotlinx.coroutines.flow.Flow

interface EntryActivityRepository {
    /** @param monthsSinceLocalDate first day of the earliest month in [EntryActivitySummary.monthlyDurations]. */
    fun subscribeSummary(entryIds: List<Long>, monthsSinceLocalDate: String): Flow<EntryActivitySummary>

    fun subscribeChapterDurations(entryIds: List<Long>): Flow<List<EntryChapterDuration>>

    /** Median time of a finished chapter across the profile's titles of [type]; null when none was timed. */
    suspend fun getTypeChapterPace(profileId: Long, type: EntryType): EntryChapterPace?
}
