package mihon.entry.interactions.merge.source

import mihon.entry.interactions.lifecycle.metadata.EntryMetadataLifecycleFeature
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
    private val metadataLifecycle: () -> EntryMetadataLifecycleFeature,
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
        val inLibrary = profile.entries(listing).filter(Entry::favorite).mapTo(mutableSetOf(), Entry::id)
        val claimed = mutableSetOf<Long>()
        // A merged entry outside the Library never takes a Library title, and one in the Library takes its titles
        // back from merges outside it. Otherwise the merge holding a title keeps it, so two merged entries listing it
        // don't trade it on every refresh. A merge a user made always keeps its titles.
        listing.forEach { entryId ->
            val other = profile.membership(entryId)
            when {
                other == null -> if (current.favorite || entryId !in inLibrary) claimed += entryId
                other.targetEntryId == current.id -> claimed += entryId
                current.favorite && profile.lockingRoot(other, entryId)?.favorite == false -> {
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
        val stored = profile.entries((claimed + unlinked).toList()).associateBy(Entry::id)
        val favorites = stored.values.filter(Entry::favorite).map(Entry::id).toSet()
        val listedMembers = listed.filter { it.id in claimed }.distinctBy(Entry::id)
        val titles = listedMembers
            .filter { it.title.isNotBlank() && it.title != stored[it.id]?.title }
            .associate { it.id to it.title }
        // Members named alike under different merged entries, such as "Season 1", keep their downloads apart
        val unnamed = listedMembers.filter { it.downloadTitle == null }
        val downloadTitles = unnamed.associate { it.id to "${current.title} - ${it.title}" }
        val result = apply(
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
            downloadTitles = downloadTitles,
            titles = titles,
            joinedEntryIds = claimed - group.memberIds.toSet(),
        )
        // A member stored before it was listed may already have downloads under its own title
        if (result is EntryMergeSourceGroupResult.Applied) {
            unnamed.forEach { member ->
                val previous = stored[member.id] ?: member
                metadataLifecycle().changed(
                    previous,
                    previous.copy(title = member.title, downloadTitle = downloadTitles.getValue(member.id)),
                )
            }
        }
        return result
    }

    /**
     * Brings [entry]'s members into its merge and the Library after [entry] joined it, from what its source listed
     * last, as a refresh would. That also restores a merge that dissolved when the whole merged entry left the Library.
     * A title that joins the Library on its own leaves the merge outside the Library that held it.
     */
    internal suspend fun addedToLibrary(entry: Entry): EntryMergeSourceGroupResult {
        val profile = host.profile(entry.profileId)
        val current = profile.entries(listOf(entry.id)).singleOrNull() ?: return EntryMergeSourceGroupResult.Conflict
        if (!current.favorite) return EntryMergeSourceGroupResult.Applied(emptySet())
        if (!current.mergedBySource) return leaveMergeOutsideLibrary(profile, current)
        val listing = profile.sourceListing(current.id)
        if (listing.isEmpty()) return EntryMergeSourceGroupResult.Applied(emptySet())
        return sync(current, profile.entries(listing))
    }

    private suspend fun leaveMergeOutsideLibrary(
        profile: EntryMergeProfileHost,
        member: Entry,
    ): EntryMergeSourceGroupResult {
        val group = profile.sourceGroupOf(member, profile.membership(member.id))
        val membership = group?.membership
        if (membership == null || group.root.favorite || member.id !in group.lockedIds) {
            return EntryMergeSourceGroupResult.Applied(emptySet())
        }
        val remaining = membership.orderedEntryIds - member.id
        return replace(group, listOfNotNull(remaining.toGroup(group.root.profileId, group.root.id)), emptySet())
            .toSourceGroupResult(emptySet())
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
        // Deleting a merged entry outside the Library leaves its members' Library state alone
        val lockedFavorites = if (root.favorite) {
            profile.entries(locked - root.id).filter(Entry::favorite).mapTo(mutableSetOf(), Entry::id)
        } else {
            emptySet()
        }
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
        downloadTitles: Map<Long, String>,
        titles: Map<Long, String>,
        joinedEntryIds: Set<Long>,
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
                downloadTitles = downloadTitles,
                titles = titles,
            ),
        )
        return result.toSourceGroupResult(joinedEntryIds)
    }
}

private fun EntryMergeHostTransitionResult.toSourceGroupResult(joinedEntryIds: Set<Long>): EntryMergeSourceGroupResult {
    return when (this) {
        is EntryMergeHostTransitionResult.Applied -> EntryMergeSourceGroupResult.Applied(joinedEntryIds)
        EntryMergeHostTransitionResult.Conflict -> EntryMergeSourceGroupResult.Conflict
        is EntryMergeHostTransitionResult.OperationalFailure -> EntryMergeSourceGroupResult.OperationalFailure(
            retryable,
        )
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
