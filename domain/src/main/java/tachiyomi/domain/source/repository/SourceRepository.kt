package tachiyomi.domain.source.repository

import kotlinx.coroutines.flow.Flow
import tachiyomi.domain.source.model.Source
import tachiyomi.domain.source.model.SourceWithCount

interface SourceRepository {

    fun getConfigurableSourceIds(): List<Long>

    fun getConfigurableSourceKeys(): List<String>

    fun getSources(): Flow<List<Source>>

    fun getOnlineSources(): Flow<List<Source>>

    fun getSourcesWithFavoriteCount(): Flow<List<Pair<Source, Long>>>

    fun getSourcesWithNonLibraryEntries(): Flow<List<SourceWithCount>>
}
