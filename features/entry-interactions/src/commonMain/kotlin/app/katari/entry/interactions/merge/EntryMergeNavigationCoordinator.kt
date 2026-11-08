package app.katari.entry.interactions.merge

import app.katari.entry.interactions.merge.host.EntryMergeHost
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

internal class EntryMergeNavigationCoordinator(
    private val host: EntryMergeHost,
) : EntryMergeNavigationFeature {
    override suspend fun resolveNavigation(subject: EntryMergeSubject): EntryMergeNavigationProjection {
        val membership = host.profile(subject.profileId).membership(subject.entryId)
        return EntryMergeNavigationProjection(subject, membership?.targetEntryId ?: subject.entryId)
    }

    override suspend fun resolveLegacyNotification(entryId: Long): EntryMergeNavigationProjection? {
        val entry = host.resolveLegacyNotificationEntry(entryId) ?: return null
        return resolveNavigation(EntryMergeSubject(entry.profileId, entry.id))
    }

    override fun observeNavigation(subject: EntryMergeSubject): Flow<EntryMergeNavigationProjection> {
        return host.profile(subject.profileId).observeMembership(subject.entryId).map { membership ->
            EntryMergeNavigationProjection(subject, membership?.targetEntryId ?: subject.entryId)
        }
    }
}
