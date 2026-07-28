package mihon.entry.interactions

import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flowOf
import mihon.entry.interactions.validation.contractExpectation
import mihon.entry.interactions.validation.productionSubjectEvaluation
import mihon.entry.interactions.validation.verifyFeatureContract
import mihon.feature.graph.validation.FeatureContractReference
import mihon.feature.graph.validation.FeatureContractVerifier
import mihon.feature.graph.validation.FeatureValidationContributionSink
import mihon.feature.graph.validation.FeatureValidationContributor
import tachiyomi.domain.entry.model.Entry

class EntryDownloadRuntimeContractValidationContributor : FeatureValidationContributor {
    override val owner = EntryDownloadRuntimeFeatureContributor.owner

    override fun contributeTo(sink: FeatureValidationContributionSink) {
        sink.add(
            FeatureContractVerifier(
                FeatureContractReference(ENTRY_DOWNLOAD_RUNTIME_FEATURE_ID, EntryDownloadRuntimeBehaviorContract),
            ) { input ->
                verifyFeatureContract {
                    val provider = input.provider(EntryDownloadCapability.definition)
                    val evaluation = productionSubjectEvaluation(
                        EntryDownloadCapability.bind(provider),
                        EntryDownloadRuntimeFeatureContributor,
                    )
                    var started = false
                    val entry = Entry.create().copy(id = 70L, type = provider.type)
                    val interaction = mockk<EntryDownloadInteraction>(relaxed = true) {
                        every { changes } returns emptyFlow()
                        every { queueState } returns flowOf(emptyList())
                        every { isInitializing } returns flowOf(false)
                        every { isRunning } returns flowOf(false)
                        every { isPaused } returns flowOf(false)
                        every { getDownloadCount(entry) } returns 3
                        every { startDownloads() } answers { started = true }
                    }
                    val feature = DefaultEntryDownloadRuntimeFeature(evaluation, interaction)

                    contractExpectation(feature.isApplicable(provider.type), "Download Runtime must be applicable")
                    contractExpectation(
                        feature.downloadCount(entry) == 3,
                        "Download Runtime must project provider state",
                    )
                    feature.start()
                    contractExpectation(started, "Download Runtime must dispatch start through its shared boundary")
                }
            },
        )
    }
}
