package mihon.entry.interactions.merge.backup

import kotlinx.serialization.Serializable
import mihon.entry.interactions.merge.EntryMergeBackupIdentity
import mihon.entry.interactions.merge.asEntry
import mihon.entry.interactions.merge.backupIdentity
import mihon.entry.interactions.merge.host.EntryMergeHost
import mihon.entry.interactions.merge.host.EntryMergeHostTransition
import mihon.entry.interactions.merge.host.EntryMergeHostTransitionResult
import mihon.entry.interactions.merge.newEntryMergeSessionId
import tachiyomi.domain.entry.model.Entry

internal const val ENTRY_MERGE_SOURCE_LISTING_BACKUP_STATE_ID = "entry.merge.source-listing"
internal const val ENTRY_MERGE_SOURCE_LISTING_BACKUP_SCHEMA_VERSION = 1

/** What a merged entry's source listed last, in source order; backed up so its members come back locked. */
@Serializable
internal data class EntryMergeSourceListingBackupState(
    val listing: List<EntryMergeBackupIdentity>,
)

internal class EntryMergeSourceListingBackup(
    private val host: EntryMergeHost,
) {
    /** The titles the merged entries among [entries] list that [entries] leave out. */
    suspend fun companions(profileId: Long, entries: List<Entry>): List<Entry> {
        val profile = host.profile(profileId)
        val includedIds = entries.mapTo(mutableSetOf(), Entry::id)
        val missingIds = entries.filter(Entry::mergedBySource)
            .flatMap { profile.sourceListing(it.id) }
            .filterNot(includedIds::contains)
            .distinct()
        return profile.entries(missingIds)
    }

    /** The listing of each of [entries] that is merged by its source and lists anything, by entry id. */
    suspend fun snapshot(profileId: Long, entries: List<Entry>): Map<Long, EntryMergeSourceListingBackupState> {
        val roots = entries.filter(Entry::mergedBySource)
        if (roots.isEmpty()) return emptyMap()
        val profile = host.profile(profileId)
        val listedIdsByRoot = roots.associate { it.id to profile.sourceListing(it.id) }
        val listed = profile.entries(listedIdsByRoot.values.flatten().distinct()).associateBy(Entry::id)
        return listedIdsByRoot.mapNotNull { (rootId, listedIds) ->
            val listing = listedIds.mapNotNull(listed::get).map { it.backupIdentity() }
            if (listing.isEmpty()) null else rootId to EntryMergeSourceListingBackupState(listing)
        }
            .toMap()
    }

    /**
     * Restores each root's listing once every restored entry exists. A listed title that wasn't restored is left out
     * until the next refresh lists it again; titles this install lists that the backup doesn't stay listed after the
     * backup's, as merge members do, so they stay locked. Returns the roots whose listing couldn't be restored.
     */
    suspend fun restore(
        profileId: Long,
        listings: Map<EntryMergeBackupIdentity, List<EntryMergeBackupIdentity>>,
    ): List<EntryMergeBackupIdentity> {
        val profile = host.profile(profileId)
        return listings.mapNotNull { (rootIdentity, listing) ->
            val root = profile.resolveEntryIdentity(rootIdentity.asEntry(profileId))
                ?.takeIf { it.mergedBySource }
                ?: return@mapNotNull rootIdentity
            val currentListing = profile.sourceListing(root.id)
            val restoredIds = listing.mapNotNull { profile.resolveEntryIdentity(it.asEntry(profileId))?.id }
            val listedIds = (restoredIds + currentListing)
                .filter { it != root.id }
                .distinct()
            val groups = listOfNotNull(profile.membership(root.id))
            val result = profile.applyTransition(
                EntryMergeHostTransition.SyncSourceGroup(
                    operationId = newEntryMergeSessionId(),
                    profileId = profileId,
                    rootEntryId = root.id,
                    expectedListing = currentListing,
                    listing = listedIds,
                    expectedGroups = groups,
                    groups = groups,
                    libraryAdditionEntryIds = emptySet(),
                    libraryAdditionCategoryIds = emptyList(),
                    libraryRemovalEntryIds = emptySet(),
                ),
            )
            rootIdentity.takeUnless { result is EntryMergeHostTransitionResult.Applied }
        }
    }
}
