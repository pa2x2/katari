package eu.kanade.tachiyomi.ui.browse.source.browse.filter

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExposedDropdownMenu
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
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
import androidx.compose.ui.unit.dp
import eu.kanade.tachiyomi.source.entry.EntryFilter
import eu.kanade.tachiyomi.source.entry.EntryFilterTextInput
import mihon.entry.interactions.catalogue.EntryCatalogueFilterSuggestionsResult
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.i18n.stringResource

@Composable
internal fun AutocompleteFilterItem(
    filter: EntryFilter.Autocomplete,
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

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { shouldExpand ->
            if (shouldExpand) {
                controller.retry()
            } else {
                controller.dismissSuggestions()
            }
        },
    ) {
        OutlinedTextField(
            modifier = Modifier
                .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryEditable)
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 4.dp)
                .onFocusChanged { controller.updateFocus(it.isFocused) },
            label = { Text(text = filter.name) },
            value = fieldValue,
            onValueChange = { value ->
                fieldValue = value
                val textChanged = controller.updateInput(
                    EntryFilterTextInput(
                        text = value.text,
                        selectionStart = value.selection.start,
                        selectionEnd = value.selection.end,
                    ),
                )
                if (textChanged) {
                    onUpdate()
                }
            },
            singleLine = true,
        )

        ExposedDropdownMenu(
            modifier = Modifier.exposedDropdownSize(matchAnchorWidth = true),
            expanded = expanded,
            onDismissRequest = controller::dismissSuggestions,
        ) {
            when (val state = autocompleteState) {
                FilterAutocompleteUiState.Idle -> Unit
                FilterAutocompleteUiState.Loading -> {
                    DropdownMenuItem(
                        text = { Text(stringResource(MR.strings.loading)) },
                        onClick = {},
                        enabled = false,
                        leadingIcon = {
                            CircularProgressIndicator(modifier = Modifier.size(24.dp))
                        },
                        contentPadding = ExposedDropdownMenuDefaults.ItemContentPadding,
                    )
                }
                is FilterAutocompleteUiState.Suggestions -> {
                    state.items.forEach { suggestion ->
                        DropdownMenuItem(
                            text = { Text(suggestion.label) },
                            onClick = {
                                val edit = controller.applySuggestion(suggestion)
                                    ?: return@DropdownMenuItem
                                fieldValue = TextFieldValue(
                                    text = edit.text,
                                    selection = TextRange(edit.selectionStart, edit.selectionEnd),
                                )
                                onUpdate()
                            },
                            contentPadding = ExposedDropdownMenuDefaults.ItemContentPadding,
                        )
                    }
                }
                is FilterAutocompleteUiState.Error -> {
                    DropdownMenuItem(
                        text = {
                            Text(
                                text = stringResource(MR.strings.internal_error),
                                color = MaterialTheme.colorScheme.error,
                            )
                        },
                        onClick = controller::retry,
                        trailingIcon = {
                            Text(
                                text = stringResource(MR.strings.action_retry),
                                color = MaterialTheme.colorScheme.primary,
                            )
                        },
                        contentPadding = ExposedDropdownMenuDefaults.ItemContentPadding,
                    )
                }
            }
        }
    }
}
