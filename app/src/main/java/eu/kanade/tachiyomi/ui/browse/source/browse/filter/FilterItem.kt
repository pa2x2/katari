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
import eu.kanade.tachiyomi.source.entry.filter.validationIssues
import eu.kanade.tachiyomi.source.filter.hasFailedSourceCallback
import eu.kanade.tachiyomi.ui.browse.source.browse.filter.change.FilterChanges
import eu.kanade.tachiyomi.ui.browse.source.browse.filter.control.FilterCheckboxRow
import eu.kanade.tachiyomi.ui.browse.source.browse.filter.control.FilterOrderingSelectRow
import eu.kanade.tachiyomi.ui.browse.source.browse.filter.control.FilterSelectField
import eu.kanade.tachiyomi.ui.browse.source.browse.filter.control.FilterSheetInsets
import eu.kanade.tachiyomi.ui.browse.source.browse.filter.control.FilterSortRow
import eu.kanade.tachiyomi.ui.browse.source.browse.filter.control.FilterTextField
import eu.kanade.tachiyomi.ui.browse.source.browse.filter.control.FilterTriStateRow
import eu.kanade.tachiyomi.ui.browse.source.browse.filter.date.DateFilterItem
import eu.kanade.tachiyomi.ui.browse.source.browse.filter.group.FilterGroupUiState
import eu.kanade.tachiyomi.ui.browse.source.browse.filter.group.GroupFilterHeader
import eu.kanade.tachiyomi.ui.browse.source.browse.filter.group.GroupFilterItem
import eu.kanade.tachiyomi.ui.browse.source.browse.filter.paged.PagedGroupSummaryItem
import eu.kanade.tachiyomi.ui.browse.source.browse.filter.validation.LocalFilterValidation
import eu.kanade.tachiyomi.ui.browse.source.browse.filter.validation.displayMessage
import mihon.entry.interactions.catalogue.EntryCatalogueFilterSuggestionsResult
import tachiyomi.i18n.*
import tachiyomi.presentation.core.i18n.stringResource

/**
 * Draws one source filter. With [groupState], a group draws only its header because the sheet lays its tools and
 * options out as separate list rows.
 */
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
    groupState: FilterGroupUiState? = null,
) {
    val invalid = LocalFilterValidation.current.isInvalid(filter)
    Column {
        when (filter) {
            is EntryFilter.Header -> FilterSourceHeader(filter.name)
            is EntryFilter.Separator -> HorizontalDivider(Modifier.padding(horizontal = FilterSheetInsets.Horizontal))
            is EntryFilter.CheckBox -> FilterCheckboxRow(label = filter.name, checked = filter.state) {
                filter.state = !filter.state
                onUpdate()
            }
            is EntryFilter.TriState -> FilterTriStateRow(filter, onUpdate)
            is EntryDateFilter -> DateFilterItem(filter, invalid, onUpdate)
            is EntryFilter.Autocomplete -> AutocompleteFilterItem(filter, invalid, onUpdate, onRequestSuggestions)
            is EntryFilter.Text -> FilterTextField(filter.name, filter.state, invalid) {
                filter.state = it
                onUpdate()
            }
            is EntryFilter.Select<*> -> if (filter.isOrdering) {
                FilterOrderingSelectRow(filter, onUpdate)
            } else {
                FilterSelectField(filter.name, filter.values, filter.state, invalid) {
                    filter.state = it
                    onUpdate()
                }
            }
            is EntryFilter.Sort -> FilterSortRow(filter, onUpdate)
            is EntryFilter.Group<*> -> if (groupState != null) {
                GroupFilterHeader(filter, groupState, changes, changedOnly, leadingDivider, onReset, onUpdate)
            } else {
                GroupFilterItem(
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
            }
            is EntryFilter.PagedGroup<*> -> PagedGroupSummaryItem(filter, changes) { onOpenPagedGroup(filter) }
        }
        // A group shows its description in its header.
        filter.metadata?.description?.takeIf { it.isNotBlank() && filter !is EntryFilter.Group<*> }?.let {
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
        // Groups list their issues under their options and date filters under their value.
        if (filter !is EntryFilter.Group<*> && filter !is EntryDateFilter) {
            filter.validationIssues().forEach {
                Text(
                    it.displayMessage(),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(horizontal = FilterSheetInsets.Horizontal),
                )
            }
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
