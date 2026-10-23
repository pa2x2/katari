package mihon.entry.interactions.merge.source

import mihon.entry.interactions.merge.EntryMergeSourceGroupFeature
import mihon.entry.interactions.merge.EntryMergeSourceGroupResult
import mihon.entry.interactions.merge.host.EntryMergeHost
import mihon.entry.interactions.merge.host.EntryMergeHostTransition
import mihon.entry.interactions.merge.host.EntryMergeHostTransitionResult
import mihon.entry.interactions.merge.host.EntryMergeMembershipSnapshot
import mihon.entry.interactions.merge.host.EntryMergeProfileHost
import tachiyomi.domain.entry.model.Entry
import java.util.UUID

/**
 * Applies source listings to the merges of entries merged by their source, and moves their locked members in and out
 * of the Library with the root.
 */
internal class EntryMergeSourceGroupCoordinator(
    private val host: EntryMergeHost,
) : EntryMergeSourceGroupFeature {

    override suspend fun sync(root: Entry, listed: List<Entry>): EntryMergeSourceGroupResult {
        val profile = host.profile(root.profileId)
        val current = profile.entries(listOf(root.id)).singleOrNull()
            ?.takeIf(Entry::mergedBySource)
            ?: return EntryMergeSourceGroupResult.Conflict
        val listing = listed
            .filter { it.id > 0L && it.id != current.id && !it.mergedBySource && it.type == current.type }
            .map(Entry::id)
            .distinct()
        val group = profile.sourceGroupOf(current, profile.membership(current.id))
            ?: return EntryMergeSourceGroupResult.Conflict
        val previouslyLocked = group.lockedIds - current.id
        val expectedGroups = linkedMapOf<Long, EntryMergeMembershipSnapshot>()
        group.membership?.let { expectedGroups[it.targetEntryId] = it }
        val claimed = mutableSetOf<Long>()
        listing.forEach { entryId ->
            val other = profile.membership(entryId)
            when {
                other == null || other.targetEntryId == current.id -> claimed += entryId
                // Between merged entries the latest listing wins; a merge a user made keeps its titles
                profile.isLockedIn(other, entryId) -> {
                    claimed += entryId
                    expectedGroups[other.targetEntryId] = other
                }
            }
        }
        val unlinked = previouslyLocked - claimed
        val order = sourceGroupOrder(group.memberIds, current.id, listing, claimed, unlinked)
        val groups = buildList {
            if (order.size >= 2) add(EntryMergeMembershipSnapshot(current.profileId, current.id, order))
            expectedGroups.values.filter { it.targetEntryId != current.id }.forEach { other ->
                val remaining = other.orderedEntryIds - claimed
                if (remaining.size >= 2) {
                    val target = other.targetEntryId.takeIf(remaining::contains) ?: remaining.first()
                    add(EntryMergeMembershipSnapshot(current.profileId, target, remaining))
                }
            }
        }
        val favorites = profile.entries((claimed + unlinked).toList()).filter(Entry::favorite).map(Entry::id).toSet()
        return apply(
            profile = profile,
            root = current,
            expectedListing = group.listing,
            listing = listing,
            expectedGroups = expectedGroups.values.toList(),
            groups = groups,
            libraryAdditions = if (current.favorite) claimed - favorites else emptySet(),
            libraryRemovals = if (current.favorite) {
                unlinked.filterTo(
                    mutableSetOf(),
                    favorites::contains,
                )
            } else {
                emptySet()
            },
        )
    }

    /** Brings [root]'s locked members into the Library after [root] joined it. */
    internal suspend fun addedToLibrary(root: Entry): EntryMergeSourceGroupResult {
        val profile = host.profile(root.profileId)
        val current = profile.entries(listOf(root.id)).singleOrNull() ?: return EntryMergeSourceGroupResult.Conflict
        if (!current.mergedBySource || !current.favorite) return EntryMergeSourceGroupResult.Applied
        val group = profile.sourceGroupOf(current, profile.membership(current.id))
            ?: return EntryMergeSourceGroupResult.Conflict
        val missing = profile.entries((group.lockedIds - current.id).toList())
            .filterNot(Entry::favorite)
            .mapTo(mutableSetOf(), Entry::id)
        if (missing.isEmpty()) return EntryMergeSourceGroupResult.Applied
        val groups = listOfNotNull(group.membership)
        return apply(profile, current, group.listing, group.listing, groups, groups, missing, emptySet())
    }

    /**
     * Takes [removedIds] out of [group] as they leave the Library. When the root leaves, its locked members leave with
     * it and stay in its merge, ready for when it comes back; members a user added go on without it. A locked member
     * can't leave on its own.
     */
    internal suspend fun leaveLibrary(
        group: EntryMergeSourceGroup,
        removedIds: Set<Long>,
    ): EntryMergeHostTransitionResult {
        val membership = group.membership ?: return EntryMergeHostTransitionResult.Applied(null)
        val root = group.root
        if (root.id !in removedIds) {
            if (removedIds.any { it in group.lockedIds }) return EntryMergeHostTransitionResult.Conflict
            val remaining = membership.orderedEntryIds.filterNot(removedIds::contains)
            return replace(group, listOfNotNull(remaining.toGroup(root.profileId, root.id)), emptySet())
        }
        val locked = membership.orderedEntryIds.filter { it in group.lockedIds }
        val userRemaining = membership.orderedEntryIds.filter { it !in group.lockedIds && it !in removedIds }
        val profile = host.profile(root.profileId)
        val lockedFavorites = profile.entries(locked - root.id).filter(Entry::favorite).mapTo(mutableSetOf(), Entry::id)
        return replace(
            group = group,
            groups = listOfNotNull(
                locked.toGroup(root.profileId, root.id),
                userRemaining.toGroup(root.profileId, userRemaining.firstOrNull()),
            ),
            libraryRemovals = lockedFavorites,
        )
    }

    private suspend fun replace(
        group: EntryMergeSourceGroup,
        groups: List<EntryMergeMembershipSnapshot>,
        libraryRemovals: Set<Long>,
    ): EntryMergeHostTransitionResult {
        return host.profile(group.root.profileId).applyTransition(
            EntryMergeHostTransition.SyncSourceGroup(
                operationId = UUID.randomUUID().toString(),
                profileId = group.root.profileId,
                rootEntryId = group.root.id,
                expectedListing = group.listing,
                listing = group.listing,
                expectedGroups = listOfNotNull(group.membership),
                groups = groups,
                libraryAdditionEntryIds = emptySet(),
                libraryAdditionCategoryIds = emptyList(),
                libraryRemovalEntryIds = libraryRemovals,
            ),
        )
    }

    private suspend fun apply(
        profile: EntryMergeProfileHost,
        root: Entry,
        expectedListing: List<Long>,
        listing: List<Long>,
        expectedGroups: List<EntryMergeMembershipSnapshot>,
        groups: List<EntryMergeMembershipSnapshot>,
        libraryAdditions: Set<Long>,
        libraryRemovals: Set<Long>,
    ): EntryMergeSourceGroupResult {
        val result = profile.applyTransition(
            EntryMergeHostTransition.SyncSourceGroup(
                operationId = UUID.randomUUID().toString(),
                profileId = root.profileId,
                rootEntryId = root.id,
                expectedListing = expectedListing,
                listing = listing,
                expectedGroups = expectedGroups,
                groups = groups,
                libraryAdditionEntryIds = libraryAdditions,
                libraryAdditionCategoryIds = if (libraryAdditions.isEmpty()) {
                    emptyList()
                } else {
                    profile.categoryIds(root.id)
                },
                libraryRemovalEntryIds = libraryRemovals,
            ),
        )
        return when (result) {
            is EntryMergeHostTransitionResult.Applied -> EntryMergeSourceGroupResult.Applied
            EntryMergeHostTransitionResult.Conflict -> EntryMergeSourceGroupResult.Conflict
            is EntryMergeHostTransitionResult.OperationalFailure ->
                EntryMergeSourceGroupResult.OperationalFailure(result.retryable)
        }
    }
}

private fun List<Long>.toGroup(profileId: Long, targetEntryId: Long?): EntryMergeMembershipSnapshot? {
    return if (size >= 2 &&
        targetEntryId != null
    ) {
        EntryMergeMembershipSnapshot(profileId, targetEntryId, this)
    } else {
        null
    }
}
