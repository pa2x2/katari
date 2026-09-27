package mihon.entry.interactions.translation

import mihon.entry.interactions.state.EntryMigrationCapability
import mihon.entry.interactions.state.EntryTranslationLanguagesCapability
import mihon.feature.graph.CapabilityExpression
import mihon.feature.graph.ContributionOwner
import mihon.feature.graph.FeatureArtifactId
import mihon.feature.graph.FeatureBehaviorContract
import mihon.feature.graph.FeatureBehaviorProjection
import mihon.feature.graph.FeatureContribution
import mihon.feature.graph.FeatureGraphContributionSink
import mihon.feature.graph.FeatureGraphContributor
import mihon.feature.graph.FeatureId
import mihon.feature.graph.FeatureIntegration
import mihon.feature.graph.FeatureIntegrationId
import mihon.feature.graph.allOf

internal val ENTRY_TRANSLATION_LANGUAGES_FEATURE_ID = FeatureId("entry.translation-languages")
internal val ENTRY_TRANSLATION_LANGUAGES_FEATURE_OWNER = ContributionOwner("entry-translation-languages")
internal val ENTRY_TRANSLATION_LANGUAGES_INTEGRATION_ID = FeatureIntegrationId("entry.translation-languages.provider")
internal val ENTRY_TRANSLATION_LANGUAGES_MIGRATION_INTEGRATION_ID =
    FeatureIntegrationId("entry.translation-languages.migration")

internal enum class EntryTranslationLanguagesBehavior(
    override val id: FeatureArtifactId,
) : FeatureBehaviorProjection {
    SERIES_LANGUAGES(FeatureArtifactId("entry.translation-languages.series-languages")),
    BACKUP_SNAPSHOT(FeatureArtifactId("entry.translation-languages.backup-snapshot")),
    BACKUP_RESTORE(FeatureArtifactId("entry.translation-languages.backup-restore")),
}

internal object EntryTranslationLanguagesMigrationBehavior : FeatureBehaviorProjection {
    override val id = FeatureArtifactId("entry.translation-languages.migration-target")
}

internal object EntryTranslationLanguagesBehaviorContract : FeatureBehaviorContract {
    override val id = FeatureArtifactId("entry.translation-languages.behavior")
}

internal object EntryTranslationLanguagesFeatureContributor : FeatureGraphContributor {
    override val owner = ENTRY_TRANSLATION_LANGUAGES_FEATURE_OWNER

    override fun contributeTo(sink: FeatureGraphContributionSink) {
        sink.add(
            FeatureContribution(
                feature = ENTRY_TRANSLATION_LANGUAGES_FEATURE_ID,
                owner = owner,
                integrations = listOf(
                    FeatureIntegration(
                        id = ENTRY_TRANSLATION_LANGUAGES_INTEGRATION_ID,
                        prerequisites = CapabilityExpression.Provided(EntryTranslationLanguagesCapability.definition),
                        behaviorProjections = EntryTranslationLanguagesBehavior.entries,
                        behavioralContracts = listOf(EntryTranslationLanguagesBehaviorContract),
                    ),
                    FeatureIntegration(
                        id = ENTRY_TRANSLATION_LANGUAGES_MIGRATION_INTEGRATION_ID,
                        prerequisites = allOf(
                            CapabilityExpression.Provided(EntryTranslationLanguagesCapability.definition),
                            CapabilityExpression.Provided(EntryMigrationCapability.definition),
                        ),
                        behaviorProjections = listOf(EntryTranslationLanguagesMigrationBehavior),
                    ),
                ),
            ),
        )
    }
}
