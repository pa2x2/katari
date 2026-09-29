package eu.kanade.tachiyomi.ui.browse.source.browse.filter

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import eu.kanade.tachiyomi.source.entry.EntryFilter
import eu.kanade.tachiyomi.source.entry.EntryFilterTextInput
import eu.kanade.tachiyomi.source.entry.filter.EntryDateFilter
import eu.kanade.tachiyomi.source.filter.hasFailedSourceCallback
import eu.kanade.tachiyomi.ui.browse.source.browse.filter.change.FilterChanges
import eu.kanade.tachiyomi.ui.browse.source.browse.filter.control.FilterCheckboxRow
import eu.kanade.tachiyomi.ui.browse.source.browse.filter.control.FilterSelectField
import eu.kanade.tachiyomi.ui.browse.source.browse.filter.control.FilterSheetInsets
import eu.kanade.tachiyomi.ui.browse.source.browse.filter.control.FilterTextField
import eu.kanade.tachiyomi.ui.browse.source.browse.filter.control.FilterTriStateRow
import eu.kanade.tachiyomi.ui.browse.source.browse.filter.date.DateFilterItem
import mihon.entry.interactions.catalogue.EntryCatalogueFilterSuggestionsResult
import tachiyomi.i18n.*
import tachiyomi.presentation.core.components.CollapsibleBox
import tachiyomi.presentation.core.components.SortItem
import tachiyomi.presentation.core.i18n.stringResource

@Composable
internal fun FilterItem(
    filter: EntryFilter<*>,
    changes: FilterChanges,
    onUpdate: () -> Unit,
    onOpenPagedGroup: (EntryFilter.PagedGroup<*>) -> Unit,
    onRequestSuggestions: suspend (
        EntryFilter.Autocomplete,
        EntryFilterTextInput,
    ) -> EntryCatalogueFilterSuggestionsResult,
    changedOnly: Boolean,
    onReset: (EntryFilter<*>) -> Unit,
    focus: List<Int>? = null,
    leadingDivider: Boolean = true,
) {
    Column {
        when (filter) {
            is EntryFilter.Header -> FilterSourceHeader(filter.name)
            is EntryFilter.Separator -> HorizontalDivider(Modifier.padding(horizontal = FilterSheetInsets.Horizontal))
            is EntryFilter.CheckBox -> FilterCheckboxRow(label = filter.name, checked = filter.state) {
                filter.state = !filter.state
                onUpdate()
            }
            is EntryFilter.TriState -> FilterTriStateRow(filter.name, filter.state) {
                filter.state = filter.state.nextTriState()
                onUpdate()
            }
            is EntryDateFilter -> DateFilterItem(filter, onUpdate)
            is EntryFilter.Autocomplete -> AutocompleteFilterItem(filter, onUpdate, onRequestSuggestions)
            is EntryFilter.Text -> FilterTextField(filter.name, filter.state) {
                filter.state = it
                onUpdate()
            }
            is EntryFilter.Select<*> -> FilterSelectField(filter.name, filter.values, filter.state) {
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
            is EntryFilter.Group<*> -> GroupFilterItem(
                filter = filter,
                changes = changes,
                changedOnly = changedOnly,
                onUpdate = onUpdate,
                onReset = onReset,
                focus = focus,
                leadingDivider = leadingDivider,
            ) { child, childFocus ->
                FilterItem(
                    filter = child,
                    changes = changes,
                    onUpdate = onUpdate,
                    onOpenPagedGroup = onOpenPagedGroup,
                    onRequestSuggestions = onRequestSuggestions,
                    changedOnly = changedOnly,
                    onReset = onReset,
                    focus = childFocus,
                )
            }
            is EntryFilter.PagedGroup<*> -> PagedGroupSummaryItem(filter) { onOpenPagedGroup(filter) }
        }
        filter.metadata?.description?.takeIf { it.isNotBlank() }?.let {
            Text(
                it,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(
                    start = FilterSheetInsets.Horizontal,
                    end = FilterSheetInsets.Horizontal,
                    bottom = 12.dp,
                ),
            )
        }
        if (filter.hasFailedSourceCallback()) {
            Text(
                stringResource(MR.strings.filter_source_callback_failed),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(
                    start = FilterSheetInsets.Horizontal,
                    end = FilterSheetInsets.Horizontal,
                    bottom = 12.dp,
                ),
            )
        }
    }
}

/** A source header: the source's own label for the filters that follow, kept where the source placed it. */
@Composable
private fun FilterSourceHeader(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(
            start = FilterSheetInsets.Horizontal,
            end = FilterSheetInsets.Horizontal,
            top = 12.dp,
            bottom = 4.dp,
        ),
    )
}

private fun Int.nextTriState(): Int = when (this) {
    EntryFilter.TriState.STATE_IGNORE -> EntryFilter.TriState.STATE_INCLUDE
    EntryFilter.TriState.STATE_INCLUDE -> EntryFilter.TriState.STATE_EXCLUDE
    else -> EntryFilter.TriState.STATE_IGNORE
}
