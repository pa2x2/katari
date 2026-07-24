package mihon.entry.interactions

import mihon.feature.graph.FeatureExecutionResult
import mihon.feature.graph.FeatureExecutionRuntime
import mihon.feature.graph.afterFeatureCommitVolatile
import tachiyomi.domain.entry.model.Entry

internal class EntryMetadataLifecycleCoordinator(
    private val executions: FeatureExecutionRuntime,
) : EntryMetadataLifecycleFeature {
    override suspend fun changed(previous: Entry, current: Entry): EntryMetadataChangeResult {
        require(previous.profileId == current.profileId && previous.id == current.id) {
            "Metadata lifecycle cannot change Entry identity"
        }
        require(previous.type == current.type) { "Metadata lifecycle cannot change Entry type" }
        if (previous == current) return EntryMetadataChangeResult.NoChange

        val execution = executions.afterFeatureCommitVolatile {
            execute(
                point = ENTRY_METADATA_CHANGED_EXECUTION_POINT,
                contentType = current.type.toContentTypeId(),
                event = EntryMetadataChangedEvent(previous, current),
            )
        }
        return EntryMetadataChangeResult.Applied(execution.toLifecycleFailures())
    }
}

internal fun FeatureExecutionResult.toLifecycleFailures(): List<EntryLifecycleConsequenceFailure> {
    return failures.map { failure ->
        EntryLifecycleConsequenceFailure(
            participantId = failure.participant.value,
            ownerId = failure.owner.value,
            cause = failure.error,
        )
    }
}
