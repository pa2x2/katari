package mihon.desktop.merge

import dev.zacsweers.metro.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import tachiyomi.domain.entry.model.Entry
import tachiyomi.domain.entry.repository.EntryRepository
import tachiyomi.domain.entry.service.EntryChildOwnershipResolution
import tachiyomi.domain.entry.service.EntryChildOwnershipResolutionPort

/**
 * Desktop doesn't host merged entries yet, so each entry owns its own chapters, as an unmerged entry does on Android.
 * Stands in until desktop hosts the merge feature.
 */
@Inject
class NoMergeChildOwnership(private val entryRepository: EntryRepository) : EntryChildOwnershipResolutionPort {
    override suspend fun resolveChildOwnership(profileId: Long, entryId: Long): EntryChildOwnershipResolution =
        ownedBy(profileId, entryId, entryRepository.getEntryById(entryId, profileId))

    override fun observeChildOwnership(profileId: Long, entryId: Long): Flow<EntryChildOwnershipResolution> =
        entryRepository.getEntryByIdAsFlow(entryId).map { entry ->
            ownedBy(profileId, entryId, entry.takeIf { it.profileId == profileId })
        }

    private fun ownedBy(profileId: Long, entryId: Long, entry: Entry?) = EntryChildOwnershipResolution(
        profileId = profileId,
        requestedEntryId = entryId,
        visibleEntryId = entryId,
        orderedOwners = listOfNotNull(entry),
    )
}
