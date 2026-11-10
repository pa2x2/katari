package tachiyomi.domain.source.repository

import kotlinx.coroutines.flow.Flow
import tachiyomi.domain.source.model.MissingLibrarySource
import tachiyomi.domain.source.model.Source
import tachiyomi.domain.source.model.SourceWithCount

interface SourceRepository {

    fun getSources(): Flow<List<Source>>

    fun getSourcesWithFavoriteCount(): Flow<List<Pair<Source, Long>>>

    fun getSourcesWithNonLibraryEntries(): Flow<List<SourceWithCount>>

    /** The sources library entries of every profile come from that aren't installed, the local source aside. */
    fun getMissingLibrarySources(): Flow<List<MissingLibrarySource>>

    /** How many library entries each profile has from each source, by source key and then profile ID. */
    fun getLibraryCountsBySource(): Flow<Map<Long, Map<Long, Long>>>

    /** Which of [sourceIds] any profile has entries from, in its library or not. */
    suspend fun getSourcesWithEntries(sourceIds: Collection<Long>): Set<Long>
}
