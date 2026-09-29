package eu.kanade.tachiyomi.ui.browse.source.browse

import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import eu.kanade.domain.source.model.FilterRestoreIssue
import eu.kanade.tachiyomi.source.entry.EntryFilter
import eu.kanade.tachiyomi.source.entry.EntryFilterList
import eu.kanade.tachiyomi.source.entry.EntryFilterTextInput
import eu.kanade.tachiyomi.ui.browse.source.browse.filter.FilterItem
import eu.kanade.tachiyomi.ui.browse.source.browse.filter.change.FilterChanges
import eu.kanade.tachiyomi.ui.browse.source.browse.filter.group.FilterGroupUiStates
import eu.kanade.tachiyomi.ui.browse.source.browse.filter.group.GroupFilterBody
import eu.kanade.tachiyomi.ui.browse.source.browse.filter.group.GroupFilterTools
import mihon.entry.interactions.catalogue.EntryCatalogueFilterSuggestionsResult

/**
 * The scrolling list of the root filter page.
 *
 * Open top-level groups are split into header, search, and options rows so a searchable group's search stays pinned
 * while its options scroll. [focus] is the path of the filter the sheet was opened at.
 */
@Composable
internal fun SourceFilterRootList(
    rows: List<SourceFilterRow>,
    listState: LazyListState,
    filters: EntryFilterList,
    changes: FilterChanges,
    groupStates: FilterGroupUiStates,
    focus: List<Int>?,
    onUpdate: () -> Unit,
    onOpenPagedGroup: (EntryFilter.PagedGroup<*>) -> Unit,
    onRequestSuggestions: suspend (
        EntryFilter.Autocomplete,
        EntryFilterTextInput,
    ) -> EntryCatalogueFilterSuggestionsResult,
    onResetGroup: (EntryFilter<*>) -> Unit,
    onRetry: (() -> Unit)?,
    onSaveRepair: () -> Unit,
    onResolveIssue: (FilterRestoreIssue, Boolean) -> Unit,
    onShowAll: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val filterItem: @Composable (EntryFilter<*>, Boolean, List<Int>?) -> Unit = { filter, changedOnly, itemFocus ->
        FilterItem(
            filter = filter,
            changes = changes,
            onUpdate = onUpdate,
            onOpenPagedGroup = onOpenPagedGroup,
            onRequestSuggestions = onRequestSuggestions,
            changedOnly = changedOnly,
            onReset = onResetGroup,
            focus = itemFocus,
        )
    }
    val focusBelow = { index: Int -> focus?.takeIf { it.firstOrNull() == index }?.drop(1) }
    val rowContent: @Composable (SourceFilterRow) -> Unit = { row ->
        SourceFilterRowContent(
            row = row,
            filters = filters,
            onRetry = onRetry,
            onSaveRepair = onSaveRepair,
            onResolveIssue = onResolveIssue,
            onShowAll = onShowAll,
            filterItem = { filterRow ->
                FilterItem(
                    filter = filterRow.filter,
                    changes = changes,
                    onUpdate = onUpdate,
                    onOpenPagedGroup = onOpenPagedGroup,
                    onRequestSuggestions = onRequestSuggestions,
                    changedOnly = filterRow.changedOnlyChildren,
                    onReset = onResetGroup,
                    focus = focusBelow(filterRow.index),
                    leadingDivider = filterRow.leadingDivider,
                    groupState = (filterRow.filter as? EntryFilter.Group<*>)?.let { groupStates.of(filterRow.index) },
                )
            },
            groupTools = { tools ->
                GroupFilterTools(tools.group, groupStates.of(tools.index), tools.changedOnly)
            },
            groupBody = { body ->
                GroupFilterBody(
                    filter = body.group,
                    state = groupStates.of(body.index),
                    changes = changes,
                    changedOnly = body.changedOnly,
                    focus = focusBelow(body.index),
                    onUpdate = onUpdate,
                ) { child, childFocus -> filterItem(child, body.changedOnly, childFocus) }
            },
            repairFilterItem = { filterItem(it, false, null) },
        )
    }
    LazyColumn(state = listState, modifier = modifier) {
        rows.forEach { row ->
            when {
                row is SourceFilterRow.GroupTools || row is SourceFilterRow.GroupEnd -> stickyHeader { rowContent(row) }
                else -> item { rowContent(row) }
            }
        }
    }
}
