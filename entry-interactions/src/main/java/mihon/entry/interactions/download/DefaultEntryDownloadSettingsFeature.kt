package mihon.entry.interactions

import mihon.feature.graph.FeatureArtifactId
import mihon.feature.graph.FeatureGraphEvaluation
import mihon.feature.graph.FeatureIntegrationId

internal class DefaultEntryDownloadSettingsFeature(
    evaluation: FeatureGraphEvaluation,
) : EntryDownloadSettingsFeature {
    override val availableSettings: Set<EntryDownloadSetting> = buildSet {
        addWhenApplicable(
            evaluation,
            EntryDownloadSetting.ARCHIVE_PACKAGING,
            ENTRY_DOWNLOAD_ARCHIVE_PACKAGING_INTEGRATION_ID,
            ENTRY_DOWNLOAD_ARCHIVE_PACKAGING_BEHAVIOR_ID,
        )
        addWhenApplicable(
            evaluation,
            EntryDownloadSetting.TALL_IMAGE_SPLITTING,
            ENTRY_DOWNLOAD_TALL_IMAGE_SPLITTING_INTEGRATION_ID,
            ENTRY_DOWNLOAD_TALL_IMAGE_SPLITTING_BEHAVIOR_ID,
        )
        addWhenApplicable(
            evaluation,
            EntryDownloadSetting.PARALLEL_SOURCE_TRANSFERS,
            ENTRY_DOWNLOAD_PARALLEL_SOURCE_TRANSFERS_INTEGRATION_ID,
            ENTRY_DOWNLOAD_PARALLEL_SOURCE_TRANSFERS_BEHAVIOR_ID,
        )
        addWhenApplicable(
            evaluation,
            EntryDownloadSetting.PARALLEL_ITEM_TRANSFERS,
            ENTRY_DOWNLOAD_PARALLEL_ITEM_TRANSFERS_INTEGRATION_ID,
            ENTRY_DOWNLOAD_PARALLEL_ITEM_TRANSFERS_BEHAVIOR_ID,
        )
    }
}

private fun MutableSet<EntryDownloadSetting>.addWhenApplicable(
    evaluation: FeatureGraphEvaluation,
    setting: EntryDownloadSetting,
    integration: FeatureIntegrationId,
    behaviorProjection: FeatureArtifactId,
) {
    val providerTypes = evaluation.applicableProviderTypes<EntryDownloadSettingProvider>(
        feature = ENTRY_DOWNLOAD_CONFIGURATION_FEATURE_ID,
        integration = integration,
        behaviorProjection = behaviorProjection,
    )
    if (providerTypes.isNotEmpty()) add(setting)
}
