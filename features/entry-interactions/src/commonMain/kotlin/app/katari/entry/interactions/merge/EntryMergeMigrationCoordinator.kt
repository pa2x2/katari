package app.katari.entry.interactions.merge

import app.katari.entry.interactions.merge.host.EntryMergeHost
import app.katari.entry.interactions.merge.host.EntryMergeHostTransition
import app.katari.entry.interactions.merge.host.EntryMergeHostTransitionResult
import app.katari.entry.interactions.merge.host.EntryMergeMembershipSnapshot
import app.katari.entry.interactions.merge.host.EntryMergeProfileHost
import app.katari.entry.interactions.merge.source.EntryMergeSourceGroup
import app.katari.entry.interactions.merge.source.pairSourceGroupMembers
import app.katari.entry.interactions.merge.source.sourceGroupOf
import app.katari.entry.interactions.merge.source.sourceGroupOrder
import tachiyomi.domain.entry.model.Entry
import java.util.UUID

internal class EntryMergeMigrationCoordinator(
    private val host: EntryMergeHost,
) : EntryMergeMigrationFeature {
    override suspend fun participateInReplacementTransaction(
        intent: EntryMergeMigrationReplacementIntent,
    ): EntryMergeMigrationReplacementResult {
        require(intent.current.profileId == intent.replacement.profileId) {
            "Migration replacement cannot cross profiles"
        }
        require(intent.current.type == intent.replacement.type) {
            "Migration replacement requires matching Entry types"
        }
        if (intent.current.id == intent.replacement.id) return EntryMergeMigrationReplacementResult.Applied

        val profile = host.profile(intent.current.profileId)
        val currentGroup = profile.membership(intent.current.id)
            ?: return EntryMergeMigrationReplacementResult.NoMembership
        val replacementGroup = profile.membership(intent.replacement.id)
        val expectedGroups = listOfNotNull(currentGroup, replacementGroup)
            .distinctBy(EntryMergeMembershipSnapshot::targetEntryId)
        val replacementSourceGroup = profile.sourceGroupOf(intent.replacement, replacementGroup)
        // A merged entry's source placed its locked members, so they stay with it as it leaves the Library, as they
        // would if it were removed; only the members a user added go on with the replacement
        val currentSourceGroup = profile.sourceGroupOf(intent.current, currentGroup)
            ?.takeIf { it.root.id == intent.current.id && it.membership != null }
        val leaving = currentSourceGroup?.let { group ->
            currentGroup.orderedEntryIds.filter {
                it in group.lockedIds && it != intent.current.id && it != intent.replacement.id
            }
        }.orEmpty()
        val carried = currentGroup.copy(orderedEntryIds = currentGroup.orderedEntryIds - leaving.toSet())
        val replacements = buildList {
            if (currentSourceGroup != null && replacementSourceGroup != null) {
                sourceGroupReplacement(intent.current.id, carried, replacementSourceGroup)?.let(::add)
            } else {
                addAll(
                    replacementGroups(
                        currentEntryId = intent.current.id,
                        replacementEntryId = intent.replacement.id,
                        currentGroup = carried,
                        replacementGroup = replacementGroup,
                    ),
                )
            }
            if (leaving.isNotEmpty()) {
                add(
                    EntryMergeMembershipSnapshot(
                        currentGroup.profileId,
                        intent.current.id,
                        listOf(intent.current.id) + leaving,
                    ),
                )
            }
        }
        val operationId = UUID.randomUUID().toString()
        return when (
            val result = profile.applyTransition(
                EntryMergeHostTransition.ReplaceForMigration(
                    operationId = operationId,
                    profileId = intent.current.profileId,
                    expectedGroups = expectedGroups,
                    currentEntryId = intent.current.id,
                    replacementEntryId = intent.replacement.id,
                    replacementGroups = replacements,
                    libraryRemovalEntryIds = profile.entries(leaving)
                        .filter(Entry::favorite)
                        .mapTo(mutableSetOf(), Entry::id),
                ),
            )
        ) {
            is EntryMergeHostTransitionResult.Applied -> EntryMergeMigrationReplacementResult.Applied
            EntryMergeHostTransitionResult.Conflict -> EntryMergeMigrationReplacementResult.Conflict
            is EntryMergeHostTransitionResult.OperationalFailure -> {
                EntryMergeMigrationReplacementResult.OperationalFailure(result.retryable)
            }
        }
    }

    override suspend fun pairLockedMembers(current: Entry, replacement: Entry): List<EntryMergeMemberPair> {
        if (current.profileId != replacement.profileId || current.type != replacement.type) return emptyList()
        val profile = host.profile(current.profileId)
        val currentMembers = profile.lockedMembersInSourceOrder(current) ?: return emptyList()
        val replacementMembers = profile.lockedMembersInSourceOrder(replacement) ?: return emptyList()
        return pairSourceGroupMembers(currentMembers, replacementMembers).map { (old, new) ->
            EntryMergeMemberPair(old, new)
        }
    }

    override suspend fun lockedMembers(root: Entry): List<Entry> =
        host.profile(root.profileId).lockedMembersInSourceOrder(root).orEmpty()

    private suspend fun EntryMergeProfileHost.lockedMembersInSourceOrder(root: Entry): List<Entry>? {
        val group = sourceGroupOf(root, membership(root.id))?.takeIf { it.root.id == root.id } ?: return null
        return entries(group.listing.filter { it in group.lockedIds })
    }
}

