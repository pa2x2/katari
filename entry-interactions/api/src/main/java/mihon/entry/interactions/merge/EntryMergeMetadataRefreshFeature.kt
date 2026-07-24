package mihon.entry.interactions

import tachiyomi.domain.entry.model.Entry

interface EntryMergeMetadataRefreshFeature {
    suspend fun resolveOwners(entry: Entry): EntryMergeMetadataRefreshOwners
}

data class EntryMergeMetadataRefreshOwners(
    val visibleEntryId: Long,
    val orderedOwners: List<Entry>,
)
