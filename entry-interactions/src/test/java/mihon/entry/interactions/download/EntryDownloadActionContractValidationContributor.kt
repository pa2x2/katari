package mihon.entry.interactions

import eu.kanade.tachiyomi.source.entry.EntryType
import io.mockk.coEvery
import io.mockk.every
import mihon.entry.interactions.validation.contractExpectation
import mihon.entry.interactions.validation.productionSubjectEvaluation
import mihon.entry.interactions.validation.verifyFeatureContract
import mihon.feature.graph.FeatureBehaviorContract
import mihon.feature.graph.FeatureContractScenarioId
import mihon.feature.graph.FeatureIntegrationId
import mihon.feature.graph.contextEvidence
import mihon.feature.graph.validation.FeatureContractExecutionInput
import mihon.feature.graph.validation.FeatureContractReference
import mihon.feature.graph.validation.FeatureContractScenario
import mihon.feature.graph.validation.FeatureContractVerificationResult
import mihon.feature.graph.validation.FeatureContractVerifier
import mihon.feature.graph.validation.FeatureValidationContributionSink
import mihon.feature.graph.validation.FeatureValidationContributor
import tachiyomi.domain.entry.model.Entry
import tachiyomi.domain.entry.model.EntryChapter

class EntryDownloadActionContractValidationContributor : FeatureValidationContributor {
    override val owner = EntryDownloadActionFeatureContributor.owner

    override fun contributeTo(sink: FeatureValidationContributionSink) {
        actionContracts.forEach { action ->
            val reference = FeatureContractReference(ENTRY_DOWNLOAD_ACTION_FEATURE_ID, action.contract)
            sink.add(FeatureContractVerifier(reference) { input -> verifyDownloadAction(input) })
            action.scenarioEvidence?.let { evidence ->
                sink.add(
                    FeatureContractScenario(
                        FeatureContractScenarioId("${action.integration.value}.applicable"),
                        reference,
                        action.integration,
                    ) { evidence() },
                )
            }
        }
    }
}

private data class DownloadActionContract(
    val integration: FeatureIntegrationId,
    val contract: FeatureBehaviorContract,
    val scenarioEvidence: (() -> List<mihon.feature.graph.ContextEvidence<*>>)? = null,
)

private val remoteEvidence = {
    listOf(contextEvidence(ENTRY_DOWNLOAD_SOURCE_ACCESS_CONTEXT, EntryDownloadSourceAccess.REMOTE))
}
private val remoteSelectionEvidence = {
    listOf(
        contextEvidence(ENTRY_DOWNLOAD_SOURCE_ACCESS_CONTEXT, EntryDownloadSourceAccess.REMOTE),
        contextEvidence(ENTRY_DOWNLOAD_SELECTION_CONTEXT, EntryDownloadSelectionState.ACTIONABLE),
    )
}

private val actionContracts = listOf(
    DownloadActionContract(
        ENTRY_DOWNLOAD_INDIVIDUAL_PROVIDER_INTEGRATION,
        EntryDownloadIndividualProviderBehaviorContract,
    ),
    DownloadActionContract(ENTRY_DOWNLOAD_BULK_PROVIDER_INTEGRATION, EntryDownloadBulkProviderBehaviorContract),
    DownloadActionContract(
        ENTRY_DOWNLOAD_BOOKMARKED_BULK_PROVIDER_INTEGRATION,
        EntryDownloadBookmarkedBulkProviderBehaviorContract,
    ),
    DownloadActionContract(
        ENTRY_DOWNLOAD_INDIVIDUAL_CONTEXT_INTEGRATION,
        EntryDownloadIndividualContextBehaviorContract,
        remoteEvidence,
    ),
    DownloadActionContract(
        ENTRY_DOWNLOAD_INDIVIDUAL_OPERATION_INTEGRATION,
        EntryDownloadIndividualOperationBehaviorContract,
        remoteSelectionEvidence,
    ),
    DownloadActionContract(
        ENTRY_DOWNLOAD_BULK_CONTEXT_INTEGRATION,
        EntryDownloadBulkContextBehaviorContract,
        remoteEvidence,
    ),
    DownloadActionContract(
        ENTRY_DOWNLOAD_BOOKMARKED_BULK_CONTEXT_INTEGRATION,
        EntryDownloadBookmarkedBulkContextBehaviorContract,
        remoteEvidence,
    ),
    DownloadActionContract(
        ENTRY_DOWNLOAD_NOTIFICATION_CONTEXT_INTEGRATION,
        EntryDownloadNotificationContextBehaviorContract,
        remoteSelectionEvidence,
    ),
)

