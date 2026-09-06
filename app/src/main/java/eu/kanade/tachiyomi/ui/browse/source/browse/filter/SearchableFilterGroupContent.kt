package eu.kanade.tachiyomi.ui.browse.source.browse.filter

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Clear
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import eu.kanade.tachiyomi.source.entry.EntryFilter
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.i18n.stringResource

@Composable
internal fun SearchableFilterGroupContent(
    group: EntryFilter.Group<*>,
    selectedOnly: Boolean = false,
    itemContent: @Composable (EntryFilter<*>) -> Unit,
) {
    val filters = group.state.filterIsInstance<EntryFilter<*>>()
    val isSearchable = filters.hasSearchableOptionList()
    var query by rememberSaveable(group) { mutableStateOf("") }
    val matchingFilters = if (isSearchable) filters.filterGroupOptions(query) else filters
    val visibleFilters = if (selectedOnly) matchingFilters.filter { it.activeCount() != 0 } else matchingFilters

    Column {
        if (isSearchable) {
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                placeholder = { Text(stringResource(MR.strings.action_search_hint)) },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Outlined.Search,
                        contentDescription = null,
                    )
                },
                trailingIcon = {
                    if (query.isNotEmpty()) {
                        IconButton(onClick = { query = "" }) {
                            Icon(
                                imageVector = Icons.Outlined.Clear,
                                contentDescription = stringResource(MR.strings.action_reset),
                            )
                        }
                    }
                },
                singleLine = true,
            )
        }

        if (visibleFilters.isEmpty()) {
            Text(
                stringResource(
                    if (query.isNotBlank()) MR.strings.no_results_found else MR.strings.browse_filter_no_selected,
                ),
                modifier = Modifier.padding(16.dp),
            )
        }
        for (filter in visibleFilters) {
            itemContent(filter)
        }
    }
}
