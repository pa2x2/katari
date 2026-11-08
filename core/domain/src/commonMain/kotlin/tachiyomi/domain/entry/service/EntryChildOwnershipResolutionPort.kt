package tachiyomi.domain.entry.service

import kotlinx.coroutines.flow.Flow
import tachiyomi.domain.entry.model.Entry

/** Purpose-specific ownership needed to load the children displayed for one Entry. */
interface EntryChildOwnershipResolutionPort {
    suspend fun resolveChildOwnership(profileId: Long, entryId: Long): EntryChildOwnershipResolution

    suspend fun resolveChildOwnership(
        profileId: Long,
        entryIds: Set<Long>,
    ): Map<Long, EntryChildOwnershipResolution> {
        return entryIds.associateWith { entryId -> resolveChildOwnership(profileId, entryId) }
    }

    fun observeChildOwnership(profileId: Long, entryId: Long): Flow<EntryChildOwnershipResolution>
}

data class EntryChildOwnershipResolution(
    val profileId: Long,
    val requestedEntryId: Long,
    val visibleEntryId: Long,
    val orderedOwners: List<Entry>,
)
