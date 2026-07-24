package mihon.entry.interactions

import tachiyomi.domain.entry.model.Entry

interface EntryMergeLibraryLifecycleFeature {
    suspend fun entriesRemovedFromLibrary(entries: List<Entry>): EntryMergeLibraryRemovalResult
}

data class EntryMergeLibraryRemovalResult(
    val changedGroupCount: Int,
    val unresolvedGroupCount: Int,
)
