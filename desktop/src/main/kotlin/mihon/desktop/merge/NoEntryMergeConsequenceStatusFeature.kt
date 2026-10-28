package mihon.desktop.merge

import dev.zacsweers.metro.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import mihon.entry.interactions.merge.EntryMergeConsequenceStatus
import mihon.entry.interactions.merge.EntryMergeConsequenceStatusFeature

/** Desktop doesn't merge entries yet, so no merge leaves work pending. Stands in until desktop hosts merges. */
@Inject
class NoEntryMergeConsequenceStatusFeature : EntryMergeConsequenceStatusFeature {
    private val idle = EntryMergeConsequenceStatus(pendingCount = 0, failedCount = 0, lastFailure = null)

    override fun observeStatus(): Flow<EntryMergeConsequenceStatus> = flowOf(idle)

    override suspend fun retryPending(): EntryMergeConsequenceStatus = idle
}
