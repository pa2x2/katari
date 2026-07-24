package mihon.entry.interactions

import eu.kanade.tachiyomi.source.entry.EntryImageSource
import eu.kanade.tachiyomi.source.entry.EntryType
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
import mihon.feature.graph.validation.FeatureExecutionContractReference
import mihon.feature.graph.validation.FeatureExecutionContractVerifier
import mihon.feature.graph.validation.FeatureValidationContributionSink
import mihon.feature.graph.validation.FeatureValidationContributor
import okhttp3.Headers
import okhttp3.OkHttpClient

class EntryCoverNetworkContractValidationContributor : FeatureValidationContributor {
    override val owner = EntryCoverNetworkFeatureContributor.owner

    override fun contributeTo(sink: FeatureValidationContributionSink) {
        val reference = FeatureContractReference(ENTRY_COVER_NETWORK_FEATURE_ID, EntryCoverNetworkBehaviorContract)
        sink.add(
            FeatureContractVerifier(reference) { input ->
                verifyFeatureContract {
                    val type = EntryType.entries.single { it.toContentTypeId() == input.subject.contentType }
                    val client = mockk<OkHttpClient>()
                    val headers = Headers.headersOf("Contract", "source")
                    val source = mockk<EntryImageSource> {
                        every { this@mockk.client } returns client
                        every { this@mockk.headers } returns headers
                    }
                    val feature = DefaultEntryCoverNetworkFeature(
                        productionSubjectEvaluation(type, EntryCoverNetworkFeatureContributor),
                        mockk { every { get(7L) } returns source },
                    )
                    contractExpectation(
                        feature.resolve(7L) == EntryCoverNetworkResolution.Available(7L, client, headers),
                        "Cover Network must expose source network context together",
                    )
                }
            },
        )
        sink.add(applicableScenario(reference))
        sink.add(
            FeatureExecutionContractVerifier(
                FeatureExecutionContractReference(
                    ENTRY_COVER_HASH_PROFILE_MOVE_PARTICIPANT.id,
                    EntryCoverHashProfileMoveBehaviorContract,
                ),
            ) {
                verifyFeatureContract {
                    var moved: EntryProfileMoveStateRequest? = null
                    val host = EntryProfileMoveCoverHashStateHost { request -> moved = request }
                    val request = EntryProfileMoveStateRequest(1L, 2L, listOf(51L))
                    host.move(request)
                    contractExpectation(moved == request, "Profile movement must transfer cover-hash state")
                }
            },
        )
    }

    private fun applicableScenario(reference: FeatureContractReference) = FeatureContractScenario(
        FeatureContractScenarioId("entry.cover-network.source.applicable"),
        reference,
        ENTRY_COVER_NETWORK_INTEGRATION_ID,
    ) { listOf(contextEvidence(ENTRY_COVER_NETWORK_CONTEXT, EntryCoverNetworkContext(true, true))) }
}
