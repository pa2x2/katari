package mihon.entry.interactions

import mihon.entry.interactions.host.EntryMergeHost
import mihon.entry.interactions.host.EntryMergeHostTransitionResult
import mihon.entry.interactions.host.EntryMergeMembershipSnapshot
import mihon.entry.interactions.host.EntryMergeProfileMoveHostTransition
import tachiyomi.domain.entry.model.Entry

internal class EntryMergeProfileMoveCoordinator(
    private val host: EntryMergeHost,
) : EntryMergeProfileMoveFeature {
    override suspend fun prepare(
        sourceProfileId: Long,
        selectedVisibleEntryIds: List<Long>,
    ): EntryMergeProfileMovePreparationResult {
        val profile = host.profile(sourceProfileId)
        val unitsByKey = linkedMapOf<String, EntryMergeProfileMoveUnit>()
        val sourceGroups = linkedMapOf<Long, EntryMergeMembershipSnapshot>()
        val standaloneIds = mutableSetOf<Long>()

        selectedVisibleEntryIds.distinct().forEach { selectedId ->
            val selectedEntry = profile.entries(listOf(selectedId)).singleOrNull() ?: return@forEach
            val membership = profile.membership(selectedId)
            if (membership == null) {
                standaloneIds += selectedEntry.id
                unitsByKey.putIfAbsent("entry:${selectedEntry.id}", EntryMergeProfileMoveUnit(listOf(selectedEntry)))
            } else {
                sourceGroups[membership.targetEntryId] = membership
                val entries = profile.entries(membership.orderedEntryIds)
                if (entries.size == membership.orderedEntryIds.size) {
                    unitsByKey.putIfAbsent(
                        "group:${membership.targetEntryId}",
                        EntryMergeProfileMoveUnit(entries),
                    )
                }
            }
        }
        if (unitsByKey.isEmpty()) return EntryMergeProfileMovePreparationResult.Empty

        val reference = FeatureEntryMergeProfileMoveReference(
            sessionId = newEntryMergeSessionId(),
            sourceProfileId = sourceProfileId,
            sourceGroups = sourceGroups.values.toList(),
            standaloneSourceEntryIds = standaloneIds,
            sourceEntries = unitsByKey.values.flatMap { it.entries }.associateBy(Entry::id),
        )
        return EntryMergeProfileMovePreparationResult.Ready(reference, unitsByKey.values.toList())
    }

    override suspend fun inspectDestination(
        reference: EntryMergeProfileMoveReference,
        destinationProfileId: Long,
        destinationEntryIds: List<Long>,
    ): EntryMergeProfileMoveDestinationResult {
        val prepared = reference as? FeatureEntryMergeProfileMoveReference
            ?: return EntryMergeProfileMoveDestinationResult.InvalidReference
        if (prepared.destinationProfileId != null || destinationProfileId == prepared.sourceProfileId) {
            return EntryMergeProfileMoveDestinationResult.InvalidReference
        }
        val profile = host.profile(destinationProfileId)
        val groups = linkedMapOf<Long, EntryMergeMembershipSnapshot>()
        val standalone = mutableSetOf<Long>()
        destinationEntryIds.distinct().forEach { entryId ->
            val entry = profile.entries(listOf(entryId)).singleOrNull()
                ?: return EntryMergeProfileMoveDestinationResult.InvalidReference
            val membership = profile.membership(entry.id)
            if (membership == null) {
                standalone += entry.id
            } else {
                groups[membership.targetEntryId] = membership
            }
        }
        return EntryMergeProfileMoveDestinationResult.Ready(
            reference = prepared.copy(
                destinationProfileId = destinationProfileId,
                destinationGroups = groups.values.toList(),
                standaloneDestinationEntryIds = standalone,
                inspectedDestinationEntryIds = destinationEntryIds.toSet(),
            ),
            mergeAffectedEntryIds = destinationEntryIds.filterTo(mutableSetOf()) { entryId ->
                groups.values.any { entryId in it.orderedEntryIds }
            },
        )
    }

    override suspend fun begin(intent: EntryMergeProfileMoveIntent): EntryMergeProfileMoveExecutionResult {
        val transition = transition(intent) ?: return EntryMergeProfileMoveExecutionResult.Conflict
        return host.profile(transition.sourceProfileId).beginProfileMove(transition).toProfileMoveResult()
    }

    override suspend fun complete(intent: EntryMergeProfileMoveIntent): EntryMergeProfileMoveExecutionResult {
        val transition = transition(intent) ?: return EntryMergeProfileMoveExecutionResult.Conflict
        return host.profile(transition.sourceProfileId).completeProfileMove(transition).toProfileMoveResult()
    }

    private fun transition(intent: EntryMergeProfileMoveIntent): EntryMergeProfileMoveHostTransition? {
        val reference = intent.reference as? FeatureEntryMergeProfileMoveReference
            ?: return null
        if (reference.destinationProfileId != intent.destinationProfileId) {
            return null
        }
        if (intent.destinationEntryIdsBySourceEntryId.keys.any { it !in reference.sourceEntries }) {
            return null
        }
        val movedIds = intent.destinationEntryIdsBySourceEntryId.keys
        if (reference.sourceGroups.any { group ->
                val movedMembers = group.orderedEntryIds.filter(movedIds::contains)
                movedMembers.isNotEmpty() && movedMembers.size != group.orderedEntryIds.size
            }
        ) {
            return null
        }
        if (intent.destinationEntryIdsToDetach.any { it !in reference.inspectedDestinationEntryIds }) {
            return null
        }

        val movedGroups = reference.sourceGroups.filter { group -> group.orderedEntryIds.all(movedIds::contains) }
        val movedStandaloneIds = reference.standaloneSourceEntryIds.filterTo(mutableSetOf(), movedIds::contains)
        val expectedDestinationGroups = reference.destinationGroups.filter { group ->
            group.orderedEntryIds.any(intent.destinationEntryIdsToDetach::contains)
        }
        val expectedStandaloneDestinationIds = reference.standaloneDestinationEntryIds
            .filterTo(mutableSetOf(), intent.destinationEntryIdsToDetach::contains)
        return EntryMergeProfileMoveHostTransition(
            sourceProfileId = reference.sourceProfileId,
            destinationProfileId = intent.destinationProfileId,
            expectedSourceEntries = reference.sourceEntries.filterKeys(movedIds::contains).values.toList(),
            expectedSourceGroups = movedGroups,
            expectedStandaloneEntryIds = movedStandaloneIds,
            expectedDestinationGroups = expectedDestinationGroups,
            expectedStandaloneDestinationEntryIds = expectedStandaloneDestinationIds,
            destinationEntryIdsBySourceEntryId = intent.destinationEntryIdsBySourceEntryId,
            destinationEntryIdsToDetach = intent.destinationEntryIdsToDetach,
        )
    }
}

private fun EntryMergeHostTransitionResult.toProfileMoveResult(): EntryMergeProfileMoveExecutionResult {
    return when (this) {
        is EntryMergeHostTransitionResult.Applied -> EntryMergeProfileMoveExecutionResult.Applied
        EntryMergeHostTransitionResult.Conflict -> EntryMergeProfileMoveExecutionResult.Conflict
        is EntryMergeHostTransitionResult.OperationalFailure ->
            EntryMergeProfileMoveExecutionResult.OperationalFailure(retryable)
    }
}

private data class FeatureEntryMergeProfileMoveReference(
    val sessionId: String,
    val sourceProfileId: Long,
    val sourceGroups: List<EntryMergeMembershipSnapshot>,
    val standaloneSourceEntryIds: Set<Long>,
    val sourceEntries: Map<Long, Entry>,
    val destinationProfileId: Long? = null,
    val destinationGroups: List<EntryMergeMembershipSnapshot> = emptyList(),
    val standaloneDestinationEntryIds: Set<Long> = emptySet(),
    val inspectedDestinationEntryIds: Set<Long> = emptySet(),
) : EntryMergeProfileMoveReference
