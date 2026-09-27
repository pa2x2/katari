package mihon.entry.interactions.translation.migration

import kotlinx.serialization.json.Json
import mihon.entry.interactions.migration.consequence.ENTRY_MIGRATION_DURABLE_EXECUTION_POINT
import mihon.entry.interactions.migration.consequence.EntryMigrationDurableEvent
import mihon.entry.interactions.state.EntryMigrationCapability
import mihon.entry.interactions.state.EntryTranslationLanguagesCapability
import mihon.entry.interactions.translation.DefaultEntryTranslationLanguagesFeature
import mihon.entry.interactions.translation.ENTRY_TRANSLATION_LANGUAGES_FEATURE_OWNER
import mihon.entry.interactions.translation.EntryTranslationLanguagesMigrationPayload
import mihon.entry.interactions.translation.EntryTranslationLanguagesMigrationPreparation
import mihon.entry.interactions.translation.EntryTranslationLanguagesWriteResult
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

internal object EntryTranslationLanguagesMigrationDurableBehaviorContract : FeatureBehaviorContract {
    override val id = FeatureArtifactId("entry.translation-languages.migration-durable.behavior")
}

internal val ENTRY_TRANSLATION_LANGUAGES_MIGRATION_PARTICIPANT = FeatureExecutionParticipantDefinition(
    id = FeatureExecutionParticipantId("entry.migration.translation-languages"),
    owner = ENTRY_TRANSLATION_LANGUAGES_FEATURE_OWNER,
    point = ENTRY_MIGRATION_DURABLE_EXECUTION_POINT,
    prerequisites = allOf(
        CapabilityExpression.Provided(EntryMigrationCapability.definition),
        CapabilityExpression.Provided(EntryTranslationLanguagesCapability.definition),
    ),
    behavioralContracts = listOf(EntryTranslationLanguagesMigrationDurableBehaviorContract),
)

internal object EntryTranslationLanguagesMigrationContributor : FeatureGraphContributor {
    override val owner = ENTRY_TRANSLATION_LANGUAGES_FEATURE_OWNER

    override fun contributeTo(sink: FeatureGraphContributionSink) {
        sink.add(ENTRY_TRANSLATION_LANGUAGES_MIGRATION_PARTICIPANT)
    }
}

internal fun entryTranslationLanguagesMigrationBinding(
    feature: () -> DefaultEntryTranslationLanguagesFeature,
): FeatureDurableExecutionParticipantBinding<EntryMigrationDurableEvent> {
    val json = Json {
        encodeDefaults = true
        ignoreUnknownKeys = true
    }
    return FeatureDurableExecutionParticipantBinding(
        definition = ENTRY_TRANSLATION_LANGUAGES_MIGRATION_PARTICIPANT,
        preparer = { event ->
            when (val result = feature().prepareMigration(event.source, event.target)) {
                is EntryTranslationLanguagesMigrationPreparation.Prepared -> FeatureDurableExecutionPayload(
                    schemaVersion = 1,
                    value = json.encodeToString(EntryTranslationLanguagesMigrationPayload.serializer(), result.payload),
                )
                EntryTranslationLanguagesMigrationPreparation.NoTargetLanguage,
                is EntryTranslationLanguagesMigrationPreparation.Inapplicable,
                -> null
                is EntryTranslationLanguagesMigrationPreparation.TypeMismatch -> error(
                    "Translation-language Migration requires matching Entry types",
                )
            }
        },
        deliveryHandler = { payload ->
            require(payload.schemaVersion == 1) {
                "Unsupported translation-language Migration payload ${payload.schemaVersion}"
            }
            val decoded = json.decodeFromString(EntryTranslationLanguagesMigrationPayload.serializer(), payload.value)
            check(feature().applyMigration(decoded) is EntryTranslationLanguagesWriteResult.Applied)
        },
    )
}
