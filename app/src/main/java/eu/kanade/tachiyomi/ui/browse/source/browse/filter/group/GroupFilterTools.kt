package eu.kanade.tachiyomi.ui.browse.source.browse.filter.group

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import eu.kanade.tachiyomi.source.entry.EntryFilter
import mihon.presentation.kit.chip.FilterChip
import mihon.presentation.kit.field.SearchField
import mihon.presentation.kit.list.currentRowInset
import mihon.presentation.kit.theme.KatariTheme
import tachiyomi.i18n.*
import tachiyomi.presentation.core.i18n.stringResource

/**
 * The option search of a large group with its Selected only toggle.
 *
 * The root page keeps this row pinned while the group's options scroll under it; the card slice it sits in is opaque,
 * so the options don't show through.
 */
@Composable
internal fun GroupFilterTools(filter: EntryFilter.Group<*>, state: FilterGroupUiState, changedOnly: Boolean) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = currentRowInset, vertical = KatariTheme.spacing.xs),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(KatariTheme.spacing.s),
    ) {
        SearchField(
            query = state.query,
            onQueryChange = { state.query = it },
            placeholder = stringResource(MR.strings.filter_search_group, filter.name),
            modifier = Modifier.weight(1f),
        )
        FilterChip(
            label = stringResource(MR.strings.filter_selected_only),
            selected = state.selectedOnly || changedOnly,
            onClick = { state.selectedOnly = !state.selectedOnly },
            enabled = !changedOnly,
        )
    }
}

internal fun EntryFilter.Group<*>.isSearchable(): Boolean =
    state.filterIsInstance<EntryFilter<*>>().hasSearchableOptionList()
