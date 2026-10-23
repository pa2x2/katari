package mihon.entry.interactions.merge

import tachiyomi.domain.entry.model.Entry

interface EntryMergeLibraryLifecycleFeature {
    suspend fun entriesRemovedFromLibrary(entries: List<Entry>): EntryMergeLibraryRemovalResult

    /** Brings the members a merged entry's source supplies into the Library with it; throws when they can't join. */
    suspend fun entryAddedToLibrary(entry: Entry)

    /**
     * The members of [entries] that a merged entry's source supplies while that merged entry isn't in [entries]. They
     * can only leave the Library with their merged entry.
     */
    suspend fun lockedMembersLeavingAlone(entries: List<Entry>): List<EntryMergeLockedMember>
}

data class EntryMergeLockedMember(
    val member: Entry,
    val mergedEntry: Entry,
)

data class EntryMergeLibraryRemovalResult(
    val changedGroupCount: Int,
    val unresolvedGroupCount: Int,
)
