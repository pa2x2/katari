package eu.kanade.tachiyomi.ui.browse.source.browse.filter.group

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
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
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import eu.kanade.tachiyomi.source.entry.EntryFilter
import eu.kanade.tachiyomi.ui.browse.source.browse.filter.control.FilterSheetInsets
import tachiyomi.i18n.*
import tachiyomi.presentation.core.i18n.stringResource

/**
 * The option search of a large group with its Selected only toggle.
 *
 * The sheet keeps this row pinned while the group's options scroll under it, so it draws an opaque background.
 */
@Composable
internal fun GroupFilterTools(filter: EntryFilter.Group<*>, state: FilterGroupUiState, changedOnly: Boolean) {
    Row(
        Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
            .padding(horizontal = FilterSheetInsets.Horizontal, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        OutlinedTextField(
            value = state.query,
            onValueChange = { state.query = it },
            modifier = Modifier.weight(1f),
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
        FilterChip(
            enabled = !changedOnly,
            selected = state.selectedOnly || changedOnly,
            onClick = { state.selectedOnly = !state.selectedOnly },
            label = { Text(stringResource(MR.strings.filter_selected_only)) },
        )
    }
}

internal fun EntryFilter.Group<*>.isSearchable(): Boolean =
    state.filterIsInstance<EntryFilter<*>>().hasSearchableOptionList()
