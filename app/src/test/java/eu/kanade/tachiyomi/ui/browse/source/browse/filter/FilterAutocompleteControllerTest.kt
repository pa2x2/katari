package eu.kanade.tachiyomi.ui.browse.source.browse.filter

import eu.kanade.tachiyomi.source.entry.EntryFilter
import eu.kanade.tachiyomi.source.entry.EntryFilterAutocompleteOptions
import eu.kanade.tachiyomi.source.entry.EntryFilterSuggestion
import eu.kanade.tachiyomi.source.entry.EntryFilterTextEdit
import eu.kanade.tachiyomi.source.entry.EntryFilterTextInput
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import mihon.entry.interactions.EntryCatalogueFilterSuggestionsResult
import org.junit.jupiter.api.Test

class FilterAutocompleteControllerTest {

    @Test
    fun `focus requests are controlled independently from existing filter text`() = runTest {
        val inputs = mutableListOf<EntryFilterTextInput>()
        val disabledFilter = filter(
            debounceMillis = 0,
            state = "existing",
            requestOnFocus = false,
        )
        val disabledController = FilterAutocompleteController(disabledFilter, this) { _, input ->
            inputs += input
            EntryCatalogueFilterSuggestionsResult.Available(emptyList())
        }

        disabledController.updateFocus(true)
        runCurrent()

        inputs shouldBe emptyList()

        val enabledFilter = filter(
            debounceMillis = 0,
            state = "existing",
            requestOnFocus = true,
        )
        val enabledController = FilterAutocompleteController(enabledFilter, this) { _, input ->
            inputs += input
            EntryCatalogueFilterSuggestionsResult.Available(emptyList())
        }

        enabledController.updateFocus(true)
        runCurrent()

        inputs shouldBe listOf(EntryFilterTextInput("existing", 8, 8))
    }

    @Test
    fun `typing requests suggestions only after the configured debounce`() = runTest {
        val filter = filter(debounceMillis = 500)
        val inputs = mutableListOf<EntryFilterTextInput>()
        val suggestion = EntryFilterSuggestion("alpha", "Alpha")
        val controller = FilterAutocompleteController(filter, this) { _, input ->
            inputs += input
            EntryCatalogueFilterSuggestionsResult.Available(listOf(suggestion))
        }

        controller.updateFocus(true)
        controller.updateInput(EntryFilterTextInput("al", 2, 2))
        advanceTimeBy(499)
        runCurrent()

        inputs shouldBe emptyList()
        controller.state shouldBe FilterAutocompleteUiState.Idle

        advanceTimeBy(1)
        runCurrent()

        inputs shouldBe listOf(EntryFilterTextInput("al", 2, 2))
        controller.state shouldBe FilterAutocompleteUiState.Suggestions(listOf(suggestion))
    }

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

    @Test
    fun `selection applies the extension-owned complete text edit without another lookup`() = runTest {
        val filter = filter(debounceMillis = 500)
        var requests = 0
        val controller = FilterAutocompleteController(filter, this) { _, _ ->
            requests += 1
            EntryCatalogueFilterSuggestionsResult.Available(emptyList())
        }
        val suggestion = EntryFilterSuggestion("tag1", "tag2", "tag1")

        controller.updateFocus(true)
        controller.updateInput(EntryFilterTextInput("tag3, -tag4", 11, 11))
        val edit = controller.applySuggestion(suggestion)
        advanceTimeBy(500)
        runCurrent()

        edit shouldBe EntryFilterTextEdit("tag3, -tag1, ", 13)
        filter.state shouldBe "tag3, -tag1, "
        requests shouldBe 0
        controller.state shouldBe FilterAutocompleteUiState.Idle
    }

    private fun filter(
        debounceMillis: Long,
        state: String = "",
        requestOnFocus: Boolean = false,
    ) = object : EntryFilter.Autocomplete(
        name = "Tags",
        state = state,
        options = EntryFilterAutocompleteOptions(
            debounceMillis = debounceMillis,
            requestOnFocus = requestOnFocus,
        ),
    ) {
        override fun getSuggestionQuery(input: EntryFilterTextInput): String = input.text

        override suspend fun getSuggestions(
            input: EntryFilterTextInput,
            query: String,
        ): List<EntryFilterSuggestion> = error("Controller delegates requests")

        override fun applySuggestion(
            input: EntryFilterTextInput,
            suggestion: EntryFilterSuggestion,
        ): EntryFilterTextEdit {
            val tokenStart = input.text.lastIndexOf("tag4")
            val replacement = input.text.replaceRange(tokenStart, input.selectionEnd, suggestion.value) + ", "
            return EntryFilterTextEdit(replacement, replacement.length)
        }
    }
}
