package mihon.desktop.merge

import dev.zacsweers.metro.Inject
import mihon.entry.interactions.merge.EntryMergeMetadataRefreshFeature
import mihon.entry.interactions.merge.EntryMergeMetadataRefreshOwners
import tachiyomi.domain.entry.model.Entry

/** Every entry outside a merge refreshes its own details, so each entry is its only owner. */
@Inject
class NoMergeMetadataRefresh : EntryMergeMetadataRefreshFeature {
    override suspend fun resolveOwners(entry: Entry): EntryMergeMetadataRefreshOwners =
        EntryMergeMetadataRefreshOwners(visibleEntryId = entry.id, orderedOwners = listOf(entry))
}
