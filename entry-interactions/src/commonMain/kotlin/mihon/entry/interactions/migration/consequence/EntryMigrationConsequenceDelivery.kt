package mihon.entry.interactions.migration.consequence

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import mihon.entry.interactions.durable.EntryDurableConsequences
import mihon.entry.interactions.durable.EntryDurablePayload
import mihon.entry.interactions.durable.EntryDurableRecord
import mihon.entry.interactions.migration.EntryMigrationFollowUp
import mihon.entry.interactions.migration.consequence.cover.EntryMigrationCustomCoverOrphanCleanup
import mihon.entry.interactions.migration.host.EntryMigrationConsequenceHost
import mihon.entry.interactions.migration.host.EntryMigrationPendingConsequence

internal class EntryMigrationConsequenceDelivery(
    private val host: EntryMigrationConsequenceHost,
    private val consequences: EntryDurableConsequences<EntryMigrationDurableEvent>,
    private val coverOrphanCleanup: EntryMigrationCustomCoverOrphanCleanup,
    private val clockMillis: () -> Long = System::currentTimeMillis,
) {
    suspend fun deliverOperation(operationId: String): EntryMigrationFollowUp {
        host.pendingConsequences(operationId, DEFAULT_BATCH_SIZE).forEach { consequence ->
            deliverSafely(consequence)
        }
        return if (host.pendingConsequenceCount(operationId) == 0L) {
            EntryMigrationFollowUp.COMPLETE
        } else {
            EntryMigrationFollowUp.INCOMPLETE
        }
    }

    suspend fun deliverPending(limit: Int = DEFAULT_BATCH_SIZE) {
        host.pendingConsequences(limit).forEach { consequence -> deliverSafely(consequence) }
    }

    suspend fun runRetryLoop() {
        try {
            coverOrphanCleanup.cleanup()
        } catch (error: CancellationException) {
            throw error
        } catch (_: Exception) {
            // Delivery still starts when optional cleanup cannot inspect its durable records.
        }
        while (currentCoroutineContext().isActive) {
            try {
                deliverPending()
            } catch (error: CancellationException) {
                throw error
            } catch (_: Exception) {
                // Host-level failure leaves every record durable for the next pass.
            }
            delay(RETRY_DELAY_MILLIS)
        }
    }

    private suspend fun deliverSafely(consequence: EntryMigrationPendingConsequence) {
        try {
            val record = consequence.record()
            consequences.deliver(record)
            host.acknowledgeConsequence(consequence.id)
            consequences.discard(listOf(record))
        } catch (error: CancellationException) {
            throw error
        } catch (error: Exception) {
            host.recordConsequenceFailure(
                consequenceId = consequence.id,
                message = error.message ?: error::class.qualifiedName.orEmpty(),
                retryAtMillis = clockMillis() + RETRY_DELAY_MILLIS,
            )
        }
    }

    private companion object {
        const val DEFAULT_BATCH_SIZE = 100
        const val RETRY_DELAY_MILLIS = 60_000L
    }
}

private fun EntryMigrationPendingConsequence.record() = EntryDurableRecord(
    participantId = participantId,
    payload = EntryDurablePayload(schemaVersion, payload),
)
