package mihon.entry.interactions.merge.consequence

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.withContext
import mihon.entry.interactions.durable.EntryDurableConsequences
import mihon.entry.interactions.durable.EntryDurablePayload
import mihon.entry.interactions.durable.EntryDurableRecord
import mihon.entry.interactions.merge.host.EntryMergeConsequenceRequest
import mihon.entry.interactions.merge.host.EntryMergeHostMemberKey
import tachiyomi.domain.entry.model.Entry

internal enum class EntryMergeDurableChange {
    ADDED_TO_LIBRARY,
    REMOVED_FROM_LIBRARY,
    REMOVED_FROM_GROUP,
}

internal data class EntryMergeDurableEvent(
    val operationId: String,
    val entry: Entry,
    val changes: Set<EntryMergeDurableChange>,
    val downloadRemovalRequested: Boolean,
)

internal sealed interface EntryMergeConsequenceTarget {
    data class EditorMember(
        val key: EntryMergeHostMemberKey,
    ) : EntryMergeConsequenceTarget

    data class PersistedEntry(
        val id: Long,
    ) : EntryMergeConsequenceTarget
}

internal data class EntryMergeDurablePreparation(
    val target: EntryMergeConsequenceTarget,
    val event: EntryMergeDurableEvent,
)

internal sealed interface EntryMergeDurablePreparationResult {
    data class Prepared(
        val requests: List<EntryMergeConsequenceRequest>,
    ) : EntryMergeDurablePreparationResult

    data object Failed : EntryMergeDurablePreparationResult
}

internal interface EntryMergeDurablePreparationGateway {
    suspend fun prepare(
        preparations: List<EntryMergeDurablePreparation>,
    ): EntryMergeDurablePreparationResult

    suspend fun discard(requests: List<EntryMergeConsequenceRequest>)
}

internal class EntryMergeDurableConsequences(
    private val consequences: EntryDurableConsequences<EntryMergeDurableEvent>,
) : EntryMergeDurablePreparationGateway {
    override suspend fun prepare(
        preparations: List<EntryMergeDurablePreparation>,
    ): EntryMergeDurablePreparationResult {
        val prepared = mutableListOf<EntryMergeConsequenceRequest>()
        try {
            preparations.forEach { preparation ->
                prepared += consequences.prepare(preparation.event).map { it.toRequest(preparation.target) }
            }
        } catch (error: CancellationException) {
            withContext(NonCancellable) { discard(prepared) }
            throw error
        } catch (_: Exception) {
            discard(prepared)
            return EntryMergeDurablePreparationResult.Failed
        }
        return EntryMergeDurablePreparationResult.Prepared(prepared)
    }

    suspend fun deliver(record: EntryDurableRecord) {
        consequences.deliver(record)
    }

    override suspend fun discard(requests: List<EntryMergeConsequenceRequest>) {
        consequences.discard(requests.map(EntryMergeConsequenceRequest::record))
    }

    suspend fun discardRecord(record: EntryDurableRecord) {
        consequences.discard(listOf(record))
    }
}

private fun EntryDurableRecord.toRequest(
    target: EntryMergeConsequenceTarget,
) = EntryMergeConsequenceRequest(
    memberKey = (target as? EntryMergeConsequenceTarget.EditorMember)?.key,
    entryId = (target as? EntryMergeConsequenceTarget.PersistedEntry)?.id,
    participantId = participantId,
    schemaVersion = payload.schemaVersion,
    payload = payload.value,
)

private fun EntryMergeConsequenceRequest.record() = EntryDurableRecord(
    participantId = participantId,
    payload = EntryDurablePayload(schemaVersion, payload),
)
