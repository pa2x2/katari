package mihon.entry.interactions

import mihon.entry.interactions.host.EntryMergeHost
import mihon.entry.interactions.host.EntryMergeHostTransition
import mihon.entry.interactions.host.EntryMergeHostTransitionResult
import tachiyomi.domain.entry.model.Entry

internal class EntryMergeLibraryLifecycleCoordinator(
    private val host: EntryMergeHost,
) : EntryMergeLibraryLifecycleFeature {
    override suspend fun entriesRemovedFromLibrary(entries: List<Entry>): EntryMergeLibraryRemovalResult {
        var changed = 0
        var unresolved = 0
        entries.distinctBy { it.profileId to it.id }
            .groupBy(Entry::profileId)
            .forEach { (profileId, profileEntries) ->
                val profile = host.profile(profileId)
                val removedIds = profileEntries.mapTo(mutableSetOf(), Entry::id)
                val memberships = removedIds.mapNotNull { profile.membership(it) }
                    .distinctBy { it.targetEntryId }
                memberships.forEach { membership ->
                    val remaining = membership.orderedEntryIds.filterNot(removedIds::contains)
                    val replacement = remaining.takeIf { it.size >= 2 }.orEmpty()
                    val result = profile.applyTransition(
                        EntryMergeHostTransition.ChangeExistingGroup(
                            operationId = newEntryMergeSessionId(),
                            profileId = profileId,
                            expected = membership,
                            replacementTargetEntryId = replacement.firstOrNull(),
                            replacementOrderedEntryIds = replacement,
                            visibleEntryId = replacement.firstOrNull(),
                            libraryRemovalEntryIds = emptySet(),
                            consequenceRequests = emptyList(),
                        ),
                    )
                    when (result) {
                        is EntryMergeHostTransitionResult.Applied -> changed++
                        EntryMergeHostTransitionResult.Conflict,
                        is EntryMergeHostTransitionResult.OperationalFailure,
                        -> unresolved++
                    }
                }
            }
        return EntryMergeLibraryRemovalResult(changed, unresolved)
    }
}
