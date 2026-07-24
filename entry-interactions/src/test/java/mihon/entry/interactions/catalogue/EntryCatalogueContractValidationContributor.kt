package mihon.entry.interactions

import androidx.paging.PagingSource
import eu.kanade.tachiyomi.source.entry.EntryFilterList
import eu.kanade.tachiyomi.source.entry.EntryItemOrientation
import eu.kanade.tachiyomi.source.entry.EntryPageResult
import eu.kanade.tachiyomi.source.entry.EntryType
import eu.kanade.tachiyomi.source.entry.SEntry
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.MutableStateFlow
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
import tachiyomi.domain.source.model.EntryCatalogueDescription
import tachiyomi.domain.source.model.EntrySourceDescription

class EntryCatalogueContractValidationContributor : FeatureValidationContributor {
    override val owner = EntryCatalogueFeatureContributor.owner

    override fun contributeTo(sink: FeatureValidationContributionSink) {
        contracts.forEach { contract ->
            val reference = FeatureContractReference(ENTRY_CATALOGUE_FEATURE_ID, contract.contract)
            sink.add(
                FeatureContractVerifier(reference) { input ->
                    verifyFeatureContract {
                        val type = EntryType.entries.single { it.toContentTypeId() == input.subject.contentType }
                        val source = EntryCatalogueHostSource(
                            id = 7L,
                            name = "Source",
                            description = description.copy(supportedEntryTypes = setOf(type)),
                            usesAsyncFilters = false,
                        )
                        val filters = EntryFilterList()
                        val host = mockk<EntryCatalogueProviderHost> {
                            every { isInitialized } returns MutableStateFlow(true)
                            every { describe(7L) } returns source.description
                            every { source(7L) } returns EntryCatalogueHostSourceResolution.Available(source)
                            every { backgroundFilters(7L) } returns filters
                            coEvery { filters(7L) } returns filters
                            coEvery { page(7L, 1, any()) } returns EntryPageResult(
                                listOf(
                                    SEntry.create().apply {
                                        url = "/entry"
                                        title = "Entry"
                                        this.type = type
                                    },
                                ),
                                false,
                            )
                        }
                        val evaluation = productionSubjectEvaluation(type, EntryCatalogueFeatureContributor)
                        val feature = DefaultEntryCatalogueFeature(
                            host,
                            EntryCatalogueGraphStateValidator(evaluation),
                            mockk<NetworkToLocalEntry> {
                                coEvery { this@mockk.invoke(any<List<Entry>>()) } answers { firstArg() }
                            },
                        )
                        when (contract.integration) {
                            SOURCE_DESCRIPTION_INTEGRATION_ID -> contractExpectation(
                                feature.description(7L).language == "en",
                                "Catalogue must project source description",
                            )
                            CATALOGUE_AVAILABILITY_INTEGRATION_ID -> {
                                val result = feature.filters(7L)
                                contractExpectation(
                                    result is EntryCatalogueFiltersResult.Available,
                                    "Catalogue must execute an installed provider through its Feature boundary",
                                )
                            }
                            LATEST_AVAILABILITY_INTEGRATION_ID -> {
                                val result = feature.paging(
                                    EntryCatalogueBrowseRequest(7L, EntryCatalogueListing.Latest),
                                ).load(
                                    PagingSource.LoadParams.Refresh(
                                        key = null,
                                        loadSize = 25,
                                        placeholdersEnabled = false,
                                    ),
                                )
                                contractExpectation(
                                    result is PagingSource.LoadResult.Page,
                                    "Catalogue must execute latest for an applicable installed provider",
                                )
                            }
                        }
                    }
                },
            )
            sink.add(
                FeatureContractScenario(
                    FeatureContractScenarioId("${contract.integration.value}.applicable"),
                    reference,
                    contract.integration,
                ) {
                    listOf(contextEvidence(SOURCE_DESCRIPTION_CONTEXT, SourceDescriptionEvidence(description)))
                },
            )
        }
    }

    private data class Contract(
        val integration: mihon.feature.graph.FeatureIntegrationId,
        val contract: mihon.feature.graph.FeatureBehaviorContract,
    )

    private companion object {
        val description = EntrySourceDescription(
            language = "en",
            supportedEntryTypes = null,
            itemOrientation = EntryItemOrientation.VERTICAL,
            catalogue = EntryCatalogueDescription(supportsLatest = true),
        )
        val contracts = listOf(
            Contract(SOURCE_DESCRIPTION_INTEGRATION_ID, EntrySourceDescriptionBehaviorContract),
            Contract(CATALOGUE_AVAILABILITY_INTEGRATION_ID, EntryCatalogueAvailabilityBehaviorContract),
            Contract(LATEST_AVAILABILITY_INTEGRATION_ID, EntryLatestAvailabilityBehaviorContract),
        )
    }
}
