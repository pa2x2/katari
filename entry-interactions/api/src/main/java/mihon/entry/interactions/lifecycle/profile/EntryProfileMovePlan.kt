package mihon.entry.interactions.lifecycle.profile

import mihon.entry.interactions.lifecycle.profile.host.EntryProfileMoveStateRequest
import tachiyomi.domain.entry.model.Entry

data class EntryProfileMoveMapping(
    val sourceEntry: Entry,
    val destinationEntryId: Long,
)

data class EntryProfileMovePlan(
    val sourceProfileId: Long,
    val destinationProfileId: Long,
    val destinationCategoryId: Long?,
    val mappings: List<EntryProfileMoveMapping>,
    val movedEntries: List<Entry>,
    val removedEntries: List<Entry>,
    val destinationEntryIdsToDetach: Set<Long>,
)

fun EntryProfileMovePlan.stateRequest() = EntryProfileMoveStateRequest(
    sourceProfileId = sourceProfileId,
    destinationProfileId = destinationProfileId,
    entryIds = movedEntries.map(Entry::id),
)
