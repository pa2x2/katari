package mihon.entry.interactions.merge.backup

import kotlinx.serialization.Serializable
import mihon.entry.interactions.merge.EntryMergeBackupIdentity
import mihon.entry.interactions.merge.asEntry
import mihon.entry.interactions.merge.backupIdentity
import mihon.entry.interactions.merge.host.EntryMergeHost
import mihon.entry.interactions.merge.host.EntryMergeHostTransition
import mihon.entry.interactions.merge.host.EntryMergeHostTransitionResult
import mihon.entry.interactions.merge.newEntryMergeSessionId

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
    suspend fun snapshot(profileId: Long, entryId: Long): EntryMergeSourceListingBackupState? {
        val profile = host.profile(profileId)
        val root = profile.entries(listOf(entryId)).singleOrNull()?.takeIf { it.mergedBySource } ?: return null
        val listing = profile.entries(profile.sourceListing(root.id)).map { it.backupIdentity() }
        return EntryMergeSourceListingBackupState(listing).takeIf { listing.isNotEmpty() }
    }

    /**
     * Restores each root's listing once every restored entry exists. A listed title that wasn't restored is left out
     * until the next refresh lists it again. Returns the roots whose listing couldn't be restored.
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
            val listedIds = listing
                .mapNotNull { profile.resolveEntryIdentity(it.asEntry(profileId))?.id }
                .filter { it != root.id }
                .distinct()
            val groups = listOfNotNull(profile.membership(root.id))
            val result = profile.applyTransition(
                EntryMergeHostTransition.SyncSourceGroup(
                    operationId = newEntryMergeSessionId(),
                    profileId = profileId,
                    rootEntryId = root.id,
                    expectedListing = profile.sourceListing(root.id),
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
