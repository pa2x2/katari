package mihon.entry.interactions

import kotlinx.serialization.json.Json
import mihon.feature.graph.CapabilityExpression
import mihon.feature.graph.FeatureArtifactId
import mihon.feature.graph.FeatureBehaviorContract
import mihon.feature.graph.FeatureDurableExecutionParticipantBinding
import mihon.feature.graph.FeatureDurableExecutionPayload
import mihon.feature.graph.FeatureExecutionHandler
import mihon.feature.graph.FeatureExecutionParticipantBinding
import mihon.feature.graph.FeatureExecutionParticipantDefinition
import mihon.feature.graph.FeatureExecutionParticipantId
import mihon.feature.graph.FeatureGraphContributionSink
import mihon.feature.graph.FeatureGraphContributor
import mihon.feature.graph.allOf

internal object EntryDownloadMigrationDurableBehaviorContract : FeatureBehaviorContract {
    override val id = FeatureArtifactId("entry.download.maintenance.migration-durable.behavior")
}

internal object EntryDownloadMigrationOptionBehaviorContract : FeatureBehaviorContract {
    override val id = FeatureArtifactId("entry.download.maintenance.migration-option.behavior")
}

internal val ENTRY_DOWNLOAD_MIGRATION_OPTION_PARTICIPANT = FeatureExecutionParticipantDefinition(
    id = FeatureExecutionParticipantId("entry.download.maintenance.migration-option"),
    owner = ENTRY_DOWNLOAD_MAINTENANCE_FEATURE_OWNER,
    point = ENTRY_MIGRATION_OPTION_DISCOVERY_POINT,
    prerequisites = allOf(
        CapabilityExpression.Provided(EntryMigrationCapability.definition),
        CapabilityExpression.Provided(EntryDownloadCapability.definition),
    ),
    behavioralContracts = listOf(EntryDownloadMigrationOptionBehaviorContract),
)

internal val ENTRY_DOWNLOAD_MIGRATION_PARTICIPANT = FeatureExecutionParticipantDefinition(
    id = FeatureExecutionParticipantId("entry.migration.download-removal"),
    owner = ENTRY_DOWNLOAD_MAINTENANCE_FEATURE_OWNER,
    point = ENTRY_MIGRATION_DURABLE_EXECUTION_POINT,
    prerequisites = allOf(
        CapabilityExpression.Provided(EntryMigrationCapability.definition),
        CapabilityExpression.Provided(EntryDownloadCapability.definition),
    ),
    behavioralContracts = listOf(EntryDownloadMigrationDurableBehaviorContract),
)

internal object EntryDownloadMigrationContributor : FeatureGraphContributor {
    override val owner = ENTRY_DOWNLOAD_MAINTENANCE_FEATURE_OWNER

    override fun contributeTo(sink: FeatureGraphContributionSink) {
        sink.add(ENTRY_DOWNLOAD_MIGRATION_OPTION_PARTICIPANT)
        sink.add(ENTRY_DOWNLOAD_MIGRATION_PARTICIPANT)
    }
}

internal fun entryDownloadMigrationOptionBinding(
    feature: () -> EntryDownloadMaintenanceFeature,
) = FeatureExecutionParticipantBinding(
    definition = ENTRY_DOWNLOAD_MIGRATION_OPTION_PARTICIPANT,
    handler = FeatureExecutionHandler { event ->
        if (feature().inspectEntry(event.source) == EntryDownloadMaintenanceInspection.HasDownloads) {
            event.options.add(EntryMigrationOption.REMOVE_SOURCE_DOWNLOADS)
        }
    },
)

internal fun entryDownloadMigrationBinding(
    feature: () -> EntryDownloadMaintenanceFeature,
): FeatureDurableExecutionParticipantBinding<EntryMigrationDurableEvent> {
    val json = Json {
        encodeDefaults = true
        ignoreUnknownKeys = true
    }
    return FeatureDurableExecutionParticipantBinding(
        definition = ENTRY_DOWNLOAD_MIGRATION_PARTICIPANT,
        preparer = { event ->
            if (EntryMigrationOption.REMOVE_SOURCE_DOWNLOADS !in event.selectedOptions) {
                null
            } else {
                when (val result = feature().prepareRemoval(event.source)) {
                    is EntryDownloadRemovalPreparation.Prepared -> FeatureDurableExecutionPayload(
                        schemaVersion = 1,
                        value = json.encodeToString(EntryDownloadRemovalPlan.serializer(), result.plan),
                    )
                    EntryDownloadRemovalPreparation.NothingToRemove,
                    is EntryDownloadRemovalPreparation.Inapplicable,
                    -> null
                }
            }
        },
        deliveryHandler = { payload ->
            require(payload.schemaVersion == 1) {
                "Unsupported Download Migration payload ${payload.schemaVersion}"
            }
            val plan = json.decodeFromString(EntryDownloadRemovalPlan.serializer(), payload.value)
            check(feature().applyRemoval(plan) == EntryDownloadMaintenanceResult.Performed) {
                "Migration download removal was not verified"
            }
        },
    )
}
