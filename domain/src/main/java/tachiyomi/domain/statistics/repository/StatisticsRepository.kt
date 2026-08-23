package tachiyomi.domain.statistics.repository

import eu.kanade.tachiyomi.source.entry.EntryType
import kotlinx.coroutines.flow.Flow
import tachiyomi.domain.statistics.model.StatisticsActivitySnapshot
import tachiyomi.domain.statistics.model.StatisticsEarlierActivityDetails

interface StatisticsRepository {
    /** A null [startLocalDate] requests the profile's complete recorded lifetime. */
    fun subscribeActivity(profileId: Long, startLocalDate: String?): Flow<StatisticsActivitySnapshot>

    suspend fun getEarlierActivityDetails(
        profileId: Long,
        type: EntryType?,
        limit: Long,
    ): StatisticsEarlierActivityDetails
}
