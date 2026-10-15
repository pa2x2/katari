package eu.kanade.presentation.entry.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import kotlinx.collections.immutable.ImmutableList
import mihon.icons.materialsymbols.MaterialSymbols
import mihon.icons.materialsymbols.rounded.Close
import mihon.presentation.kit.button.IconButton
import mihon.presentation.kit.field.TextField
import mihon.presentation.kit.sheet.Sheet
import mihon.presentation.kit.sheet.SheetHeader
import mihon.presentation.kit.surface.Card
import mihon.presentation.kit.surface.CardStyle
import mihon.presentation.kit.theme.KatariTheme
import tachiyomi.i18n.*
import tachiyomi.presentation.core.i18n.stringResource

/** Picks the entry to merge into: candidates listed bottom-up above a search field, nearest matches closest to it. */
@Composable
fun MergeTargetPickerSheet(
    title: String,
    query: String,
    entries: ImmutableList<MergeEditorEntry>,
    onDismissRequest: () -> Unit,
    onQueryChange: (String) -> Unit,
    onSelectTarget: (Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    val focusRequester = remember { FocusRequester() }
    var textFieldValue by rememberSaveable(stateSaver = TextFieldValue.Saver) {
        mutableStateOf(
            TextFieldValue(
                text = query,
                selection = TextRange(query.length),
            ),
        )
    }

    LaunchedEffect(query) {
        if (textFieldValue.text != query) {
            textFieldValue = TextFieldValue(
                text = query,
                selection = TextRange(query.length),
            )
        }
    }

    LaunchedEffect(focusRequester) {
        focusRequester.requestFocus()
    }

    Sheet(
        onDismissRequest = onDismissRequest,
        modifier = modifier,
        capHeight = true,
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            SheetHeader(title = title)
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f, fill = false),
                reverseLayout = true,
                contentPadding = PaddingValues(
                    horizontal = KatariTheme.spacing.rowInset,
                    vertical = KatariTheme.spacing.m,
                ),
                verticalArrangement = Arrangement.spacedBy(KatariTheme.spacing.s),
            ) {
                items(entries, key = { it.id }) { entry ->
                    Card(style = CardStyle.Elevated, onClick = { onSelectTarget(entry.id) }) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(KatariTheme.spacing.s),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(KatariTheme.spacing.s),
                        ) {
                            MergeEditorEntryCover(entry)
                            MergeEditorEntryDetails(entry = entry, modifier = Modifier.weight(1f))
                        }
                    }
                }
            }
            TextField(
                value = textFieldValue,
                onValueChange = {
                    textFieldValue = it
                    onQueryChange(it.text)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(
                        start = KatariTheme.spacing.rowInset,
                        end = KatariTheme.spacing.rowInset,
                        bottom = KatariTheme.spacing.l,
                    )
                    .focusRequester(focusRequester),
                label = stringResource(MR.strings.action_search),
                trailingIcon = if (textFieldValue.text.isNotEmpty()) {
                    {
                        IconButton(
                            icon = MaterialSymbols.Rounded.Close,
                            contentDescription = stringResource(MR.strings.action_reset),
                            onClick = {
                                textFieldValue = TextFieldValue("")
                                onQueryChange("")
                            },
                        )
                    }
                } else {
                    null
                },
            )
        }
    }
}
