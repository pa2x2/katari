package mihon.entry.interactions

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import mihon.entry.interactions.host.EntryMigrationConsequenceHost

internal class EntryMigrationConsequenceStatusCoordinator(
    private val host: EntryMigrationConsequenceHost,
    private val delivery: EntryMigrationConsequenceDelivery,
) : EntryMigrationConsequenceStatusFeature {
    override fun observeStatus(): Flow<EntryMigrationConsequenceStatus> {
        return host.observeConsequenceStatus().map { status ->
            EntryMigrationConsequenceStatus(status.pendingCount, status.failedCount, status.lastFailure)
        }
    }

    override suspend fun retryPending(): EntryMigrationConsequenceStatus {
        host.makeConsequencesRetryable()
        delivery.deliverPending()
        val status = host.observeConsequenceStatus().first()
        return EntryMigrationConsequenceStatus(status.pendingCount, status.failedCount, status.lastFailure)
    }
}
