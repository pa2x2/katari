package mihon.entry.interactions

import kotlinx.coroutines.flow.Flow
import tachiyomi.domain.entry.model.Entry

/** Collapses a caller-supplied Library population without exposing unrelated membership. */
interface EntryMergeLibraryGroupingFeature {
    suspend fun groupLibraryEntries(
        profileId: Long,
        entries: List<Entry>,
    ): EntryMergeLibraryGroupingProjection

    fun observeLibraryGroups(
        profileId: Long,
        entries: Flow<List<Entry>>,
    ): Flow<EntryMergeLibraryGroupingProjection>
}

data class EntryMergeLibraryGroupingProjection(
    val profileId: Long,
    val groups: List<EntryMergeLibraryGroup>,
)

/** Every supplied Entry appears exactly once; standalone groups contain one Entry. */
data class EntryMergeLibraryGroup(
    val visibleEntry: Entry,
    val orderedEntries: List<Entry>,
)
