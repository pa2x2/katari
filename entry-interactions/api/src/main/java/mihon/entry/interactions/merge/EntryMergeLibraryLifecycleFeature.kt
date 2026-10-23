package mihon.entry.interactions.merge

import tachiyomi.domain.entry.model.Entry

interface EntryMergeLibraryLifecycleFeature {
    suspend fun entriesRemovedFromLibrary(entries: List<Entry>): EntryMergeLibraryRemovalResult

    /** Brings the members a merged entry's source supplies into the Library with it; throws when they can't join. */
    suspend fun entryAddedToLibrary(entry: Entry)
}

data class EntryMergeLibraryRemovalResult(
    val changedGroupCount: Int,
    val unresolvedGroupCount: Int,
)
