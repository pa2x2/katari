package tachiyomi.domain.source.repository

import kotlinx.coroutines.flow.Flow
import tachiyomi.domain.source.model.Source
import tachiyomi.domain.source.model.SourceWithCount

interface SourceRepository {

    fun getSources(): Flow<List<Source>>

    fun getSourcesWithFavoriteCount(): Flow<List<Pair<Source, Long>>>

    fun getSourcesWithNonLibraryEntries(): Flow<List<SourceWithCount>>
}
