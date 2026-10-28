package mihon.desktop.merge

import dev.zacsweers.metro.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import mihon.entry.interactions.merge.EntryMergeNavigationFeature
import mihon.entry.interactions.merge.EntryMergeNavigationProjection
import mihon.entry.interactions.merge.EntryMergeSubject

/**
 * Desktop doesn't host merged entries yet, so every entry opens as itself, as an unmerged entry does on Android, and
 * History and Updates list each one on its own. Stands in until desktop hosts the merge feature.
 */
@Inject
class NoMergeNavigation : EntryMergeNavigationFeature {
    override suspend fun resolveNavigation(subject: EntryMergeSubject): EntryMergeNavigationProjection =
        EntryMergeNavigationProjection(subject, subject.entryId)

    // Only Android's notifications carry these.
    override suspend fun resolveLegacyNotification(entryId: Long): EntryMergeNavigationProjection? = null

    override fun observeNavigation(subject: EntryMergeSubject): Flow<EntryMergeNavigationProjection> =
        flowOf(EntryMergeNavigationProjection(subject, subject.entryId))
}
