package mihon.entry.interactions

import io.mockk.coEvery
import mihon.entry.interactions.validation.contractExpectation
import mihon.entry.interactions.validation.productionSubjectEvaluation
import mihon.entry.interactions.validation.verifyFeatureContract
import mihon.feature.graph.FeatureBehaviorContract
import mihon.feature.graph.FeatureIntegrationId
import mihon.feature.graph.validation.FeatureContractExecutionInput
import mihon.feature.graph.validation.FeatureContractReference
import mihon.feature.graph.validation.FeatureContractVerifier
import mihon.feature.graph.validation.FeatureValidationContributionSink
import mihon.feature.graph.validation.FeatureValidationContributor
import tachiyomi.domain.entry.model.Entry
import tachiyomi.domain.entry.model.EntryChapter

class EntryDownloadConfigurationContractValidationContributor : FeatureValidationContributor {
    override val owner = EntryDownloadConfigurationFeatureContributor.owner

    override fun contributeTo(sink: FeatureValidationContributionSink) {
        sink.addEntryBackupParticipationContract(
            ENTRY_DOWNLOAD_CONFIGURATION_BACKUP_SNAPSHOT_PARTICIPANT,
            EntryDownloadOptionsBehaviorContract,
            EntryDownloadConfigurationBackupState.serializer(),
            EntryDownloadConfigurationBackupState(dubKey = "dub"),
        )
        sink.addEntryBackupParticipationContract(
            ENTRY_DOWNLOAD_CONFIGURATION_BACKUP_RESTORE_PARTICIPANT,
            EntryDownloadOptionsBehaviorContract,
            EntryDownloadConfigurationBackupState.serializer(),
            EntryDownloadConfigurationBackupState(dubKey = "dub"),
        )
        configurations.forEach { configuration ->
            sink.add(
                FeatureContractVerifier(
                    FeatureContractReference(ENTRY_DOWNLOAD_CONFIGURATION_FEATURE_ID, configuration.contract),
                ) { input -> verifyConfiguration(input, configuration) },
            )
        }
    }
}

private data class DownloadConfigurationContract(
    val integration: FeatureIntegrationId,
    val contract: FeatureBehaviorContract,
    val setting: EntryDownloadSetting? = null,
)

private val configurations = listOf(
    DownloadConfigurationContract(ENTRY_DOWNLOAD_OPTIONS_INTEGRATION_ID, EntryDownloadOptionsBehaviorContract),
    DownloadConfigurationContract(
        ENTRY_DOWNLOAD_ARCHIVE_PACKAGING_INTEGRATION_ID,
        EntryDownloadArchivePackagingBehaviorContract,
        EntryDownloadSetting.ARCHIVE_PACKAGING,
    ),
    DownloadConfigurationContract(
        ENTRY_DOWNLOAD_TALL_IMAGE_SPLITTING_INTEGRATION_ID,
        EntryDownloadTallImageSplittingBehaviorContract,
        EntryDownloadSetting.TALL_IMAGE_SPLITTING,
    ),
    DownloadConfigurationContract(
        ENTRY_DOWNLOAD_PARALLEL_SOURCE_TRANSFERS_INTEGRATION_ID,
        EntryDownloadParallelSourceTransfersBehaviorContract,
        EntryDownloadSetting.PARALLEL_SOURCE_TRANSFERS,
    ),
    DownloadConfigurationContract(
        ENTRY_DOWNLOAD_PARALLEL_ITEM_TRANSFERS_INTEGRATION_ID,
        EntryDownloadParallelItemTransfersBehaviorContract,
        EntryDownloadSetting.PARALLEL_ITEM_TRANSFERS,
    ),
)

private suspend fun verifyConfiguration(
    input: FeatureContractExecutionInput,
    configuration: DownloadConfigurationContract,
) = verifyFeatureContract {
    contractExpectation(
        input.subject.integration == configuration.integration,
        "Download Configuration contract selected the wrong integration",
    )
    if (configuration.setting == null) {
        val provider = input.provider(EntryDownloadOptionsCapability.definition)
        val evaluation = productionSubjectEvaluation(
            EntryDownloadOptionsCapability.bind(provider),
            EntryDownloadConfigurationFeatureContributor,
        )
        val entry = Entry.create().copy(id = 80L, type = provider.type)
        val chapter = EntryChapter.create().copy(id = 81L, entryId = entry.id)
        val selection = EntryDownloadOptionSelection(emptyMap())
        var dispatched = false
        val interaction = recordingDownloadInteraction()
        coEvery {
            interaction.downloadWithOptions(entry, listOf(chapter), selection, startNow = false)
        } answers { dispatched = true }
        val feature = DefaultEntryDownloadOptionsFeature(evaluation, interaction)
        contractExpectation(feature.isApplicable(provider.type), "Download Options must be applicable")
        contractExpectation(
            feature.download(entry, listOf(chapter), selection) == EntryDownloadOptionsActionResult.Performed,
            "Download Options must accept a selected option set",
        )
        contractExpectation(dispatched, "Download Options must dispatch through its shared boundary")
    } else {
        val capability = when (configuration.setting) {
            EntryDownloadSetting.ARCHIVE_PACKAGING -> EntryDownloadArchivePackagingCapability
            EntryDownloadSetting.TALL_IMAGE_SPLITTING -> EntryDownloadTallImageSplittingCapability
            EntryDownloadSetting.PARALLEL_SOURCE_TRANSFERS -> EntryDownloadParallelSourceTransfersCapability
            EntryDownloadSetting.PARALLEL_ITEM_TRANSFERS -> EntryDownloadParallelItemTransfersCapability
        }
        val provider = input.provider(capability.definition)
        val evaluation = productionSubjectEvaluation(
            capability.bind(provider),
            EntryDownloadConfigurationFeatureContributor,
        )
        val feature = DefaultEntryDownloadSettingsFeature(evaluation)
        contractExpectation(
            configuration.setting in feature.availableSettings,
            "Download Configuration did not expose ${configuration.setting}",
        )
    }
}
