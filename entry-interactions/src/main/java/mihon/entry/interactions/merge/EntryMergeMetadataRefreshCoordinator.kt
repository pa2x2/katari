package mihon.entry.interactions

import mihon.entry.interactions.host.EntryMergeHost
import tachiyomi.domain.entry.model.Entry

internal class EntryMergeMetadataRefreshCoordinator(
    private val host: EntryMergeHost,
) : EntryMergeMetadataRefreshFeature {
    override suspend fun resolveOwners(entry: Entry): EntryMergeMetadataRefreshOwners {
        val profile = host.profile(entry.profileId)
        val membership = profile.membership(entry.id)
        if (membership == null) {
            return EntryMergeMetadataRefreshOwners(entry.id, listOf(entry))
        }
        val owners = profile.entries(membership.orderedEntryIds)
        return EntryMergeMetadataRefreshOwners(
            visibleEntryId = membership.targetEntryId,
            orderedOwners = owners.ifEmpty { listOf(entry) },
        )
    }
}
