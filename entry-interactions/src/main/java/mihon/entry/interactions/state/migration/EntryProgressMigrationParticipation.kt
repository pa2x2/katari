package mihon.entry.interactions.state.migration

import kotlinx.serialization.json.Json
import mihon.entry.interactions.migration.EntryMigrationChildMatch
import mihon.entry.interactions.migration.EntryMigrationOption
import mihon.entry.interactions.migration.consequence.ENTRY_MIGRATION_DURABLE_EXECUTION_POINT
import mihon.entry.interactions.migration.consequence.EntryMigrationDurableEvent
import mihon.entry.interactions.state.ENTRY_PROGRESS_FEATURE_OWNER
import mihon.entry.interactions.state.EntryMigrationCapability
import mihon.entry.interactions.state.EntryProgressCapability
import mihon.entry.interactions.state.EntryProgressFeature
import mihon.entry.interactions.state.EntryProgressMigrationPayload
import mihon.entry.interactions.state.EntryProgressMigrationPreparation
import mihon.entry.interactions.state.EntryProgressResourceMapping
import mihon.entry.interactions.state.EntryProgressRestoreResult
import mihon.feature.graph.CapabilityExpression
import mihon.feature.graph.FeatureArtifactId
import mihon.feature.graph.FeatureBehaviorContract
import mihon.feature.graph.FeatureExecutionParticipantId
import mihon.feature.graph.FeatureGraphContributionSink
import mihon.feature.graph.FeatureGraphContributor
import mihon.feature.graph.allOf
import mihon.feature.graph.execution.FeatureDurableExecutionParticipantBinding
import mihon.feature.graph.execution.FeatureDurableExecutionPayload
import mihon.feature.graph.execution.FeatureExecutionParticipantDefinition
import tachiyomi.domain.entry.model.progressResourceKey

internal object EntryProgressMigrationDurableBehaviorContract : FeatureBehaviorContract {
    override val id = FeatureArtifactId("entry.progress-transfer.migration-durable.behavior")
}

internal val ENTRY_PROGRESS_MIGRATION_PARTICIPANT = FeatureExecutionParticipantDefinition(
    id = FeatureExecutionParticipantId("entry.migration.progress"),
    owner = ENTRY_PROGRESS_FEATURE_OWNER,
    point = ENTRY_MIGRATION_DURABLE_EXECUTION_POINT,
    prerequisites = allOf(
        CapabilityExpression.Provided(EntryMigrationCapability.definition),
        CapabilityExpression.Provided(EntryProgressCapability.definition),
    ),
    behavioralContracts = listOf(EntryProgressMigrationDurableBehaviorContract),
)

internal object EntryProgressMigrationContributor : FeatureGraphContributor {
    override val owner = ENTRY_PROGRESS_FEATURE_OWNER

    override fun contributeTo(sink: FeatureGraphContributionSink) {
        sink.add(ENTRY_PROGRESS_MIGRATION_PARTICIPANT)
    }
}

internal fun entryProgressMigrationBinding(
    feature: () -> EntryProgressFeature,
): FeatureDurableExecutionParticipantBinding<EntryMigrationDurableEvent> {
    val json = Json {
        encodeDefaults = true
        ignoreUnknownKeys = true
    }
    return FeatureDurableExecutionParticipantBinding(
        definition = ENTRY_PROGRESS_MIGRATION_PARTICIPANT,
        preparer = { event ->
            val mappings = if (EntryMigrationOption.CHILD_STATE in event.selectedOptions) {
                prepareMigrationProgressMappings(event.childMatches)
            } else {
                emptyList()
            }
            when (
                val result = feature().prepareMigration(event.source, event.target, mappings)
            ) {
                is EntryProgressMigrationPreparation.Prepared -> FeatureDurableExecutionPayload(
                    schemaVersion = 1,
                    value = json.encodeToString(EntryProgressMigrationPayload.serializer(), result.payload),
                )
                is EntryProgressMigrationPreparation.Inapplicable -> null
                is EntryProgressMigrationPreparation.IncompatibleTypes -> error(
                    "Progress Migration requires matching Entry types",
                )
            }
        },
        deliveryHandler = { payload ->
            require(payload.schemaVersion == 1) { "Unsupported Progress Migration payload ${payload.schemaVersion}" }
            val decoded = json.decodeFromString(EntryProgressMigrationPayload.serializer(), payload.value)
            check(feature().applyMigration(decoded) is EntryProgressRestoreResult.Applied)
        },
    )
}

private fun prepareMigrationProgressMappings(
    childMatches: List<EntryMigrationChildMatch>,
): List<EntryProgressResourceMapping> {
    return childMatches.mapNotNull { match ->
        match.source?.let { source ->
            EntryProgressResourceMapping(
                sourceResourceKey = source.progressResourceKey,
                targetResourceKey = match.target.progressResourceKey,
                targetChapterId = match.target.id,
            )
        }
    }
}
