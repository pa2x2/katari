package mihon.entry.interactions.migration.consequence.cover

import kotlinx.serialization.json.Json
import mihon.entry.interactions.library.membership.consequence.ENTRY_LIBRARY_CUSTOM_COVER_OWNER
import mihon.entry.interactions.migration.EntryMigrationOption
import mihon.entry.interactions.migration.consequence.ENTRY_MIGRATION_DURABLE_EXECUTION_POINT
import mihon.entry.interactions.migration.consequence.EntryMigrationDurableEvent
import mihon.entry.interactions.migration.host.EntryMigrationCustomCoverHost
import mihon.entry.interactions.migration.host.EntryMigrationCustomCoverPayload
import mihon.entry.interactions.state.EntryMigrationCapability
import mihon.feature.graph.CapabilityExpression
import mihon.feature.graph.FeatureArtifactId
import mihon.feature.graph.FeatureBehaviorContract
import mihon.feature.graph.FeatureExecutionParticipantId
import mihon.feature.graph.FeatureGraphContributionSink
import mihon.feature.graph.FeatureGraphContributor
import mihon.feature.graph.execution.FeatureDurableExecutionParticipantBinding
import mihon.feature.graph.execution.FeatureDurableExecutionPayload
import mihon.feature.graph.execution.FeatureExecutionParticipantDefinition

internal object EntryMigrationCustomCoverDurableBehaviorContract : FeatureBehaviorContract {
    override val id = FeatureArtifactId("entry.custom-cover.migration-durable.behavior")
}

internal val ENTRY_MIGRATION_CUSTOM_COVER_PARTICIPANT = FeatureExecutionParticipantDefinition(
    id = FeatureExecutionParticipantId("entry.migration.custom-cover"),
    owner = ENTRY_LIBRARY_CUSTOM_COVER_OWNER,
    point = ENTRY_MIGRATION_DURABLE_EXECUTION_POINT,
    prerequisites = CapabilityExpression.Provided(EntryMigrationCapability.definition),
    behavioralContracts = listOf(EntryMigrationCustomCoverDurableBehaviorContract),
)

internal object EntryMigrationCustomCoverContributor : FeatureGraphContributor {
    override val owner = ENTRY_LIBRARY_CUSTOM_COVER_OWNER

    override fun contributeTo(sink: FeatureGraphContributionSink) {
        sink.add(ENTRY_MIGRATION_CUSTOM_COVER_PARTICIPANT)
    }
}

internal fun entryMigrationCustomCoverBinding(
    host: EntryMigrationCustomCoverHost,
): FeatureDurableExecutionParticipantBinding<EntryMigrationDurableEvent> {
    val json = Json {
        encodeDefaults = true
        ignoreUnknownKeys = true
    }
    return FeatureDurableExecutionParticipantBinding(
        definition = ENTRY_MIGRATION_CUSTOM_COVER_PARTICIPANT,
        preparer = { event ->
            if (EntryMigrationOption.CUSTOM_COVER !in event.selectedOptions) {
                null
            } else {
                host.stage(event.operationId, event.source, event.target)?.let { payload ->
                    FeatureDurableExecutionPayload(
                        schemaVersion = 1,
                        value = json.encodeToString(EntryMigrationCustomCoverPayload.serializer(), payload),
                    )
                }
            }
        },
        deliveryHandler = { payload -> host.promote(payload.decodeCustomCover(json)) },
        discardHandler = { payload -> host.discard(payload.decodeCustomCover(json)) },
    )
}

internal fun FeatureDurableExecutionPayload.decodeCustomCover(json: Json): EntryMigrationCustomCoverPayload {
    require(schemaVersion == 1) { "Unsupported custom-cover Migration payload $schemaVersion" }
    return json.decodeFromString(EntryMigrationCustomCoverPayload.serializer(), value)
}
