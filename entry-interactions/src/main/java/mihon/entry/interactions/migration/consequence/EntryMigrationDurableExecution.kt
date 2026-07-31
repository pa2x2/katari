package mihon.entry.interactions.migration.consequence

import mihon.entry.interactions.migration.ENTRY_MIGRATION_FEATURE_OWNER
import mihon.entry.interactions.migration.EntryMigrationOption
import mihon.entry.interactions.prepareDurable
import mihon.entry.interactions.runtime.toContentTypeId
import mihon.feature.graph.FeatureExecutionPointId
import mihon.feature.graph.execution.FeatureDurableExecutionEnvelope
import mihon.feature.graph.execution.FeatureDurableExecutionPreparationResult
import mihon.feature.graph.execution.FeatureExecutionFailurePolicy
import mihon.feature.graph.execution.FeatureExecutionRuntime
import mihon.feature.graph.execution.durableFeatureExecutionPointDefinition
import tachiyomi.domain.entry.model.Entry
import tachiyomi.domain.entry.model.EntryChapter

internal data class EntryMigrationDurableEvent(
    val operationId: String,
    val source: Entry,
    val target: Entry,
    val selectedOptions: Set<EntryMigrationOption>,
    val sourceChildren: List<EntryChapter>,
    val targetChildren: List<EntryChapter>,
)

internal val ENTRY_MIGRATION_DURABLE_EXECUTION_POINT =
    durableFeatureExecutionPointDefinition<EntryMigrationDurableEvent>(
        id = FeatureExecutionPointId("entry.migration.durable-consequences"),
        owner = ENTRY_MIGRATION_FEATURE_OWNER,
        failurePolicy = FeatureExecutionFailurePolicy.FAIL_FAST,
    )

internal class EntryMigrationDurableConsequences(
    private val executions: FeatureExecutionRuntime,
) {
    suspend fun prepare(event: EntryMigrationDurableEvent): FeatureDurableExecutionPreparationResult {
        return executions.prepareDurable(
            point = ENTRY_MIGRATION_DURABLE_EXECUTION_POINT,
            contentType = event.source.type.toContentTypeId(),
            event = event,
        )
    }

    suspend fun deliver(envelope: FeatureDurableExecutionEnvelope) {
        executions.deliverDurable(envelope)
    }

    suspend fun discard(envelopes: List<FeatureDurableExecutionEnvelope>) {
        executions.discardDurable(envelopes)
    }
}
