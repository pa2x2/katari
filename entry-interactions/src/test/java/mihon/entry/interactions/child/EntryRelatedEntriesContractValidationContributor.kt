package mihon.entry.interactions

import eu.kanade.tachiyomi.source.entry.EntryItemOrientation
import eu.kanade.tachiyomi.source.entry.EntryType
import eu.kanade.tachiyomi.source.entry.RelatedEntriesSource
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
import tachiyomi.domain.source.model.EntrySourceDescription
import tachiyomi.domain.source.service.EntrySourceDescriptionResolutionPort

class EntryRelatedEntriesContractValidationContributor : FeatureValidationContributor {
    override val owner = EntryRelatedEntriesFeatureContributor.owner

    override fun contributeTo(sink: FeatureValidationContributionSink) {
        val reference = FeatureContractReference(ENTRY_RELATED_ENTRIES_FEATURE_ID, EntryRelatedEntriesBehaviorContract)
        sink.add(
            FeatureContractVerifier(reference) { input ->
                verifyFeatureContract {
                    val type = EntryType.entries.single { it.toContentTypeId() == input.subject.contentType }
                    val source = mockk<RelatedEntriesSource> { every { id } returns 9L }
                    val feature = DefaultEntryRelatedEntriesFeature(
                        productionSubjectEvaluation(type, EntryRelatedEntriesFeatureContributor),
                        mockk(),
                        mockk(),
                        mockk(),
                        EntrySourceDescriptionResolutionPort {
                            EntrySourceDescription("", null, EntryItemOrientation.HORIZONTAL, null)
                        },
                    )
                    val entry = tachiyomi.domain.entry.model.Entry.create().copy(source = 9L, type = type)
                    contractExpectation(
                        feature.availability(EntryRelatedEntriesContext(entry, source)) ==
                            EntryRelatedEntriesAvailability.Available(EntryItemOrientation.HORIZONTAL),
                        "Related Entries must expose applicable source availability",
                    )
                }
            },
        )
        sink.add(
            FeatureContractScenario(
                FeatureContractScenarioId("entry.related-entries.source-context.applicable"),
                reference,
                ENTRY_RELATED_ENTRIES_INTEGRATION_ID,
            ) {
                listOf(
                    contextEvidence(ENTRY_RELATED_ENTRIES_SOURCE_INSTALLED_CONTEXT, true),
                    contextEvidence(ENTRY_RELATED_ENTRIES_SOURCE_SUPPORT_CONTEXT, true),
                )
            },
        )
    }
}
