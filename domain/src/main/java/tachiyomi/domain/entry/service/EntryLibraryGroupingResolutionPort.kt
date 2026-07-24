package tachiyomi.domain.entry.service

import kotlinx.coroutines.flow.Flow
import tachiyomi.domain.entry.model.Entry

/** Purpose-specific grouping of a supplied Library population. It exposes no membership persistence operations. */
interface EntryLibraryGroupingResolutionPort {
    suspend fun resolveLibraryGrouping(profileId: Long, entries: List<Entry>): EntryLibraryGroupingResolution

    fun observeLibraryGrouping(
        profileId: Long,
        entries: Flow<List<Entry>>,
    ): Flow<EntryLibraryGroupingResolution>
}

data class EntryLibraryGroupingResolution(
    val profileId: Long,
    val groups: List<EntryLibraryGroupResolution>,
)

data class EntryLibraryGroupResolution(
    val visibleEntry: Entry,
    val orderedEntries: List<Entry>,
)
