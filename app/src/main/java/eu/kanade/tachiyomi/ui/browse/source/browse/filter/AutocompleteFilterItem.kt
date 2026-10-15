package eu.kanade.tachiyomi.ui.browse.source.browse.filter

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import eu.kanade.tachiyomi.source.entry.EntryFilter
import eu.kanade.tachiyomi.source.entry.EntryFilterTextInput
import eu.kanade.tachiyomi.ui.browse.source.browse.filter.control.FilterFieldClearButton
import eu.kanade.tachiyomi.ui.browse.source.browse.filter.control.FilterFieldKeyboardOptions
import eu.kanade.tachiyomi.ui.browse.source.browse.filter.control.filterFieldKeyboardActions
import mihon.entry.interactions.catalogue.EntryCatalogueFilterSuggestionsResult
import mihon.presentation.kit.content.Text
import mihon.presentation.kit.field.AutocompleteTextField
import mihon.presentation.kit.menu.DropdownMenuItem
import mihon.presentation.kit.progress.LoadingIndicator
import mihon.presentation.kit.theme.ContentTone
import mihon.presentation.kit.theme.KatariTheme
import tachiyomi.i18n.*
import tachiyomi.presentation.core.i18n.stringResource

@Composable
internal fun AutocompleteFilterItem(
    filter: EntryFilter.Autocomplete,
    isError: Boolean,
    onUpdate: () -> Unit,
    onRequestSuggestions: suspend (
        EntryFilter.Autocomplete,
        EntryFilterTextInput,
    ) -> EntryCatalogueFilterSuggestionsResult,
) {
    val scope = rememberCoroutineScope()
    val requestSuggestions = rememberUpdatedState(onRequestSuggestions)
    val controller = remember(filter) {
        FilterAutocompleteController(
            filter = filter,
            scope = scope,
            requestSuggestions = { requestedFilter, input ->
                requestSuggestions.value(requestedFilter, input)
            },
        )
    }
    var fieldValue by remember(filter) {
        mutableStateOf(
            TextFieldValue(
                text = filter.state,
                selection = TextRange(filter.state.length),
            ),
        )
    }

    DisposableEffect(controller) {
        onDispose(controller::close)
    }

    val autocompleteState = controller.state
    val expanded = when (autocompleteState) {
        FilterAutocompleteUiState.Idle -> false
        FilterAutocompleteUiState.Loading -> true
        is FilterAutocompleteUiState.Suggestions -> autocompleteState.items.isNotEmpty()
        is FilterAutocompleteUiState.Error -> true
    }

    fun updateField(value: TextFieldValue) {
        fieldValue = value
        val textChanged = controller.updateInput(
            EntryFilterTextInput(
                text = value.text,
                selectionStart = value.selection.start,
                selectionEnd = value.selection.end,
            ),
        )
        if (textChanged) onUpdate()
    }

    AutocompleteTextField(
        value = fieldValue,
        onValueChange = ::updateField,
        expanded = expanded,
        onExpandedChange = { shouldExpand ->
            if (shouldExpand) {
                controller.retry()
            } else {
                controller.dismissSuggestions()
            }
        },
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = KatariTheme.spacing.rowInset, vertical = KatariTheme.spacing.s)
            .onFocusChanged { controller.updateFocus(it.isFocused) },
        label = filter.name,
        isError = isError,
        trailingIcon = {
            FilterFieldClearButton(filter.name, visible = fieldValue.text.isNotEmpty()) {
                updateField(TextFieldValue())
            }
        },
        keyboardOptions = FilterFieldKeyboardOptions,
        keyboardActions = filterFieldKeyboardActions(),
    ) {
        when (val state = autocompleteState) {
            FilterAutocompleteUiState.Idle -> Unit
            FilterAutocompleteUiState.Loading -> {
                DropdownMenuItem(
                    label = stringResource(MR.strings.loading),
                    onClick = {},
                    enabled = false,
                    trailing = { LoadingIndicator(modifier = Modifier.size(KatariTheme.sizes.icon)) },
                )
            }
            is FilterAutocompleteUiState.Suggestions -> {
                state.items.forEach { suggestion ->
                    DropdownMenuItem(
                        label = suggestion.label,
                        onClick = {
                            val edit = controller.applySuggestion(suggestion) ?: return@DropdownMenuItem
                            fieldValue = TextFieldValue(
                                text = edit.text,
                                selection = TextRange(edit.selectionStart, edit.selectionEnd),
                            )
                            onUpdate()
                        },
                    )
                }
            }
            is FilterAutocompleteUiState.Error -> {
                DropdownMenuItem(
                    label = stringResource(MR.strings.filter_suggestions_failed),
                    onClick = controller::retry,
                    trailing = { Text(text = stringResource(MR.strings.action_retry), tone = ContentTone.Primary) },
                )
            }
        }
    }
}
