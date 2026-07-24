package mihon.entry.interactions

import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import mihon.entry.interactions.validation.contractExpectation
import mihon.entry.interactions.validation.productionSubjectEvaluation
import mihon.entry.interactions.validation.verifyFeatureContract
import mihon.feature.graph.FeatureContractScenarioId
import mihon.feature.graph.contextEvidence
import mihon.feature.graph.validation.FeatureContractReference
import mihon.feature.graph.validation.FeatureContractScenario
import mihon.feature.graph.validation.FeatureContractVerifier
import mihon.feature.graph.validation.FeatureExecutionContractReference
import mihon.feature.graph.validation.FeatureExecutionContractVerifier
import mihon.feature.graph.validation.FeatureValidationContributionSink
import mihon.feature.graph.validation.FeatureValidationContributor
import tachiyomi.domain.entry.model.Entry
import tachiyomi.domain.entry.model.EntryChapter

class EntryAutomaticDownloadContractValidationContributor : FeatureValidationContributor {
    override val owner = EntryAutomaticDownloadFeatureContributor.owner

    override fun contributeTo(sink: FeatureValidationContributionSink) {
        val providerReference = FeatureContractReference(
            ENTRY_AUTOMATIC_DOWNLOAD_FEATURE_ID,
            EntryAutomaticDownloadProviderBehaviorContract,
        )
        val contextReference = FeatureContractReference(
            ENTRY_AUTOMATIC_DOWNLOAD_FEATURE_ID,
            EntryAutomaticDownloadContextBehaviorContract,
        )
        sink.add(FeatureContractVerifier(providerReference, ::verifyAutomaticDownload))
        sink.add(FeatureContractVerifier(contextReference, ::verifyAutomaticDownload))
        sink.add(
            FeatureExecutionContractVerifier(
                FeatureExecutionContractReference(
                    ENTRY_AUTOMATIC_DOWNLOAD_SOURCE_REFRESH_PARTICIPANT.id,
                    EntryAutomaticDownloadSourceRefreshBehaviorContract,
                ),
            ) { input ->
                verifyFeatureContract {
                    val provider = input.provider(EntryDownloadCapability.definition)
                    val entry = Entry.create().copy(id = 75L, type = provider.type)
                    val chapter = EntryChapter.create().copy(id = 76L, entryId = entry.id)
                    val feature = mockk<EntryAutomaticDownloadCoordinator> {
                        coEvery {
                            downloadAfterEntryRefresh(entry, listOf(chapter))
                        } returns EntryAutomaticDownloadResult.Scheduled(1)
                    }

                    entryAutomaticDownloadSourceRefreshBinding { feature }
                        .handler
                        .execute(EntrySourceRefreshNewChildrenEvent(entry, listOf(chapter)))

                    coVerify(exactly = 1) { feature.downloadAfterEntryRefresh(entry, listOf(chapter)) }
                }
            },
        )
        sink.add(
            FeatureExecutionContractVerifier(
                FeatureExecutionContractReference(
                    ENTRY_AUTOMATIC_DOWNLOAD_LIBRARY_UPDATE_PARTICIPANT.id,
                    EntryAutomaticDownloadLibraryUpdateBehaviorContract,
                ),
            ) { input ->
                verifyFeatureContract {
                    val provider = input.provider(EntryDownloadCapability.definition)
                    val entry = Entry.create().copy(id = 77L, type = provider.type)
                    val chapter = EntryChapter.create().copy(id = 78L, entryId = entry.id)
                    val batch = mockk<EntryAutomaticDownloadBatch> {
                        coEvery { enqueue(entry, listOf(chapter)) } returns
                            EntryAutomaticDownloadResult.Scheduled(1)
                        every { complete() } returns Unit
                    }
                    val feature = mockk<EntryAutomaticDownloadCoordinator> {
                        every { newLibraryUpdateBatch() } returns batch
                    }
                    val session = EntryLibraryUpdateExecutionSession()
                    val binding = entryAutomaticDownloadLibraryUpdateBinding { feature }
                    val event = EntryLibraryUpdateNewChildrenEvent(entry, listOf(chapter), session)

                    binding.handler.execute(event)
                    binding.handler.execute(event)
                    session.complete()
                    session.complete()

                    verify(exactly = 1) { feature.newLibraryUpdateBatch() }
                    coVerify(exactly = 2) { batch.enqueue(entry, listOf(chapter)) }
                    verify(exactly = 1) { batch.complete() }
                }
            },
        )
        sink.add(
            FeatureContractScenario(
                FeatureContractScenarioId("entry.download.automatic.context.applicable"),
                contextReference,
                ENTRY_AUTOMATIC_DOWNLOAD_CONTEXT_INTEGRATION,
            ) {
                listOf(
                    contextEvidence(ENTRY_AUTOMATIC_DOWNLOAD_NEW_CHILDREN_CONTEXT, true),
                    contextEvidence(ENTRY_AUTOMATIC_DOWNLOAD_ENABLED_CONTEXT, true),
                    contextEvidence(ENTRY_AUTOMATIC_DOWNLOAD_FAVORITE_CONTEXT, true),
                    contextEvidence(ENTRY_AUTOMATIC_DOWNLOAD_CATEGORY_ALLOWED_CONTEXT, true),
                    contextEvidence(ENTRY_AUTOMATIC_DOWNLOAD_UNREAD_ONLY_CONTEXT, false),
                    contextEvidence(ENTRY_AUTOMATIC_DOWNLOAD_CANDIDATES_CONTEXT, true),
                )
            },
        )
    }
}

private suspend fun verifyAutomaticDownload(input: mihon.feature.graph.validation.FeatureContractExecutionInput) =
    verifyFeatureContract {
        val provider = input.provider(EntryDownloadCapability.definition)
        val evaluation = productionSubjectEvaluation(
            EntryDownloadCapability.bind(provider),
            EntryAutomaticDownloadFeatureContributor,
        )
        val entry = Entry.create().copy(id = 73L, type = provider.type, favorite = true)
        val chapter = EntryChapter.create().copy(id = 74L, entryId = entry.id)
        val policy = mockk<EntryAutomaticDownloadPolicy> {
            coEvery { evaluate(entry, listOf(chapter)) } returns EntryAutomaticDownloadPolicyDecision(
                candidates = listOf(chapter),
                hasNewChapters = true,
                enabled = true,
                favorite = true,
                categoryAllowed = true,
                unreadOnly = false,
            )
        }
        val feature = DefaultEntryAutomaticDownloadFeature(evaluation, recordingDownloadInteraction(), policy)

        contractExpectation(feature.isApplicable(provider.type), "Automatic Download must be applicable")
        contractExpectation(
            feature.downloadAfterEntryRefresh(entry, listOf(chapter)) == EntryAutomaticDownloadResult.Scheduled(1),
            "Automatic Download must schedule candidates accepted by shared policy",
        )
    }
