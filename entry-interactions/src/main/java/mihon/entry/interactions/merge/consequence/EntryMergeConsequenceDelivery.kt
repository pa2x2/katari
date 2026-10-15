package mihon.entry.interactions.merge.consequence

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import mihon.entry.interactions.durable.EntryDurablePayload
import mihon.entry.interactions.durable.EntryDurableRecord
import mihon.entry.interactions.merge.EntryMergeFollowUp
import mihon.entry.interactions.merge.consequence.legacy.EntryMergeLegacyConsequenceDelivery
import mihon.entry.interactions.merge.host.EntryMergeHost
import mihon.entry.interactions.merge.host.EntryMergePendingConsequence

internal class EntryMergeConsequenceDelivery(
    private val host: EntryMergeHost,
    private val consequences: EntryMergeDurableConsequences,
    private val legacy: EntryMergeLegacyConsequenceDelivery? = null,
    private val clockMillis: () -> Long = System::currentTimeMillis,
) {
    suspend fun deliverOperation(operationId: String): EntryMergeFollowUp {
        deliverPending()
        return if (host.pendingConsequenceCount(operationId) == 0L) {
            EntryMergeFollowUp.COMPLETE
        } else {
            EntryMergeFollowUp.PENDING
        }
    }

    suspend fun deliverPending(limit: Int = DEFAULT_BATCH_SIZE) {
        host.pendingConsequences(limit).forEach { consequence -> deliverSafely(consequence) }
    }

    suspend fun runRetryLoop() {
        while (currentCoroutineContext().isActive) {
            try {
                deliverPending()
            } catch (error: CancellationException) {
                throw error
            } catch (_: Exception) {
                // A host-level failure leaves every record durable for the next pass.
            }
            delay(RETRY_DELAY_MILLIS)
        }
    }

    private suspend fun deliverSafely(consequence: EntryMergePendingConsequence) {
        try {
            val record = if (legacy?.handles(consequence) == true) {
                legacy.deliver(consequence)
                null
            } else {
                consequence.record().also { consequences.deliver(it) }
            }
            host.acknowledgeConsequence(consequence.id)
            record?.let { consequences.discardRecord(it) }
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

private fun EntryMergePendingConsequence.record() = EntryDurableRecord(
    participantId = participantId,
    payload = EntryDurablePayload(schemaVersion, payload),
)
