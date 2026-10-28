package mihon.desktop.merge

import dev.zacsweers.metro.Inject
import mihon.entry.interactions.merge.EntryMergeLibraryLifecycleFeature
import mihon.entry.interactions.merge.EntryMergeLibraryRemovalResult
import mihon.entry.interactions.merge.EntryMergeLockedMember
import tachiyomi.domain.entry.model.Entry

/**
 * Desktop doesn't host merged entries yet, so entries join and leave the library alone, as unmerged entries do on
 * Android. Stands in until desktop hosts the merge feature.
 */
@Inject
class NoMergeLibraryLifecycle : EntryMergeLibraryLifecycleFeature {
    override suspend fun entriesRemovedFromLibrary(entries: List<Entry>): EntryMergeLibraryRemovalResult =
        EntryMergeLibraryRemovalResult(changedGroupCount = 0, unresolvedGroupCount = 0)

    override suspend fun entryAddedToLibrary(entry: Entry) = Unit

    override suspend fun lockedMembersLeavingAlone(entries: List<Entry>): List<EntryMergeLockedMember> = emptyList()
}
