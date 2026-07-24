package mihon.entry.interactions

import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
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
import mihon.feature.graph.validation.FeatureExecutionContractReference
import mihon.feature.graph.validation.FeatureExecutionContractVerifier
import mihon.feature.graph.validation.FeatureValidationContributionSink
import mihon.feature.graph.validation.FeatureValidationContributor
import tachiyomi.core.common.preference.InMemoryPreferenceStore
import tachiyomi.domain.category.interactor.GetCategories
import tachiyomi.domain.download.service.DownloadPreferences
import tachiyomi.domain.entry.interactor.GetEntryWithChapters
import tachiyomi.domain.entry.model.Entry
import tachiyomi.domain.entry.model.EntryChapter
import tachiyomi.domain.entry.repository.EntryRepository

class EntryDownloadLifecycleContractValidationContributor : FeatureValidationContributor {
    override val owner = EntryDownloadLifecycleFeatureContributor.owner

    override fun contributeTo(sink: FeatureValidationContributionSink) {
        lifecycleContracts.forEach { lifecycle ->
            val reference = FeatureContractReference(ENTRY_DOWNLOAD_LIFECYCLE_FEATURE_ID, lifecycle.contract)
            sink.add(FeatureContractVerifier(reference, ::verifyDownloadLifecycle))
            lifecycle.scenarioEvidence?.let { evidence ->
                sink.add(
                    FeatureContractScenario(
                        FeatureContractScenarioId("${lifecycle.integration.value}.applicable"),
                        reference,
                        lifecycle.integration,
                    ) { evidence() },
                )
            }
        }
        sink.add(
            FeatureExecutionContractVerifier(
                FeatureExecutionContractReference(
                    ENTRY_DOWNLOAD_MEDIA_SESSION_PARTICIPANT.id,
                    EntryDownloadMediaSessionBehaviorContract,
                ),
            ) { input ->
                verifyFeatureContract {
                    val event = mediaSessionContractEvent(
                        input.provider(EntryDownloadCapability.definition).type,
                    )
                    val received = mutableListOf<EntryDownloadLifecycleEvent>()
                    val feature = object : EntryDownloadLifecycleFeature {
                        override fun isApplicable(type: eu.kanade.tachiyomi.source.entry.EntryType) = true

                        override suspend fun onEvent(
                            event: EntryDownloadLifecycleEvent,
                        ): EntryDownloadLifecycleResult {
                            received += event
                            return EntryDownloadLifecycleResult.Handled
                        }
                    }
                    val execution = EntryMediaSessionExecutionEvent(event).apply {
                        progressResult = EntryProgressRecordingResult(event.progress, completedNow = true)
                    }

                    entryDownloadMediaSessionBinding { feature }.handler.execute(execution)

                    contractExpectation(
                        received.map { it::class } == listOf(
                            EntryDownloadLifecycleEvent.Progressed::class,
                            EntryDownloadLifecycleEvent.Completed::class,
                        ),
                        "Download Lifecycle must receive progress and newly-completed Media Session consequences",
                    )
                }
            },
        )
    }
}

private data class DownloadLifecycleContract(
    val integration: FeatureIntegrationId,
    val contract: FeatureBehaviorContract,
    val scenarioEvidence: (() -> List<mihon.feature.graph.ContextEvidence<*>>)? = null,
)

private val lifecycleContracts = listOf(
    DownloadLifecycleContract(
        ENTRY_DOWNLOAD_LIFECYCLE_PROVIDER_INTEGRATION,
        EntryDownloadLifecycleProviderBehaviorContract,
    ),
    DownloadLifecycleContract(
        ENTRY_DOWNLOAD_MARKED_CONSUMED_INTEGRATION,
        EntryDownloadMarkedConsumedBehaviorContract,
    ) { listOf(contextEvidence(ENTRY_DOWNLOAD_REMOVE_MARKED_CONSUMED_CONTEXT, true)) },
    DownloadLifecycleContract(
        ENTRY_DOWNLOAD_COMPLETION_INTEGRATION,
        EntryDownloadCompletionBehaviorContract,
    ) { listOf(contextEvidence(ENTRY_DOWNLOAD_COMPLETION_CLEANUP_CONTEXT, true)) },
    DownloadLifecycleContract(
        ENTRY_DOWNLOAD_AHEAD_INTEGRATION,
        EntryDownloadAheadBehaviorContract,
    ) {
        listOf(
            contextEvidence(ENTRY_DOWNLOAD_VIEWER_PROGRESS_CONTEXT, true),
            contextEvidence(ENTRY_DOWNLOAD_AHEAD_CONTEXT, true),
        )
    },
    DownloadLifecycleContract(
        ENTRY_DOWNLOAD_CLEANUP_OWNER_INTEGRATION,
        EntryDownloadCleanupOwnerBehaviorContract,
    ) { listOf(contextEvidence(ENTRY_DOWNLOAD_CLEANUP_CATEGORY_ALLOWED_CONTEXT, true)) },
    DownloadLifecycleContract(
        ENTRY_DOWNLOAD_BOOKMARK_PROTECTION_PROVIDER_INTEGRATION,
        EntryDownloadBookmarkProtectionProviderBehaviorContract,
    ),
    DownloadLifecycleContract(
        ENTRY_DOWNLOAD_BOOKMARK_PROTECTION_CONTEXT_INTEGRATION,
        EntryDownloadBookmarkProtectionContextBehaviorContract,
    ) { listOf(contextEvidence(ENTRY_DOWNLOAD_REMOVE_BOOKMARKED_CONTEXT, false)) },
)

