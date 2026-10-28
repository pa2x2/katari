package mihon.entry.interactions.lifecycle.removal

import mihon.entry.interactions.lifecycle.EntryLifecycleConsequenceFailure
import tachiyomi.domain.entry.model.Entry

interface EntryDestructiveRemovalFeature {
    /**
     * False where the platform can't delete entries with what they own yet; what deletes entries, such as clearing
     * the database or deleting a profile, is then hidden, as [remove] fails for any entry.
     */
    val isOffered: Boolean

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
