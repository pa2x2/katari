package eu.kanade.tachiyomi.ui.browse.source.browse.filter

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
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
import mihon.entry.interactions.EntryCatalogueFilterSuggestionsResult
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

    Column {
        OutlinedTextField(
            modifier = Modifier
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

        when (val state = controller.state) {
            FilterAutocompleteUiState.Idle -> Unit
            FilterAutocompleteUiState.Loading -> {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                ) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp))
                    Text(
                        text = stringResource(MR.strings.loading),
                        modifier = Modifier.padding(start = 12.dp),
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
            }
            is FilterAutocompleteUiState.Suggestions -> {
                state.items.forEachIndexed { index, suggestion ->
                    key(suggestion.id) {
                        ListItem(
                            headlineContent = { Text(suggestion.label) },
                            modifier = Modifier.clickable {
                                val edit = controller.applySuggestion(suggestion) ?: return@clickable
                                fieldValue = TextFieldValue(
                                    text = edit.text,
                                    selection = TextRange(edit.selectionStart, edit.selectionEnd),
                                )
                                onUpdate()
                            },
                        )
                    }
                    if (index != state.items.lastIndex) {
                        HorizontalDivider()
                    }
                }
            }
            is FilterAutocompleteUiState.Error -> {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp),
                ) {
                    Text(
                        text = stringResource(MR.strings.internal_error),
                        modifier = Modifier
                            .weight(1f)
                            .padding(vertical = 12.dp),
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    TextButton(onClick = controller::retry) {
                        Text(stringResource(MR.strings.action_retry))
                    }
                }
            }
        }
    }
}
