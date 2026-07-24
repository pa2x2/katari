package mihon.entry.interactions

import mihon.entry.interactions.documentation.EntryContentTypeReferenceContribution
import mihon.entry.interactions.documentation.EntryContentTypeReferenceSection
import mihon.entry.interactions.documentation.entryContentTypeReferenceContribution
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

internal val ENTRY_DOWNLOAD_CONFIGURATION_FEATURE_ID = FeatureId("entry.download.configuration")
internal val ENTRY_DOWNLOAD_CONFIGURATION_FEATURE_OWNER = ContributionOwner("entry-download-configuration")
private val ENTRY_DOWNLOAD_CONFIGURATION_REFERENCE = entryContentTypeReferenceContribution(
    id = "download-configuration",
    owner = ENTRY_DOWNLOAD_CONFIGURATION_FEATURE_OWNER,
    section = EntryContentTypeReferenceSection.DOWNLOADS,
    label = "Configure type-specific download behavior",
    order = 600,
)

internal val ENTRY_DOWNLOAD_OPTIONS_INTEGRATION_ID = FeatureIntegrationId("entry.download.configuration.options")
internal val ENTRY_DOWNLOAD_OPTIONS_BEHAVIOR_ID = FeatureArtifactId("entry.download.configuration.options.dispatch")

internal val ENTRY_DOWNLOAD_ARCHIVE_PACKAGING_INTEGRATION_ID =
    FeatureIntegrationId("entry.download.configuration.setting.archive-packaging")
internal val ENTRY_DOWNLOAD_ARCHIVE_PACKAGING_BEHAVIOR_ID =
    FeatureArtifactId("entry.download.configuration.setting.archive-packaging.visibility")

internal val ENTRY_DOWNLOAD_TALL_IMAGE_SPLITTING_INTEGRATION_ID =
    FeatureIntegrationId("entry.download.configuration.setting.tall-image-splitting")
internal val ENTRY_DOWNLOAD_TALL_IMAGE_SPLITTING_BEHAVIOR_ID =
    FeatureArtifactId("entry.download.configuration.setting.tall-image-splitting.visibility")

internal val ENTRY_DOWNLOAD_PARALLEL_SOURCE_TRANSFERS_INTEGRATION_ID =
    FeatureIntegrationId("entry.download.configuration.setting.parallel-source-transfers")
internal val ENTRY_DOWNLOAD_PARALLEL_SOURCE_TRANSFERS_BEHAVIOR_ID =
    FeatureArtifactId("entry.download.configuration.setting.parallel-source-transfers.visibility")

internal val ENTRY_DOWNLOAD_PARALLEL_ITEM_TRANSFERS_INTEGRATION_ID =
    FeatureIntegrationId("entry.download.configuration.setting.parallel-item-transfers")
internal val ENTRY_DOWNLOAD_PARALLEL_ITEM_TRANSFERS_BEHAVIOR_ID =
    FeatureArtifactId("entry.download.configuration.setting.parallel-item-transfers.visibility")

internal object EntryDownloadOptionsBehaviorContract : FeatureBehaviorContract {
    override val id = FeatureArtifactId("entry.download.configuration.options-behavior")
}

internal object EntryDownloadArchivePackagingBehaviorContract : FeatureBehaviorContract {
    override val id = FeatureArtifactId("entry.download.configuration.setting.archive-packaging-behavior")
}

internal object EntryDownloadTallImageSplittingBehaviorContract : FeatureBehaviorContract {
    override val id = FeatureArtifactId("entry.download.configuration.setting.tall-image-splitting-behavior")
}

internal object EntryDownloadParallelSourceTransfersBehaviorContract : FeatureBehaviorContract {
    override val id = FeatureArtifactId("entry.download.configuration.setting.parallel-source-transfers-behavior")
}

internal object EntryDownloadParallelItemTransfersBehaviorContract : FeatureBehaviorContract {
    override val id = FeatureArtifactId("entry.download.configuration.setting.parallel-item-transfers-behavior")
}

private class EntryDownloadConfigurationBehavior(
    override val id: FeatureArtifactId,
) : FeatureBehaviorProjection

internal object EntryDownloadConfigurationFeatureContributor : FeatureGraphContributor {
    override val owner = ENTRY_DOWNLOAD_CONFIGURATION_FEATURE_OWNER

    override fun contributeTo(sink: FeatureGraphContributionSink) {
        sink.add(
            FeatureContribution(
                feature = ENTRY_DOWNLOAD_CONFIGURATION_FEATURE_ID,
                owner = owner,
                integrations = listOf(
                    integration(
                        ENTRY_DOWNLOAD_OPTIONS_INTEGRATION_ID,
                        EntryDownloadOptionsCapability,
                        ENTRY_DOWNLOAD_OPTIONS_BEHAVIOR_ID,
                        EntryDownloadOptionsBehaviorContract,
                        ENTRY_DOWNLOAD_CONFIGURATION_REFERENCE,
                    ),
                    integration(
                        ENTRY_DOWNLOAD_ARCHIVE_PACKAGING_INTEGRATION_ID,
                        EntryDownloadArchivePackagingCapability,
                        ENTRY_DOWNLOAD_ARCHIVE_PACKAGING_BEHAVIOR_ID,
                        EntryDownloadArchivePackagingBehaviorContract,
                        ENTRY_DOWNLOAD_CONFIGURATION_REFERENCE,
                    ),
                    integration(
                        ENTRY_DOWNLOAD_TALL_IMAGE_SPLITTING_INTEGRATION_ID,
                        EntryDownloadTallImageSplittingCapability,
                        ENTRY_DOWNLOAD_TALL_IMAGE_SPLITTING_BEHAVIOR_ID,
                        EntryDownloadTallImageSplittingBehaviorContract,
                        ENTRY_DOWNLOAD_CONFIGURATION_REFERENCE,
                    ),
                    integration(
                        ENTRY_DOWNLOAD_PARALLEL_SOURCE_TRANSFERS_INTEGRATION_ID,
                        EntryDownloadParallelSourceTransfersCapability,
                        ENTRY_DOWNLOAD_PARALLEL_SOURCE_TRANSFERS_BEHAVIOR_ID,
                        EntryDownloadParallelSourceTransfersBehaviorContract,
                        ENTRY_DOWNLOAD_CONFIGURATION_REFERENCE,
                    ),
                    integration(
                        ENTRY_DOWNLOAD_PARALLEL_ITEM_TRANSFERS_INTEGRATION_ID,
                        EntryDownloadParallelItemTransfersCapability,
                        ENTRY_DOWNLOAD_PARALLEL_ITEM_TRANSFERS_BEHAVIOR_ID,
                        EntryDownloadParallelItemTransfersBehaviorContract,
                        ENTRY_DOWNLOAD_CONFIGURATION_REFERENCE,
                    ),
                ),
            ),
        )
    }

    private fun integration(
        id: FeatureIntegrationId,
        capability: EntryInteractionCapability<*>,
        behaviorProjection: FeatureArtifactId,
        contract: FeatureBehaviorContract,
        reference: EntryContentTypeReferenceContribution,
    ): FeatureIntegration {
        return FeatureIntegration(
            id = id,
            prerequisites = CapabilityExpression.Provided(capability.definition),
            behaviorProjections = listOf(EntryDownloadConfigurationBehavior(behaviorProjection)),
            behavioralContracts = listOf(contract),
            projectionRequirements = listOf(reference.requirement),
            projections = listOf(reference.projection),
        )
    }
}
