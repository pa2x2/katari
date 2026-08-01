package eu.kanade.tachiyomi.ui.browse.source.browse.filter

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.HorizontalDivider
import androidx.compose.runtime.Composable
import eu.kanade.tachiyomi.source.entry.EntryFilter
import eu.kanade.tachiyomi.source.entry.EntryFilterTextInput
import mihon.entry.interactions.catalogue.EntryCatalogueFilterSuggestionsResult
import tachiyomi.core.common.preference.TriState
import tachiyomi.presentation.core.components.CheckboxItem
import tachiyomi.presentation.core.components.CollapsibleBox
import tachiyomi.presentation.core.components.HeadingItem
import tachiyomi.presentation.core.components.SelectItem
import tachiyomi.presentation.core.components.SortItem
import tachiyomi.presentation.core.components.TextItem
import tachiyomi.presentation.core.components.TriStateItem

@Composable
internal fun FilterItem(
    filter: EntryFilter<*>,
    onUpdate: () -> Unit,
    onOpenPagedGroup: (EntryFilter.PagedGroup<*>) -> Unit,
    onRequestSuggestions: suspend (
        EntryFilter.Autocomplete,
        EntryFilterTextInput,
    ) -> EntryCatalogueFilterSuggestionsResult,
) {
    when (filter) {
        is EntryFilter.Header -> HeadingItem(filter.name)
        is EntryFilter.Separator -> HorizontalDivider()
        is EntryFilter.CheckBox -> CheckboxItem(label = filter.name, checked = filter.state) {
            filter.state = !filter.state
            onUpdate()
        }
        is EntryFilter.TriState -> TriStateItem(filter.name, filter.state.toTriStateFilter()) {
            filter.state = filter.state.toTriStateFilter().next().toTriStateInt()
            onUpdate()
        }
        is EntryFilter.Autocomplete -> AutocompleteFilterItem(filter, onUpdate, onRequestSuggestions)
        is EntryFilter.Text -> TextItem(filter.name, filter.state) {
            filter.state = it
            onUpdate()
        }
        is EntryFilter.Select<*> -> SelectItem(filter.name, filter.values, filter.state) {
            filter.state = it
            onUpdate()
        }
        is EntryFilter.Sort -> CollapsibleBox(heading = filter.name) {
            Column {
                filter.values.mapIndexed { index, item ->
                    val sortAscending = filter.state?.ascending?.takeIf { index == filter.state?.index }
                    SortItem(
                        label = item,
                        sortDescending = sortAscending?.not(),
                        onClick = {
                            val ascending = if (index == filter.state?.index) {
                                !filter.state!!.ascending
                            } else {
                                filter.state?.ascending ?: true
                            }
                            filter.state = EntryFilter.Sort.Selection(index, ascending)
                            onUpdate()
                        },
                    )
                }
            }
        }
        is EntryFilter.Group<*> -> CollapsibleBox(heading = filter.name) {
            SearchableFilterGroupContent(group = filter) {
                FilterItem(it, onUpdate, onOpenPagedGroup, onRequestSuggestions)
            }
        }
        is EntryFilter.PagedGroup<*> -> PagedGroupSummaryItem(filter) { onOpenPagedGroup(filter) }
    }
}

private fun Int.toTriStateFilter(): TriState = when (this) {
    EntryFilter.TriState.STATE_IGNORE -> TriState.DISABLED
    EntryFilter.TriState.STATE_INCLUDE -> TriState.ENABLED_IS
    EntryFilter.TriState.STATE_EXCLUDE -> TriState.ENABLED_NOT
    else -> throw IllegalStateException("Unknown TriState state: $this")
}

private fun TriState.toTriStateInt(): Int = when (this) {
    TriState.DISABLED -> EntryFilter.TriState.STATE_IGNORE
    TriState.ENABLED_IS -> EntryFilter.TriState.STATE_INCLUDE
    TriState.ENABLED_NOT -> EntryFilter.TriState.STATE_EXCLUDE
}
