package mihon.entry.interactions

import eu.kanade.tachiyomi.source.entry.EntryType
import eu.kanade.tachiyomi.source.entry.UnifiedSource
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import mihon.entry.interactions.validation.contractExpectation
import mihon.entry.interactions.validation.verifyFeatureContract
import mihon.feature.graph.FeatureContractScenarioId
import mihon.feature.graph.contextEvidence
import mihon.feature.graph.validation.FeatureContractReference
import mihon.feature.graph.validation.FeatureContractScenario
import mihon.feature.graph.validation.FeatureContractVerifier
import mihon.feature.graph.validation.FeatureValidationContributionSink
import mihon.feature.graph.validation.FeatureValidationContributor
import tachiyomi.domain.entry.interactor.SyncEntryWithSource
import tachiyomi.domain.entry.model.Entry
import tachiyomi.domain.entry.model.EntryChapter

class EntrySourceRefreshContractValidationContributor : FeatureValidationContributor {
    override val owner = EntrySourceRefreshFeatureContributor.owner

    override fun contributeTo(sink: FeatureValidationContributionSink) {
        val reference = FeatureContractReference(ENTRY_SOURCE_REFRESH_FEATURE_ID, EntrySourceRefreshBehaviorContract)
        sink.add(
            FeatureContractVerifier(reference) { input ->
                verifyFeatureContract {
                    val type = EntryType.entries.single { it.toContentTypeId() == input.subject.contentType }
                    val entry = Entry.create().copy(id = 11L, source = 7L, profileId = 3L, type = type)
                    val child = EntryChapter.create().copy(id = 12L, entryId = entry.id)
                    val sync = mockk<SyncEntryWithSource> {
                        coEvery { syncStrictly(any(), any(), any(), any(), any(), any(), any()) } returns
                            SyncEntryWithSource.SyncResult(listOf(child), 1, 0, 0, false)
                    }
                    val composition = refreshFeatureTestComposition(type)
                    val feature = DefaultEntrySourceRefreshFeature(
                        evaluation = composition.featureGraphEvaluation,
                        executions = composition.featureExecutions,
                        sourceManager = mockk { every { get(entry.source) } returns mockk<UnifiedSource>() },
                        syncEntryWithSource = sync,
                        updateLibraryTitles = { false },
                    )
                    contractExpectation(
                        feature.refresh(EntrySourceRefreshRequest(entry, manual = false)) ==
                            EntrySourceRefreshResult.Refreshed(listOf(child), 1, 0, 0, false),
                        "Source Refresh must expose the shared synchronization result",
                    )
                }
            },
        )
        sink.add(
            FeatureContractScenario(
                FeatureContractScenarioId("entry.source-refresh.execution.applicable"),
                reference,
                ENTRY_SOURCE_REFRESH_INTEGRATION_ID,
            ) { listOf(contextEvidence(ENTRY_SOURCE_REFRESH_SOURCE_CONTEXT, true)) },
        )
    }
}
