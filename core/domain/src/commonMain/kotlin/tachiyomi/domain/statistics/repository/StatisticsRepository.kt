package tachiyomi.domain.statistics.repository

import eu.kanade.tachiyomi.source.entry.EntryType
import kotlinx.coroutines.flow.Flow
import tachiyomi.domain.statistics.model.StatisticsActivitySnapshot
import tachiyomi.domain.statistics.model.StatisticsActivityTimeline
import tachiyomi.domain.statistics.model.StatisticsEarlierActivityDetails
import tachiyomi.domain.statistics.model.StatisticsTopEntry

interface StatisticsRepository {
    /**
     * Subscribes to activity inside an inclusive local-date window.
     *
     * A null [startLocalDate] starts at the profile's first recorded activity. A null [endLocalDate]
     * preserves the unbounded legacy call for callers that do not navigate historical windows.
     */
    fun subscribeActivity(
        profileId: Long,
        startLocalDate: String?,
        endLocalDate: String? = null,
    ): Flow<StatisticsActivitySnapshot>

    /** Subscribes to the graph-only timeline inside an inclusive local-date window. */
    fun subscribeActivityTimeline(
        profileId: Long,
        startLocalDate: String?,
        endLocalDate: String,
    ): Flow<StatisticsActivityTimeline>

    /** Ranks every entry with qualifying timed activity in the window, longest first. */
    suspend fun getTopEntriesPage(
        profileId: Long,
        type: EntryType?,
        startLocalDate: String?,
        endLocalDate: String,
        offset: Long,
        limit: Long,
    ): List<StatisticsTopEntry>

    suspend fun getEarlierActivityDetails(
        profileId: Long,
        type: EntryType?,
        limit: Long,
    ): StatisticsEarlierActivityDetails
}
