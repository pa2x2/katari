package eu.kanade.tachiyomi.ui.browse.source.browse.filter

import eu.kanade.tachiyomi.source.entry.EntryFilter
import eu.kanade.tachiyomi.source.entry.EntryFilterAutocompleteOptions
import eu.kanade.tachiyomi.source.entry.EntryFilterSuggestion
import eu.kanade.tachiyomi.source.entry.EntryFilterTextEdit
import eu.kanade.tachiyomi.source.entry.EntryFilterTextInput
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import mihon.entry.interactions.catalogue.EntryCatalogueFilterSuggestionsResult
import org.junit.jupiter.api.Test

class FilterAutocompleteControllerTest {

    @Test
    fun `new input cancels an in-flight request and only publishes the latest results`() = runTest {
        val filter = filter(debounceMillis = 0)
        var firstRequestCancelled = false
        val latestSuggestion = EntryFilterSuggestion("latest", "Latest")
        val controller = FilterAutocompleteController(filter, this) { _, input ->
            if (input.text == "a") {
                try {
                    awaitCancellation()
                } finally {
                    firstRequestCancelled = true
                }
            }
            EntryCatalogueFilterSuggestionsResult.Available(listOf(latestSuggestion))
        }

        controller.updateFocus(true)
        controller.updateInput(EntryFilterTextInput("a", 1, 1))
        runCurrent()
        controller.state shouldBe FilterAutocompleteUiState.Loading

        controller.updateInput(EntryFilterTextInput("ab", 2, 2))
        runCurrent()

        firstRequestCancelled shouldBe true
        controller.state shouldBe FilterAutocompleteUiState.Suggestions(listOf(latestSuggestion))
    }

    private fun filter(debounceMillis: Long) = object : EntryFilter.Autocomplete(
        name = "Tags",
        state = "",
        options = EntryFilterAutocompleteOptions(debounceMillis = debounceMillis),
    ) {
        override fun getSuggestionQuery(input: EntryFilterTextInput): String = input.text

        override suspend fun getSuggestions(
            input: EntryFilterTextInput,
            query: String,
        ): List<EntryFilterSuggestion> = error("Controller delegates requests")

        override fun applySuggestion(
            input: EntryFilterTextInput,
            suggestion: EntryFilterSuggestion,
        ): EntryFilterTextEdit = error("Not used")
    }
}
