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

internal object EntryPlaybackPreferencesMigrationDurableBehaviorContract : FeatureBehaviorContract {
    override val id = FeatureArtifactId("entry.playback-preferences-transfer.migration-durable.behavior")
}

internal val ENTRY_PLAYBACK_PREFERENCES_MIGRATION_PARTICIPANT = FeatureExecutionParticipantDefinition(
    id = FeatureExecutionParticipantId("entry.migration.playback-preferences"),
    owner = ENTRY_PLAYBACK_PREFERENCES_FEATURE_OWNER,
    point = ENTRY_MIGRATION_DURABLE_EXECUTION_POINT,
    prerequisites = allOf(
        CapabilityExpression.Provided(EntryMigrationCapability.definition),
        CapabilityExpression.Provided(EntryPlaybackPreferencesCapability.definition),
    ),
    behavioralContracts = listOf(EntryPlaybackPreferencesMigrationDurableBehaviorContract),
)

internal object EntryPlaybackPreferencesMigrationContributor : FeatureGraphContributor {
    override val owner = ENTRY_PLAYBACK_PREFERENCES_FEATURE_OWNER

    override fun contributeTo(sink: FeatureGraphContributionSink) {
        sink.add(ENTRY_PLAYBACK_PREFERENCES_MIGRATION_PARTICIPANT)
    }
}

internal fun entryPlaybackPreferencesMigrationBinding(
    feature: () -> EntryPlaybackPreferencesFeature,
): FeatureDurableExecutionParticipantBinding<EntryMigrationDurableEvent> {
    val json = Json {
        encodeDefaults = true
        ignoreUnknownKeys = true
    }
    return FeatureDurableExecutionParticipantBinding(
        definition = ENTRY_PLAYBACK_PREFERENCES_MIGRATION_PARTICIPANT,
        preparer = { event ->
            when (val result = feature().prepareMigration(event.source, event.target)) {
                is EntryPlaybackPreferencesMigrationPreparation.Prepared -> FeatureDurableExecutionPayload(
                    schemaVersion = 1,
                    value = json.encodeToString(EntryPlaybackPreferencesMigrationPayload.serializer(), result.payload),
                )
                EntryPlaybackPreferencesMigrationPreparation.NoPreferences,
                is EntryPlaybackPreferencesMigrationPreparation.Inapplicable,
                -> null
                is EntryPlaybackPreferencesMigrationPreparation.TypeMismatch -> error(
                    "Playback-preference Migration requires matching Entry types",
                )
            }
        },
        deliveryHandler = { payload ->
            require(payload.schemaVersion == 1) {
                "Unsupported playback-preference Migration payload ${payload.schemaVersion}"
            }
            val decoded = json.decodeFromString(EntryPlaybackPreferencesMigrationPayload.serializer(), payload.value)
            check(feature().applyMigration(decoded) is EntryPlaybackPreferencesRestoreResult.Applied)
        },
    )
}
