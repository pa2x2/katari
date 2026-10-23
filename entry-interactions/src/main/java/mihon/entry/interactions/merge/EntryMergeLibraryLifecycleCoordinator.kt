package mihon.entry.interactions.merge

import mihon.entry.interactions.merge.host.EntryMergeHost
import mihon.entry.interactions.merge.host.EntryMergeHostTransition
import mihon.entry.interactions.merge.host.EntryMergeHostTransitionResult
import mihon.entry.interactions.merge.source.EntryMergeSourceGroupCoordinator
import mihon.entry.interactions.merge.source.sourceGroupOf
import tachiyomi.domain.entry.model.Entry

internal class EntryMergeLibraryLifecycleCoordinator(
    private val host: EntryMergeHost,
    private val sourceGroups: EntryMergeSourceGroupCoordinator,
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
                    val target = profile.entries(listOf(membership.targetEntryId)).singleOrNull()
                    val sourceGroup = target?.let { profile.sourceGroupOf(it, membership) }
                    val result = if (sourceGroup != null) {
                        sourceGroups.leaveLibrary(sourceGroup, removedIds)
                    } else {
                        val remaining = membership.orderedEntryIds.filterNot(removedIds::contains)
                        val replacement = remaining.takeIf { it.size >= 2 }.orEmpty()
                        profile.applyTransition(
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
                    }
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

    override suspend fun lockedMembersLeavingAlone(entries: List<Entry>): List<EntryMergeLockedMember> {
        return entries.groupBy(Entry::profileId).flatMap { (profileId, profileEntries) ->
            val profile = host.profile(profileId)
            val leavingIds = profileEntries.mapTo(mutableSetOf(), Entry::id)
            profileEntries.mapNotNull { entry ->
                val group = profile.sourceGroupOf(entry, profile.membership(entry.id)) ?: return@mapNotNull null
                EntryMergeLockedMember(entry, group.root).takeIf {
                    entry.id != group.root.id && entry.id in group.lockedIds && group.root.id !in leavingIds
                }
            }
        }
    }

    override suspend fun entryAddedToLibrary(entry: Entry) {
        val result = sourceGroups.addedToLibrary(entry)
        check(result == EntryMergeSourceGroupResult.Applied) {
            "Merged entry members couldn't join the Library: $result"
        }
    }
}