/**
 * The merge after a merged entry migrates to [replacement]: its own members, already swapped for their pairs, keep
 * their places under the new root, and the replacement's other locked members join in source order.
 */
private fun sourceGroupReplacement(
    currentEntryId: Long,
    currentGroup: EntryMergeMembershipSnapshot,
    replacement: EntryMergeSourceGroup,
): EntryMergeMembershipSnapshot? {
    val rootId = replacement.root.id
    val current = currentGroup.orderedEntryIds.map { if (it == currentEntryId) rootId else it }.distinct()
    val held = current.toSet() + replacement.memberIds
    val claimed = replacement.listing.filterTo(mutableSetOf(), held::contains)
    val order = sourceGroupOrder(current, rootId, replacement.listing, claimed, unlinked = emptySet())
    return order.takeIf { it.size >= 2 }?.let { EntryMergeMembershipSnapshot(currentGroup.profileId, rootId, it) }
}

internal fun replacementGroups(
    currentEntryId: Long,
    replacementEntryId: Long,
    currentGroup: EntryMergeMembershipSnapshot,
    replacementGroup: EntryMergeMembershipSnapshot?,
): List<EntryMergeMembershipSnapshot> {
    val replacedIds = currentGroup.orderedEntryIds
        .map { entryId -> if (entryId == currentEntryId) replacementEntryId else entryId }
        .distinct()
    val currentReplacement = replacedIds.toSnapshotOrNull(
        profileId = currentGroup.profileId,
        preferredTargetId = if (currentGroup.targetEntryId == currentEntryId) {
            replacementEntryId
        } else {
            currentGroup.targetEntryId
        },
    )
    val otherReplacement = replacementGroup
        ?.takeIf { it.targetEntryId != currentGroup.targetEntryId }
        ?.orderedEntryIds
        ?.filterNot { it == replacementEntryId }
        ?.toSnapshotOrNull(
            profileId = currentGroup.profileId,
            preferredTargetId = replacementGroup.targetEntryId,
        )
    return listOfNotNull(currentReplacement, otherReplacement)
}

private fun List<Long>.toSnapshotOrNull(
    profileId: Long,
    preferredTargetId: Long,
): EntryMergeMembershipSnapshot? {
    if (size < 2) return null
    return EntryMergeMembershipSnapshot(
        profileId = profileId,
        targetEntryId = preferredTargetId.takeIf { it in this } ?: first(),
        orderedEntryIds = this,
    )
}
