package mihon.entry.interactions.host

import tachiyomi.domain.entry.model.Entry

data class EntryMergeProfileMoveHostTransition(
    val sourceProfileId: Long,
    val destinationProfileId: Long,
    val expectedSourceEntries: List<Entry>,
    val expectedSourceGroups: List<EntryMergeMembershipSnapshot>,
    val expectedStandaloneEntryIds: Set<Long>,
    val expectedDestinationGroups: List<EntryMergeMembershipSnapshot>,
    val expectedStandaloneDestinationEntryIds: Set<Long>,
    val destinationEntryIdsBySourceEntryId: Map<Long, Long>,
    val destinationEntryIdsToDetach: Set<Long>,
)
