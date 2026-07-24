package mihon.entry.interactions

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import mihon.entry.interactions.host.EntryMergeHost
import tachiyomi.domain.entry.service.EntryChildOwnershipResolution

internal class EntryMergeChildOwnershipCoordinator(
    private val host: EntryMergeHost,
) : EntryMergeChildOwnershipProjection {
    override suspend fun resolveChildOwnership(profileId: Long, entryId: Long): EntryChildOwnershipResolution {
        val profile = host.profile(profileId)
        val membership = profile.membership(entryId)
        val ownerIds = membership?.orderedEntryIds ?: listOf(entryId)
        return EntryChildOwnershipResolution(
            profileId = profileId,
            requestedEntryId = entryId,
            visibleEntryId = membership?.targetEntryId ?: entryId,
            orderedOwners = profile.entries(ownerIds),
        )
    }

    override fun observeChildOwnership(profileId: Long, entryId: Long): Flow<EntryChildOwnershipResolution> {
        val profile = host.profile(profileId)
        return profile.observeMembership(entryId).map { membership ->
            val ownerIds = membership?.orderedEntryIds ?: listOf(entryId)
            EntryChildOwnershipResolution(
                profileId = profileId,
                requestedEntryId = entryId,
                visibleEntryId = membership?.targetEntryId ?: entryId,
                orderedOwners = profile.entries(ownerIds),
            )
        }
    }
}

internal class EntryMergeDownloadOwnershipCoordinator(
    private val host: EntryMergeHost,
) : EntryMergeDownloadOwnershipProjection {
    override suspend fun resolveDownloadOwners(subject: EntryMergeSubject): EntryMergeDownloadOwners {
        val profile = host.profile(subject.profileId)
        val membership = profile.membership(subject.entryId)
        val ownerIds = membership?.orderedEntryIds ?: listOf(subject.entryId)
        val owners = profile.entries(ownerIds)
        check(owners.map { it.id } == ownerIds) {
            "Download ownership projection could not resolve every Merge owner"
        }
        return EntryMergeDownloadOwners(
            profileId = subject.profileId,
            visibleEntryId = membership?.targetEntryId ?: subject.entryId,
            orderedOwners = owners,
        )
    }
}