private suspend fun verifyDownloadAction(
    input: FeatureContractExecutionInput,
): FeatureContractVerificationResult = verifyFeatureContract {
    val download = input.provider(EntryDownloadCapability.definition)
    val integration = input.subject.integration
    val bindings = buildList {
        add(EntryDownloadCapability.bind(download))
        if (integration.isBulkAction()) {
            add(
                EntryBulkDownloadCandidateCapability.bind(
                    input.provider(EntryBulkDownloadCandidateCapability.definition),
                ),
            )
        }
        if (integration.isBookmarkedBulkAction()) {
            add(EntryBookmarkCapability.bind(input.provider(EntryBookmarkCapability.definition)))
        }
    }
    val evaluation = productionSubjectEvaluation(bindings, EntryDownloadActionFeatureContributor)
    val entry = Entry.create().copy(id = 71L, type = download.type)
    val chapter = EntryChapter.create().copy(id = 72L, entryId = entry.id, bookmark = true)
    val request = EntryDownloadActionRequest.forEntry(entry)
    val interaction = recordingDownloadInteraction()
    var started = false
    every { interaction.startDownloads() } answers { started = true }
    coEvery { interaction.resolveBulkDownloadCandidatePool(entry, any()) } returns listOf(chapter)
    val feature = DefaultEntryDownloadActionFeature(
        evaluation,
        interaction,
        EntryDownloadSourceAccessResolver { EntryDownloadSourceAccess.REMOTE },
    )

    when (integration) {
        ENTRY_DOWNLOAD_INDIVIDUAL_PROVIDER_INTEGRATION,
        ENTRY_DOWNLOAD_INDIVIDUAL_CONTEXT_INTEGRATION,
        -> {
            contractExpectation(
                feature.individualAvailability(request) == EntryDownloadActionAvailability.Available,
                "Download Actions must expose individual availability",
            )
            contractExpectation(
                feature.retry(listOf(request)) == EntryDownloadActionResult.Performed && started,
                "Download Actions must dispatch an applicable individual action",
            )
        }
        ENTRY_DOWNLOAD_INDIVIDUAL_OPERATION_INTEGRATION -> contractExpectation(
            feature.download(entry, listOf(chapter)) == EntryDownloadActionResult.Performed,
            "Download Actions must dispatch an applicable individual operation",
        )
        ENTRY_DOWNLOAD_BULK_PROVIDER_INTEGRATION,
        ENTRY_DOWNLOAD_BULK_CONTEXT_INTEGRATION,
        -> contractExpectation(
            feature.resolveBulkDownloadCandidates(EntryBulkDownloadRequest(entry, EntryBulkDownloadAction.unread)) ==
                EntryBulkDownloadResolutionResult.Candidates(listOf(chapter)),
            "Download Actions must resolve shared bulk candidates",
        )
        ENTRY_DOWNLOAD_BOOKMARKED_BULK_PROVIDER_INTEGRATION,
        ENTRY_DOWNLOAD_BOOKMARKED_BULK_CONTEXT_INTEGRATION,
        -> contractExpectation(
            feature.resolveBulkDownloadCandidates(
                EntryBulkDownloadRequest(entry, EntryBulkDownloadAction.bookmarked),
            ) ==
                EntryBulkDownloadResolutionResult.Candidates(listOf(chapter)),
            "Download Actions must resolve bookmarked bulk candidates",
        )
        ENTRY_DOWNLOAD_NOTIFICATION_CONTEXT_INTEGRATION -> contractExpectation(
            feature.notificationAvailability(entry, childCount = 1) == EntryDownloadActionAvailability.Available,
            "Download Actions must expose an applicable notification action",
        )
        else -> error("Unexpected Download Actions integration $integration")
    }
}

private fun FeatureIntegrationId.isBulkAction(): Boolean = this in setOf(
    ENTRY_DOWNLOAD_BULK_PROVIDER_INTEGRATION,
    ENTRY_DOWNLOAD_BOOKMARKED_BULK_PROVIDER_INTEGRATION,
    ENTRY_DOWNLOAD_BULK_CONTEXT_INTEGRATION,
    ENTRY_DOWNLOAD_BOOKMARKED_BULK_CONTEXT_INTEGRATION,
)

private fun FeatureIntegrationId.isBookmarkedBulkAction(): Boolean = this in setOf(
    ENTRY_DOWNLOAD_BOOKMARKED_BULK_PROVIDER_INTEGRATION,
    ENTRY_DOWNLOAD_BOOKMARKED_BULK_CONTEXT_INTEGRATION,
)