private suspend fun verifyDownloadLifecycle(
    input: FeatureContractExecutionInput,
): FeatureContractVerificationResult = verifyFeatureContract {
    val download = input.provider(EntryDownloadCapability.definition)
    val integration = input.subject.integration
    val bindings = buildList {
        add(EntryDownloadCapability.bind(download))
        if (integration.isBookmarkProtection()) {
            add(EntryBookmarkCapability.bind(input.provider(EntryBookmarkCapability.definition)))
        }
    }
    val evaluation = productionSubjectEvaluation(bindings, EntryDownloadLifecycleFeatureContributor)
    val preferences = DownloadPreferences(InMemoryPreferenceStore())
    val entry = Entry.create().copy(id = 76L, profileId = 7L, type = download.type)
    val current = EntryChapter.create().copy(id = 77L, entryId = entry.id, chapterNumber = 1.0, sourceOrder = 1)
    val next = EntryChapter.create().copy(id = 78L, entryId = entry.id, chapterNumber = 2.0, sourceOrder = 2)
    val later = EntryChapter.create().copy(id = 79L, entryId = entry.id, chapterNumber = 3.0, sourceOrder = 3)
    val bookmarked = current.copy(bookmark = true)
    val readingOrder = listOf(later, next, current)
    val interaction = recordingDownloadInteraction()
    val categories = mockk<GetCategories> { coEvery { await(any()) } returns emptyList() }
    val chapters = mockk<GetEntryWithChapters> {
        coEvery { awaitChapters(any(), any(), any()) } returns readingOrder
    }
    val feature = DefaultEntryDownloadLifecycleFeature(
        evaluation = evaluation,
        downloadPreferences = preferences,
        getCategories = categories,
        getEntryWithChapters = chapters,
        entryRepository = mockk<EntryRepository>(relaxed = true),
        downloads = interaction,
    )

    contractExpectation(feature.isApplicable(download.type), "Download Lifecycle must be applicable")
    when (integration) {
        ENTRY_DOWNLOAD_LIFECYCLE_PROVIDER_INTEGRATION -> contractExpectation(
            feature.onEvent(EntryDownloadLifecycleEvent.MarkedConsumed(entry, listOf(current))) ==
                EntryDownloadLifecycleResult.Handled,
            "Download Lifecycle must accept structured events",
        )
        ENTRY_DOWNLOAD_MARKED_CONSUMED_INTEGRATION,
        ENTRY_DOWNLOAD_CLEANUP_OWNER_INTEGRATION,
        -> {
            preferences.removeAfterMarkedAsRead.set(true)
            var deleted = false
            coEvery { interaction.delete(entry, listOf(current)) } answers { deleted = true }
            feature.onEvent(EntryDownloadLifecycleEvent.MarkedConsumed(entry, listOf(current)))
            contractExpectation(deleted, "Download Lifecycle must dispatch eligible cleanup")
        }
        ENTRY_DOWNLOAD_COMPLETION_INTEGRATION -> {
            preferences.removeAfterReadSlots.set(0)
            var deferred = false
            coEvery { interaction.cleanup(entry, listOf(current)) } answers { deferred = true }
            feature.onEvent(EntryDownloadLifecycleEvent.Completed(entry, current))
            contractExpectation(deferred, "Download Lifecycle must defer completion cleanup")
        }
        ENTRY_DOWNLOAD_AHEAD_INTEGRATION -> {
            preferences.autoDownloadWhileReading.set(1)
            every { interaction.isDownloaded(entry, current, any()) } returns true
            every { interaction.isDownloaded(entry, next, any()) } returns true
            every { interaction.isDownloaded(entry, later, any()) } returns false
            var queued = false
            coEvery { interaction.queue(entry, listOf(later), autoStart = false) } answers { queued = true }
            feature.onEvent(EntryDownloadLifecycleEvent.Progressed(entry, current, fraction = 0.5))
            contractExpectation(queued, "Download Lifecycle must dispatch download-ahead candidates")
        }
        ENTRY_DOWNLOAD_BOOKMARK_PROTECTION_PROVIDER_INTEGRATION,
        ENTRY_DOWNLOAD_BOOKMARK_PROTECTION_CONTEXT_INTEGRATION,
        -> {
            preferences.removeAfterMarkedAsRead.set(true)
            var deleted = false
            coEvery { interaction.delete(any(), any()) } answers { deleted = true }
            feature.onEvent(EntryDownloadLifecycleEvent.MarkedConsumed(entry, listOf(bookmarked)))
            contractExpectation(!deleted, "Download Lifecycle must protect bookmarked downloads by default")
        }
        else -> error("Unexpected Download Lifecycle integration $integration")
    }
}

private fun FeatureIntegrationId.isBookmarkProtection(): Boolean = this in setOf(
    ENTRY_DOWNLOAD_BOOKMARK_PROTECTION_PROVIDER_INTEGRATION,
    ENTRY_DOWNLOAD_BOOKMARK_PROTECTION_CONTEXT_INTEGRATION,
)
