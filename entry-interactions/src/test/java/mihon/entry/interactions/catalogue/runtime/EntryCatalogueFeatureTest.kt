package mihon.entry.interactions.catalogue.runtime

import eu.kanade.tachiyomi.source.entry.EntryFilter
import eu.kanade.tachiyomi.source.entry.EntryFilterList
import eu.kanade.tachiyomi.source.entry.EntryFilterSuggestion
import eu.kanade.tachiyomi.source.entry.EntryFilterTextEdit
import eu.kanade.tachiyomi.source.entry.EntryFilterTextInput
import eu.kanade.tachiyomi.source.entry.EntryItemOrientation
import eu.kanade.tachiyomi.source.entry.EntryType
import io.kotest.matchers.shouldBe
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import mihon.entry.interactions.catalogue.EntryCatalogueFeature
import mihon.entry.interactions.catalogue.EntryCatalogueFeatureContributor
import mihon.entry.interactions.catalogue.EntryCatalogueFilterSuggestionsResult
import mihon.entry.interactions.catalogue.EntryCatalogueSearchRequest
import mihon.entry.interactions.catalogue.host.EntryCatalogueHostSource
import mihon.entry.interactions.catalogue.host.EntryCatalogueHostSourceResolution
import mihon.entry.interactions.catalogue.host.EntryCatalogueProviderHost
import mihon.entry.interactions.validation.productionSubjectEvaluation
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import tachiyomi.domain.source.model.EntryCatalogueDescription
import tachiyomi.domain.source.model.EntrySourceDescription

class EntryCatalogueFeatureTest {
    private val description = EntrySourceDescription(
        language = "en",
        supportedEntryTypes = setOf(EntryType.MANGA, EntryType.ANIME),
        itemOrientation = EntryItemOrientation.HORIZONTAL,
        catalogue = EntryCatalogueDescription(supportsLatest = true),
    )
    private val source = EntryCatalogueHostSource(7L, "Source", description)

    @Test
    fun `provider failures are normalized while cancellation remains cancellation`() = runTest {
        val input = EntryFilterTextInput("query", 5, 5)
        val filter = autocomplete()
        val failure = IllegalStateException("suggestions failed")
        val host = host()
        every { host.source(7L) } returns EntryCatalogueHostSourceResolution.Available(source)
        coEvery { host.filterSuggestions(7L, filter, input, "query") } throws failure andThenThrows
            CancellationException()
        coEvery { host.backgroundFilters(7L) } returns EntryFilterList()
        coEvery { host.page(any(), any(), any()) } throws CancellationException()
        val feature = feature(host)

        feature.filterSuggestions(7L, filter, input) shouldBe
            EntryCatalogueFilterSuggestionsResult.Failed(failure)
        assertThrows<CancellationException> {
            feature.filterSuggestions(7L, filter, input)
        }
        assertThrows<CancellationException> {
            feature.search(EntryCatalogueSearchRequest(7L, "query"))
        }
    }

    private fun feature(host: EntryCatalogueProviderHost): EntryCatalogueFeature {
        val evaluation = productionSubjectEvaluation(
            EntryType.BOOK,
            EntryCatalogueFeatureContributor,
        )
        return DefaultEntryCatalogueFeature(
            host = host,
            graphStateValidator = EntryCatalogueGraphStateValidator(evaluation),
            networkToLocalEntry = mockk(),
        )
    }

    private fun host() = mockk<EntryCatalogueProviderHost> {
        every { isInitialized } returns MutableStateFlow(true)
    }

    private fun autocomplete() = object : EntryFilter.Autocomplete("Autocomplete") {
        override fun getSuggestionQuery(input: EntryFilterTextInput): String = input.text

        override suspend fun getSuggestions(
            input: EntryFilterTextInput,
            query: String,
        ): List<EntryFilterSuggestion> = error("Host executes suggestions")

        override fun applySuggestion(
            input: EntryFilterTextInput,
            suggestion: EntryFilterSuggestion,
        ): EntryFilterTextEdit = EntryFilterTextEdit(suggestion.value, suggestion.value.length)
    }
}
