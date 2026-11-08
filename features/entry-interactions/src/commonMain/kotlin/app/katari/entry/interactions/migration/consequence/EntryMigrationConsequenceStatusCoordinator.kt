package app.katari.entry.interactions.migration.consequence

import app.katari.entry.interactions.migration.EntryMigrationConsequenceStatus
import app.katari.entry.interactions.migration.EntryMigrationConsequenceStatusFeature
import app.katari.entry.interactions.migration.host.EntryMigrationConsequenceHost
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

internal class EntryMigrationConsequenceStatusCoordinator(
    private val host: EntryMigrationConsequenceHost,
    private val delivery: EntryMigrationConsequenceDelivery,
) : EntryMigrationConsequenceStatusFeature {
    override fun observeStatus(): Flow<EntryMigrationConsequenceStatus> {
        return host.observeConsequenceStatus().map { status ->
            EntryMigrationConsequenceStatus(
                status.pendingCount,
                status.failedCount,
                status.lastFailure,
            )
        }
    }

    override suspend fun retryPending(): EntryMigrationConsequenceStatus {
        host.makeConsequencesRetryable()
        delivery.deliverPending()
        val status = host.observeConsequenceStatus().first()
        return EntryMigrationConsequenceStatus(
            status.pendingCount,
            status.failedCount,
            status.lastFailure,
        )
    }
}
