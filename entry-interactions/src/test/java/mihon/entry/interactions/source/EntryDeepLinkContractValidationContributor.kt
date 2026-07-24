package mihon.entry.interactions

import eu.kanade.tachiyomi.source.entry.EntryType
import eu.kanade.tachiyomi.source.entry.EntryUriType
import eu.kanade.tachiyomi.source.entry.ResolvableSource
import eu.kanade.tachiyomi.source.entry.SEntry
import io.mockk.coEvery
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
import tachiyomi.domain.entry.interactor.NetworkToLocalEntry
import tachiyomi.domain.entry.model.Entry

class EntryDeepLinkContractValidationContributor : FeatureValidationContributor {
    override val owner = EntryDeepLinkFeatureContributor.owner

    override fun contributeTo(sink: FeatureValidationContributionSink) {
        val reference = FeatureContractReference(ENTRY_DEEP_LINK_FEATURE_ID, EntryDeepLinkBehaviorContract)
        sink.add(
            FeatureContractVerifier(reference) { input ->
                verifyFeatureContract {
                    val type = EntryType.entries.single { it.toContentTypeId() == input.subject.contentType }
                    val uri = "https://example.test/entry"
                    val networkEntry = SEntry.create().apply {
                        url = "/entry"
                        title = "Entry"
                        this.type = type
                    }
                    val persisted = Entry.create().copy(id = 11L, source = 7L, type = type)
                    val resolver = mockk<ResolvableSource> {
                        every { id } returns 7L
                        every { getUriType(uri) } returns EntryUriType.Entry
                        coEvery { getEntry(uri) } returns networkEntry
                    }
                    val feature = DefaultEntryDeepLinkFeature(
                        productionSubjectEvaluation(type, EntryDeepLinkFeatureContributor),
                        mockk { every { getAll() } returns listOf(resolver) },
                        mockk<NetworkToLocalEntry> {
                            coEvery { this@mockk.invoke(any<Entry>()) } returns persisted
                        },
                        mockk(),
                        mockk(),
                    )
                    contractExpectation(
                        feature.resolve(uri) == EntryDeepLinkResolution.Resolved(persisted),
                        "Deep Link must return the persisted resolution",
                    )
                }
            },
        )
        sink.add(
            FeatureContractScenario(
                FeatureContractScenarioId("entry.deep-link.resolution.applicable"),
                reference,
                ENTRY_DEEP_LINK_INTEGRATION_ID,
            ) {
                listOf(
                    contextEvidence(
                        ENTRY_DEEP_LINK_CONTEXT,
                        EntryDeepLinkContext(true, EntryDeepLinkMatchState.MATCHED),
                    ),
                )
            },
        )
    }
}
