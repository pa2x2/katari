package mihon.desktop.merge

import dev.zacsweers.metro.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import mihon.entry.interactions.merge.EntryMergeEditorProjection
import mihon.entry.interactions.merge.EntryMergeExecutionResult
import mihon.entry.interactions.merge.EntryMergeFeature
import mihon.entry.interactions.merge.EntryMergePreparationResult
import mihon.entry.interactions.merge.EntryMergePrepareIntent
import mihon.entry.interactions.merge.EntryMergeRejection
import mihon.entry.interactions.merge.EntryMergeWorkflowIntent
import tachiyomi.domain.entry.model.Entry

/**
 * Desktop doesn't host merged entries yet, so no selection can be merged and the library doesn't offer merging, and
 * every entry stands alone, as an unmerged entry does on Android. Stands in until desktop hosts the merge feature.
 */
@Inject
class NoEntryMergeFeature : EntryMergeFeature {
    override suspend fun prepare(intent: EntryMergePrepareIntent): EntryMergePreparationResult =
        EntryMergePreparationResult.Rejected(EntryMergeRejection.PREPARATION_MISSING)

    override fun observeExisting(entry: Entry): Flow<EntryMergeEditorProjection?> = flowOf(null)

    override suspend fun execute(intent: EntryMergeWorkflowIntent): EntryMergeExecutionResult =
        EntryMergeExecutionResult.Rejected(EntryMergeRejection.PREPARATION_MISSING)
}
