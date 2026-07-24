package mihon.entry.interactions

import kotlinx.serialization.json.Json
import mihon.entry.interactions.host.EntryMigrationCustomCoverHost
import mihon.entry.interactions.host.EntryMigrationCustomCoverPayload
import mihon.feature.graph.CapabilityExpression
import mihon.feature.graph.FeatureArtifactId
import mihon.feature.graph.FeatureBehaviorContract
import mihon.feature.graph.FeatureDurableExecutionParticipantBinding
import mihon.feature.graph.FeatureDurableExecutionPayload
import mihon.feature.graph.FeatureExecutionParticipantDefinition
import mihon.feature.graph.FeatureExecutionParticipantId
import mihon.feature.graph.FeatureGraphContributionSink
import mihon.feature.graph.FeatureGraphContributor

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
