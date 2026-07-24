package mihon.entry.interactions

import eu.kanade.tachiyomi.source.entry.EntryType
import io.mockk.coEvery
import io.mockk.mockk
import mihon.entry.interactions.validation.contractExpectation
import mihon.entry.interactions.validation.verifyFeatureContract
import mihon.feature.graph.validation.FeatureContractReference
import mihon.feature.graph.validation.FeatureContractVerifier
import mihon.feature.graph.validation.FeatureValidationContributionSink
import mihon.feature.graph.validation.FeatureValidationContributor
import tachiyomi.domain.entry.model.Entry
import tachiyomi.domain.entry.model.EntryChapter

class EntryLibraryUpdateRefreshContractValidationContributor : FeatureValidationContributor {
    override val owner = EntryLibraryUpdateRefreshFeatureContributor.owner

    override fun contributeTo(sink: FeatureValidationContributionSink) {
        sink.add(
            FeatureContractVerifier(
                FeatureContractReference(
                    ENTRY_LIBRARY_UPDATE_REFRESH_FEATURE_ID,
                    EntryLibraryUpdateRefreshBehaviorContract,
                ),
            ) { input ->
                verifyFeatureContract {
                    val type = EntryType.entries.single { it.toContentTypeId() == input.subject.contentType }
                    val entry = Entry.create().copy(id = 85L, type = type)
                    val earlier = EntryChapter.create().copy(id = 86L, entryId = entry.id, sourceOrder = 1L)
                    val later = EntryChapter.create().copy(id = 87L, entryId = entry.id, sourceOrder = 2L)
                    val sourceRefresh = mockk<EntrySourceRefreshFeature> {
                        coEvery { refresh(any()) } returns EntrySourceRefreshResult.Refreshed(
                            listOf(earlier, later),
                            2,
                            0,
                            0,
                            false,
                        )
                    }
                    val composition = refreshFeatureTestComposition(type)
                    val feature = DefaultEntryLibraryUpdateRefreshFeature(
                        evaluation = composition.featureGraphEvaluation,
                        sourceRefresh = sourceRefresh,
                        executions = composition.featureExecutions,
                    )

                    contractExpectation(
                        feature.newSession().refresh(EntryLibraryUpdateRefreshRequest(entry, true, 0L, 0L)) ==
                            EntryLibraryUpdateRefreshResult.Updated(listOf(later, earlier)),
                        "Library Update Refresh must hand ordered inserted children to its caller",
                    )
                }
            },
        )
    }
}
