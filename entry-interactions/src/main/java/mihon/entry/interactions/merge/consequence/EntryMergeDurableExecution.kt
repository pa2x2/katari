package mihon.entry.interactions

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.withContext
import mihon.entry.interactions.host.EntryMergeConsequenceRequest
import mihon.entry.interactions.host.EntryMergeHostMemberKey
import mihon.feature.graph.FeatureDurableExecutionEnvelope
import mihon.feature.graph.FeatureExecutionFailurePolicy
import mihon.feature.graph.FeatureExecutionPointId
import mihon.feature.graph.FeatureExecutionRuntime
import mihon.feature.graph.durableFeatureExecutionPointDefinition
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

internal val ENTRY_MERGE_DURABLE_EXECUTION_POINT =
    durableFeatureExecutionPointDefinition<EntryMergeDurableEvent>(
        id = FeatureExecutionPointId("entry.merge.durable-consequences"),
        owner = ENTRY_MERGE_FEATURE_OWNER,
        failurePolicy = FeatureExecutionFailurePolicy.FAIL_FAST,
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
    private val executions: FeatureExecutionRuntime,
) : EntryMergeDurablePreparationGateway {
    override suspend fun prepare(
        preparations: List<EntryMergeDurablePreparation>,
    ): EntryMergeDurablePreparationResult {
        val prepared = mutableListOf<EntryMergeConsequenceRequest>()
        try {
            preparations.forEach { preparation ->
                val result = executions.prepareDurable(
                    point = ENTRY_MERGE_DURABLE_EXECUTION_POINT,
                    contentType = preparation.event.entry.type.toContentTypeId(),
                    event = preparation.event,
                )
                val requests = result.envelopes.map { envelope ->
                    envelope.toRequest(preparation.target)
                }
                prepared += requests
                if (!result.isSuccessful) {
                    discard(prepared)
                    return EntryMergeDurablePreparationResult.Failed
                }
            }
        } catch (error: CancellationException) {
            withContext(NonCancellable) {
                runCatching { discard(prepared) }
            }
            throw error
        }
        return EntryMergeDurablePreparationResult.Prepared(prepared)
    }

    suspend fun deliver(envelope: FeatureDurableExecutionEnvelope) {
        executions.deliverDurable(envelope)
    }

    override suspend fun discard(requests: List<EntryMergeConsequenceRequest>) {
        executions.discardDurable(requests.map(EntryMergeConsequenceRequest::envelope))
    }

    suspend fun discardEnvelope(envelope: FeatureDurableExecutionEnvelope) {
        executions.discardDurable(listOf(envelope))
    }
}

private fun FeatureDurableExecutionEnvelope.toRequest(
    target: EntryMergeConsequenceTarget,
) = EntryMergeConsequenceRequest(
    memberKey = (target as? EntryMergeConsequenceTarget.EditorMember)?.key,
    entryId = (target as? EntryMergeConsequenceTarget.PersistedEntry)?.id,
    participantId = participant.value,
    schemaVersion = schemaVersion,
    payload = payload,
)

private fun EntryMergeConsequenceRequest.envelope() = FeatureDurableExecutionEnvelope(
    participant = mihon.feature.graph.FeatureExecutionParticipantId(participantId),
    schemaVersion = schemaVersion,
    payload = payload,
)
