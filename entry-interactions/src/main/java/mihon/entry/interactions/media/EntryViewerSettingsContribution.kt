package mihon.entry.interactions

import eu.kanade.tachiyomi.source.entry.EntryType
import mihon.entry.interactions.documentation.EntryContentTypeReferenceSection
import mihon.entry.interactions.documentation.entryContentTypeReferenceContribution
import mihon.entry.viewer.settings.ViewerSettingDefinition
import mihon.entry.viewer.settings.ViewerSettingId
import mihon.entry.viewer.settings.ViewerSettingOverride
import mihon.entry.viewer.settings.ViewerSettingOverrideRepository
import mihon.entry.viewer.settings.ViewerSettingScope
import mihon.entry.viewer.settings.ViewerSettingsProvider
import mihon.feature.graph.CapabilityExpression
import mihon.feature.graph.ContributionOwner
import mihon.feature.graph.FeatureArtifactId
import mihon.feature.graph.FeatureBehaviorContract
import mihon.feature.graph.FeatureBehaviorProjection
import mihon.feature.graph.FeatureContribution
import mihon.feature.graph.FeatureGraphContributionSink
import mihon.feature.graph.FeatureGraphContributor
import mihon.feature.graph.FeatureGraphEvaluation
import mihon.feature.graph.FeatureId
import mihon.feature.graph.FeatureIntegration
import mihon.feature.graph.FeatureIntegrationId
import mihon.feature.graph.allOf
import tachiyomi.domain.entry.model.Entry

internal val ENTRY_VIEWER_SETTINGS_FEATURE_ID = FeatureId("entry.viewer-settings")
internal val ENTRY_VIEWER_SETTINGS_FEATURE_OWNER = ContributionOwner("entry-viewer-settings")
private val ENTRY_VIEWER_SETTINGS_REFERENCE = entryContentTypeReferenceContribution(
    id = "viewer-settings",
    owner = ENTRY_VIEWER_SETTINGS_FEATURE_OWNER,
    section = EntryContentTypeReferenceSection.ENTRY_INTERACTIONS,
    label = "Configure media viewer settings",
    order = 1200,
)
internal val ENTRY_VIEWER_SETTINGS_PROVIDER_INTEGRATION_ID = FeatureIntegrationId("entry.viewer-settings.provider")
internal val ENTRY_VIEWER_SETTINGS_MIGRATION_INTEGRATION_ID = FeatureIntegrationId("entry.viewer-settings.migration")

internal enum class EntryViewerSettingsBehavior(
    override val id: FeatureArtifactId,
) : FeatureBehaviorProjection {
    DISCOVERY(FeatureArtifactId("entry.viewer-settings.discovery")),
    SETTINGS_HUB(FeatureArtifactId("entry.viewer-settings.settings-hub")),
    SCREEN_PROJECTION(FeatureArtifactId("entry.viewer-settings.screen-projection")),
    SEARCH_INDEX(FeatureArtifactId("entry.viewer-settings.search-index")),
    ENTRY_OVERRIDE(FeatureArtifactId("entry.viewer-settings.entry-override")),
    PREFERENCE_OWNERSHIP(FeatureArtifactId("entry.viewer-settings.preference-ownership")),
    RESET(FeatureArtifactId("entry.viewer-settings.reset")),
    BACKUP(FeatureArtifactId("entry.viewer-settings.backup")),
}

internal object EntryViewerSettingsMigrationBehavior : FeatureBehaviorProjection {
    override val id = FeatureArtifactId("entry.viewer-settings.migration")
}

internal object EntryViewerSettingsBehaviorContract : FeatureBehaviorContract {
    override val id = FeatureArtifactId("entry.viewer-settings.behavior")
}

internal object EntryViewerSettingsMigrationBehaviorContract : FeatureBehaviorContract {
    override val id = FeatureArtifactId("entry.viewer-settings.migration-behavior")
}

internal object EntryViewerSettingsFeatureContributor : FeatureGraphContributor {
    override val owner = ENTRY_VIEWER_SETTINGS_FEATURE_OWNER

    override fun contributeTo(sink: FeatureGraphContributionSink) {
        sink.add(
            FeatureContribution(
                feature = ENTRY_VIEWER_SETTINGS_FEATURE_ID,
                owner = owner,
                integrations = listOf(
                    FeatureIntegration(
                        id = ENTRY_VIEWER_SETTINGS_PROVIDER_INTEGRATION_ID,
                        prerequisites = CapabilityExpression.Provided(EntryViewerSettingsCapability.definition),
                        behaviorProjections = EntryViewerSettingsBehavior.entries,
                        behavioralContracts = listOf(EntryViewerSettingsBehaviorContract),
                        projectionRequirements = listOf(ENTRY_VIEWER_SETTINGS_REFERENCE.requirement),
                        projections = listOf(ENTRY_VIEWER_SETTINGS_REFERENCE.projection),
                    ),
                    FeatureIntegration(
                        id = ENTRY_VIEWER_SETTINGS_MIGRATION_INTEGRATION_ID,
                        prerequisites = allOf(
                            CapabilityExpression.Provided(EntryViewerSettingsCapability.definition),
                            CapabilityExpression.Provided(EntryMigrationCapability.definition),
                        ),
                        behaviorProjections = listOf(EntryViewerSettingsMigrationBehavior),
                        behavioralContracts = listOf(EntryViewerSettingsMigrationBehaviorContract),
                    ),
                ),
            ),
        )
    }
}
