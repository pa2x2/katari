package app.katari.entry.interactions.merge.lifecycle

import app.katari.entry.interactions.lifecycle.profile.EntryProfileMovePlan
import app.katari.entry.interactions.merge.EntryMergeProfileMoveIntent
import app.katari.entry.interactions.merge.EntryMergeProfileMoveReference

internal fun EntryProfileMovePlan.toMergeIntent(
    reference: EntryMergeProfileMoveReference,
) = EntryMergeProfileMoveIntent(
    reference = reference,
    destinationProfileId = destinationProfileId,
    destinationEntryIdsBySourceEntryId = mappings.associate { it.sourceEntry.id to it.destinationEntryId },
    destinationEntryIdsToDetach = destinationEntryIdsToDetach,
)
