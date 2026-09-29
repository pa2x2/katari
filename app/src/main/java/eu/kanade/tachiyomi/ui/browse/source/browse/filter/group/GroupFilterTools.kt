package eu.kanade.tachiyomi.ui.browse.source.browse.filter.group

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Clear
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import eu.kanade.tachiyomi.source.entry.EntryFilter
import eu.kanade.tachiyomi.ui.browse.source.browse.filter.canClear
import eu.kanade.tachiyomi.ui.browse.source.browse.filter.clearSelection
import eu.kanade.tachiyomi.ui.browse.source.browse.filter.control.FilterSheetInsets
import tachiyomi.i18n.*
import tachiyomi.presentation.core.i18n.stringResource

/**
 * The option search and group actions shown above an open group's options.
 *
 * When [pinned], the sheet keeps this row at the top while the group's options scroll under it, so it draws an opaque
 * background.
 */
@Composable
internal fun GroupFilterTools(
    filter: EntryFilter.Group<*>,
    state: FilterGroupUiState,
    changedOnly: Boolean,
    pinned: Boolean,
    onReset: (EntryFilter<*>) -> Unit,
    onUpdate: () -> Unit,
) {
    val horizontal = FilterSheetInsets.Horizontal
    Column(
        if (pinned) Modifier.background(MaterialTheme.colorScheme.surfaceContainerHigh) else Modifier,
    ) {
        if (filter.isSearchable()) {
            OutlinedTextField(
                value = state.query,
                onValueChange = { state.query = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = horizontal, vertical = 4.dp),
                placeholder = { Text(stringResource(MR.strings.filter_search_group, filter.name)) },
                leadingIcon = { Icon(Icons.Outlined.Search, contentDescription = null) },
                trailingIcon = {
                    if (state.query.isNotEmpty()) {
                        IconButton(onClick = { state.query = "" }) {
                            Icon(Icons.Outlined.Clear, contentDescription = stringResource(MR.strings.action_reset))
                        }
                    }
                },
                singleLine = true,
            )
        }
        FlowRow(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = horizontal),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            itemVerticalAlignment = Alignment.CenterVertically,
        ) {
            FilterChip(
                enabled = !changedOnly,
                selected = state.selectedOnly || changedOnly,
                onClick = { state.selectedOnly = !state.selectedOnly },
                label = { Text(stringResource(MR.strings.filter_selected_only)) },
            )
            TextButton(onClick = {
                onReset(filter)
                onUpdate()
            }) { Text(stringResource(MR.strings.filter_reset_defaults)) }
            if (filter.canClear()) {
                TextButton(onClick = {
                    filter.clearSelection()
                    onUpdate()
                }) { Text(stringResource(MR.strings.filter_clear)) }
            }
        }
    }
}

internal fun EntryFilter.Group<*>.isSearchable(): Boolean =
    state.filterIsInstance<EntryFilter<*>>().hasSearchableOptionList()
