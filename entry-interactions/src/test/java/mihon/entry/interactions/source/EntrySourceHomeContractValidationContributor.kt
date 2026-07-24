package mihon.entry.interactions

import eu.kanade.tachiyomi.source.entry.EntryType
import eu.kanade.tachiyomi.source.entry.SourceHomePage
import io.mockk.every
import io.mockk.mockk
import mihon.entry.interactions.validation.contractExpectation
import mihon.entry.interactions.validation.productionSubjectEvaluation
import mihon.entry.interactions.validation.verifyFeatureContract
import mihon.feature.graph.FeatureContractScenarioId
import mihon.feature.graph.contextEvidence
import mihon.feature.graph.validation.FeatureContractReference
import mihon.feature.graph.validation.FeatureContractScenario
import mihon.feature.graph.validation.FeatureContractVerifier
import mihon.feature.graph.validation.FeatureValidationContributionSink
import mihon.feature.graph.validation.FeatureValidationContributor

class EntrySourceHomeContractValidationContributor : FeatureValidationContributor {
    override val owner = EntrySourceHomeFeatureContributor.owner

    override fun contributeTo(sink: FeatureValidationContributionSink) {
        val reference = FeatureContractReference(SOURCE_HOME_FEATURE_ID, EntrySourceHomeBehaviorContract)
        sink.add(
            FeatureContractVerifier(reference) { input ->
                verifyFeatureContract {
                    val type = EntryType.entries.single { it.toContentTypeId() == input.subject.contentType }
                    val source = mockk<SourceHomePage> {
                        every { id } returns 7L
                        every { name } returns "Contract Source"
                        every { getHomeUrl() } returns "https://example.test"
                    }
                    val feature = DefaultEntrySourceHomeFeature(
                        productionSubjectEvaluation(type, EntrySourceHomeFeatureContributor),
                        mockk { every { get(7L) } returns source },
                    )
                    contractExpectation(
                        feature.resolve(7L) ==
                            EntrySourceHomeResolution.Available(7L, "Contract Source", "https://example.test"),
                        "Source Home must expose an applicable navigation target",
                    )
                }
            },
        )
        sink.add(
            FeatureContractScenario(
                FeatureContractScenarioId("entry.source-home.navigation.applicable"),
                reference,
                SOURCE_HOME_INTEGRATION_ID,
            ) {
                listOf(
                    contextEvidence(SOURCE_HOME_CONTEXT, SourceHomeContext(true, true, SourceHomeUrlState.AVAILABLE)),
                )
            },
        )
    }
}
