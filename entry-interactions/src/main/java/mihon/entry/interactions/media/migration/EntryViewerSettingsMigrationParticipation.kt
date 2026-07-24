package mihon.entry.interactions

import kotlinx.serialization.json.Json
import mihon.feature.graph.CapabilityExpression
import mihon.feature.graph.FeatureArtifactId
import mihon.feature.graph.FeatureBehaviorContract
import mihon.feature.graph.FeatureDurableExecutionParticipantBinding
import mihon.feature.graph.FeatureDurableExecutionPayload
import mihon.feature.graph.FeatureExecutionParticipantDefinition
import mihon.feature.graph.FeatureExecutionParticipantId
import mihon.feature.graph.FeatureGraphContributionSink
import mihon.feature.graph.FeatureGraphContributor
import mihon.feature.graph.allOf

internal object EntryViewerSettingsMigrationDurableBehaviorContract : FeatureBehaviorContract {
    override val id = FeatureArtifactId("entry.viewer-settings.migration-durable.behavior")
}

internal val ENTRY_VIEWER_SETTINGS_MIGRATION_PARTICIPANT = FeatureExecutionParticipantDefinition(
    id = FeatureExecutionParticipantId("entry.migration.viewer-settings"),
    owner = ENTRY_VIEWER_SETTINGS_FEATURE_OWNER,
    point = ENTRY_MIGRATION_DURABLE_EXECUTION_POINT,
    prerequisites = allOf(
        CapabilityExpression.Provided(EntryMigrationCapability.definition),
        CapabilityExpression.Provided(EntryViewerSettingsCapability.definition),
    ),
    behavioralContracts = listOf(EntryViewerSettingsMigrationDurableBehaviorContract),
)

internal object EntryViewerSettingsMigrationContributor : FeatureGraphContributor {
    override val owner = ENTRY_VIEWER_SETTINGS_FEATURE_OWNER

    override fun contributeTo(sink: FeatureGraphContributionSink) {
        sink.add(ENTRY_VIEWER_SETTINGS_MIGRATION_PARTICIPANT)
    }
}

internal fun entryViewerSettingsMigrationBinding(
    feature: () -> EntryViewerSettingsFeature,
): FeatureDurableExecutionParticipantBinding<EntryMigrationDurableEvent> {
    val json = Json {
        encodeDefaults = true
        ignoreUnknownKeys = true
    }
    return FeatureDurableExecutionParticipantBinding(
        definition = ENTRY_VIEWER_SETTINGS_MIGRATION_PARTICIPANT,
        preparer = { event ->
            when (val result = feature().prepareMigration(event.source, event.target)) {
                is EntryViewerSettingsMigrationPreparation.Prepared -> FeatureDurableExecutionPayload(
                    schemaVersion = 1,
                    value = json.encodeToString(EntryViewerSettingsMigrationPayload.serializer(), result.payload),
                )
                is EntryViewerSettingsMigrationPreparation.Inapplicable -> null
                is EntryViewerSettingsMigrationPreparation.TypeMismatch -> error(
                    "Viewer Settings Migration requires matching Entry types",
                )
            }
        },
        deliveryHandler = { payload ->
            require(payload.schemaVersion == 1) {
                "Unsupported Viewer Settings Migration payload ${payload.schemaVersion}"
            }
            val decoded = json.decodeFromString(EntryViewerSettingsMigrationPayload.serializer(), payload.value)
            check(feature().applyMigration(decoded) is EntryViewerSettingsRestoreResult.Restored)
        },
    )
}
