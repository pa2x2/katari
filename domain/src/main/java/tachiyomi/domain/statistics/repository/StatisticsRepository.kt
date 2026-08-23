package tachiyomi.domain.statistics.repository

import kotlinx.coroutines.flow.Flow
import tachiyomi.domain.statistics.model.StatisticsActivitySnapshot

interface StatisticsRepository {
    /** A null [startLocalDate] requests the profile's complete recorded lifetime. */
    fun subscribeActivity(profileId: Long, startLocalDate: String?): Flow<StatisticsActivitySnapshot>
}
