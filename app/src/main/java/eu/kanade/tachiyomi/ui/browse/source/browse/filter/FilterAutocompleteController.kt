package eu.kanade.tachiyomi.ui.browse.source.browse.filter

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import eu.kanade.tachiyomi.source.entry.EntryFilter
import eu.kanade.tachiyomi.source.entry.EntryFilterSuggestion
import eu.kanade.tachiyomi.source.entry.EntryFilterTextEdit
import eu.kanade.tachiyomi.source.entry.EntryFilterTextInput
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.launch
import mihon.entry.interactions.catalogue.EntryCatalogueFilterSuggestionsResult

internal class FilterAutocompleteController(
    private val filter: EntryFilter.Autocomplete,
    private val scope: CoroutineScope,
    private val requestSuggestions: suspend (
        EntryFilter.Autocomplete,
        EntryFilterTextInput,
    ) -> EntryCatalogueFilterSuggestionsResult,
) {
    private var input = EntryFilterTextInput(
        text = filter.state,
        selectionStart = filter.state.length,
        selectionEnd = filter.state.length,
    )
    private var focused = false
    private var requestGeneration = 0L
    private var requestJob: Job? = null

    var state: FilterAutocompleteUiState by mutableStateOf(FilterAutocompleteUiState.Idle)
        private set

    fun updateInput(value: EntryFilterTextInput): Boolean {
        val textChanged = filter.state != value.text
        input = value
        if (textChanged) {
            filter.state = value.text
        }
        if (focused) {
            scheduleRequest(debounce = true)
        }
        return textChanged
    }

    fun updateFocus(isFocused: Boolean) {
        if (focused == isFocused) return
        focused = isFocused
        if (!isFocused) {
            cancelRequest()
            state = FilterAutocompleteUiState.Idle
        } else if (filter.options.requestOnFocus) {
            scheduleRequest(debounce = true)
        }
    }

    fun retry() {
        if (focused) {
            scheduleRequest(debounce = false)
        }
    }

    fun dismissSuggestions() {
        cancelRequest()
        state = FilterAutocompleteUiState.Idle
    }

    fun applySuggestion(suggestion: EntryFilterSuggestion): EntryFilterTextEdit? {
        val edit = runCatching {
            filter.applySuggestion(input, suggestion)
        }.getOrElse { error ->
            state = FilterAutocompleteUiState.Error(error)
            return null
        }

        cancelRequest()
        input = EntryFilterTextInput(
            text = edit.text,
            selectionStart = edit.selectionStart,
            selectionEnd = edit.selectionEnd,
        )
        filter.state = edit.text
        state = FilterAutocompleteUiState.Idle
        return edit
    }

    fun close() {
        focused = false
        cancelRequest()
    }

    private fun scheduleRequest(debounce: Boolean) {
        requestJob?.cancel()
        requestGeneration += 1
        val generation = requestGeneration
        val requestInput = input
        state = FilterAutocompleteUiState.Idle
        requestJob = scope.launch {
            if (debounce) {
                delay(filter.options.debounceMillis)
            }
            currentCoroutineContext().ensureActive()
            if (generation != requestGeneration || !focused) return@launch
            state = FilterAutocompleteUiState.Loading

            val result = requestSuggestions(filter, requestInput)
            currentCoroutineContext().ensureActive()
            if (generation != requestGeneration || !focused) return@launch
            state = when (result) {
                is EntryCatalogueFilterSuggestionsResult.Available -> {
                    FilterAutocompleteUiState.Suggestions(result.suggestions)
                }
                EntryCatalogueFilterSuggestionsResult.NotApplicable -> FilterAutocompleteUiState.Idle
                is EntryCatalogueFilterSuggestionsResult.Unavailable -> FilterAutocompleteUiState.Error()
                is EntryCatalogueFilterSuggestionsResult.Failed -> {
                    FilterAutocompleteUiState.Error(result.cause)
                }
            }
        }
    }

    private fun cancelRequest() {
        requestGeneration += 1
        requestJob?.cancel()
        requestJob = null
    }
}

internal sealed interface FilterAutocompleteUiState {
    data object Idle : FilterAutocompleteUiState
    data object Loading : FilterAutocompleteUiState
    data class Suggestions(
        val items: List<EntryFilterSuggestion>,
    ) : FilterAutocompleteUiState

    data class Error(
        val cause: Throwable? = null,
    ) : FilterAutocompleteUiState
}
