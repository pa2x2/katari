package mihon.entry.interactions

import tachiyomi.domain.entry.model.Entry

interface EntryDestructiveRemovalFeature {
    suspend fun remove(entries: List<Entry>): EntryDestructiveRemovalResult
}

sealed interface EntryDestructiveRemovalResult {
    data object NoChange : EntryDestructiveRemovalResult

    data class Removed(
        val entries: List<Entry>,
        val failures: List<EntryLifecycleConsequenceFailure>,
    ) : EntryDestructiveRemovalResult

    data class Failed(
        val entries: List<Entry>,
        val cause: Throwable,
    ) : EntryDestructiveRemovalResult
}
