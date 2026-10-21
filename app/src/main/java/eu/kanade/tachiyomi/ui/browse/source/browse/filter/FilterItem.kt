package eu.kanade.tachiyomi.ui.browse.source.browse.filter

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import eu.kanade.tachiyomi.source.entry.EntryFilter
import eu.kanade.tachiyomi.source.entry.EntryFilterTextInput
import eu.kanade.tachiyomi.source.entry.filter.EntryDateFilter
import eu.kanade.tachiyomi.source.entry.filter.validationIssues
import eu.kanade.tachiyomi.source.filter.hasFailedSourceCallback
import eu.kanade.tachiyomi.ui.browse.source.browse.filter.change.FilterChanges
import eu.kanade.tachiyomi.ui.browse.source.browse.filter.control.FilterCheckboxRow
import eu.kanade.tachiyomi.ui.browse.source.browse.filter.control.FilterOrderingSelectRow
import eu.kanade.tachiyomi.ui.browse.source.browse.filter.control.FilterSelectControl
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
import mihon.presentation.kit.content.Text
import mihon.presentation.kit.list.RowGroupSeparator
import mihon.presentation.kit.list.RowGroupSubheader
import mihon.presentation.kit.list.currentRowInset
import mihon.presentation.kit.theme.ContentTone
import mihon.presentation.kit.theme.KatariTheme
import mihon.presentation.kit.theme.TextRole
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
            // The source's own label for the filters that follow, kept where the source placed it.
            is EntryFilter.Header -> RowGroupSubheader(filter.name)
            is EntryFilter.Separator -> RowGroupSeparator()
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
                FilterSelectControl(filter.name, filter.values, filter.state, invalid) {
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
            FilterNote(it, ContentTone.Secondary, Modifier.padding(bottom = KatariTheme.spacing.m))
        }
        // Groups list their issues under their options and date filters under their value.
        if (filter !is EntryFilter.Group<*> && filter !is EntryDateFilter) {
            filter.validationIssues().forEach {
                FilterNote(it.displayMessage(), ContentTone.Error)
            }
        }
        if (filter.hasFailedSourceCallback()) {
            FilterNote(
                stringResource(MR.strings.filter_source_callback_failed),
                ContentTone.Error,
                Modifier.padding(bottom = KatariTheme.spacing.m),
            )
        }
    }
}

/** A line of supporting text under a filter, such as its description or what is wrong with its value. */
@Composable
private fun FilterNote(text: String, tone: ContentTone, modifier: Modifier = Modifier) {
    Text(
        text = text,
        modifier = modifier.padding(horizontal = currentRowInset),
        role = TextRole.Supporting,
        tone = tone,
    )
}
