package mihon.entry.interactions.merge

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import mihon.entry.interactions.merge.consequence.EntryMergeConsequenceDelivery
import mihon.entry.interactions.merge.host.EntryMergeHost

internal class EntryMergeConsequenceStatusCoordinator(
    private val host: EntryMergeHost,
    private val delivery: EntryMergeConsequenceDelivery,
) : EntryMergeConsequenceStatusFeature {
    override fun observeStatus(): Flow<EntryMergeConsequenceStatus> {
        return host.observeConsequenceStatus().map { status ->
            EntryMergeConsequenceStatus(status.pendingCount, status.failedCount, status.lastFailure)
        }
    }

    override suspend fun retryPending(): EntryMergeConsequenceStatus {
        host.makeConsequencesRetryable()
        delivery.deliverPending()
        val status = host.observeConsequenceStatus().first()
        return EntryMergeConsequenceStatus(
            status.pendingCount,
            status.failedCount,
            status.lastFailure,
        )
    }
}
