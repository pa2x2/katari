package mihon.entry.interactions.merge.lifecycle

import mihon.entry.interactions.lifecycle.profile.EntryProfileMovePlan
import mihon.entry.interactions.merge.EntryMergeProfileMoveIntent
import mihon.entry.interactions.merge.EntryMergeProfileMoveReference

internal fun EntryProfileMovePlan.toMergeIntent(
    reference: EntryMergeProfileMoveReference,
) = EntryMergeProfileMoveIntent(
    reference = reference,
    destinationProfileId = destinationProfileId,
    destinationEntryIdsBySourceEntryId = mappings.associate { it.sourceEntry.id to it.destinationEntryId },
    destinationEntryIdsToDetach = destinationEntryIdsToDetach,
)
