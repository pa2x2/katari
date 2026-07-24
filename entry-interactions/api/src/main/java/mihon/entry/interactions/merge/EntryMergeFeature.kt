package mihon.entry.interactions

import kotlinx.coroutines.flow.Flow
import tachiyomi.domain.entry.model.Entry

/**
 * Application boundary for preparing and executing shared Merge workflows.
 *
 * Callers provide user intent. The Merge feature resolves authoritative membership, validates the intent, and owns
 * every resulting consequence. Raw membership reads and persistence operations are deliberately absent from this
 * contract.
 */
interface EntryMergeFeature {
    suspend fun prepare(intent: EntryMergePrepareIntent): EntryMergePreparationResult

    /** Observes an existing group as a feature-owned editor projection; standalone Entries emit `null`. */
    fun observeExisting(entry: Entry): Flow<EntryMergeEditorProjection?>

    suspend fun execute(intent: EntryMergeWorkflowIntent): EntryMergeExecutionResult
}
