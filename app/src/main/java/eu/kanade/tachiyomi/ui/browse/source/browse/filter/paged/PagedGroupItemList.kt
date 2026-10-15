package eu.kanade.tachiyomi.ui.browse.source.browse.filter.paged

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.paging.LoadState
import androidx.paging.compose.LazyPagingItems
import eu.kanade.tachiyomi.source.entry.EntryFilter
import eu.kanade.tachiyomi.source.entry.EntryFilterNavigationTarget
import eu.kanade.tachiyomi.source.entry.EntryFilterPageItem
import eu.kanade.tachiyomi.source.entry.EntryFilterPageScope
import eu.kanade.tachiyomi.source.entry.EntryFilterTextInput
import mihon.entry.interactions.catalogue.EntryCatalogueFilterSuggestionsResult
import mihon.presentation.kit.button.Button
import mihon.presentation.kit.button.ButtonStyle
import mihon.presentation.kit.content.Text
import mihon.presentation.kit.feedback.NoResults
import mihon.presentation.kit.progress.LoadingIndicator
import mihon.presentation.kit.progress.PullRefresh
import mihon.presentation.kit.theme.KatariTheme
import tachiyomi.i18n.*
import tachiyomi.presentation.core.i18n.stringResource

@Composable
internal fun PagedGroupItemList(
    filter: EntryFilter.PagedGroup<*>,
    items: LazyPagingItems<EntryFilterPageItem>,
    listState: LazyListState,
    scope: EntryFilterPageScope,
    refreshing: Boolean,
    encodedState: String?,
    navigationTargets: List<EntryFilterNavigationTarget>,
    usesNavigationRail: Boolean,
    currentNavigationTargetId: String?,
    onRefresh: () -> Unit,
    onJump: (EntryFilterNavigationTarget) -> Unit,
    onEditItem: (EntryFilterPageItem, EntryFilter<*>, (Boolean) -> Unit) -> Unit,
    onRequestSuggestions: suspend (
        EntryFilter.Autocomplete,
        EntryFilterTextInput,
    ) -> EntryCatalogueFilterSuggestionsResult,
    modifier: Modifier,
) {
    Column(modifier) {
        if (navigationTargets.isNotEmpty() && !usesNavigationRail) {
            PagedFilterNavigationMenu(
                targets = navigationTargets,
                onJump = onJump,
            )
        }

        Row(modifier = Modifier.fillMaxSize()) {
            PullRefresh(
                refreshing = refreshing,
                enabled = items.loadState.refresh !is LoadState.Loading,
                onRefresh = onRefresh,
                modifier = Modifier.weight(1f),
            ) {
                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize(),
                ) {
                    if (items.loadState.refresh is LoadState.Loading && items.itemCount == 0) {
                        item {
                            PagedListLoadingItem()
                        }
                    } else if (items.loadState.refresh is LoadState.Error && items.itemCount == 0) {
                        item { RetryItem(onRetry = items::retry) }
                    } else if (items.itemCount == 0) {
                        item {
                            NoResults(
                                message = stringResource(
                                    if (scope == EntryFilterPageScope.SELECTED) {
                                        MR.strings.browse_filter_no_selected
                                    } else {
                                        MR.strings.no_results_found
                                    },
                                ),
                            )
                        }
                    }

                    items(count = items.itemCount, key = { index -> items[index]?.id ?: index }) { index ->
                        val item = items[index] ?: return@items
                        ProjectedFilterItem(
                            group = filter,
                            item = item,
                            encodedState = encodedState,
                            onEditItem = onEditItem,
                            onSelectedItemUpdate = items::refresh,
                            selectedScope = scope == EntryFilterPageScope.SELECTED,
                            onRequestSuggestions = onRequestSuggestions,
                        )
                    }

                    if (items.loadState.append is LoadState.Loading) {
                        item {
                            PagedListLoadingItem()
                        }
                    } else if (items.loadState.append is LoadState.Error) {
                        item { RetryItem(onRetry = items::retry) }
                    }
                }
            }
            if (usesNavigationRail) {
                PagedFilterNavigationRail(
                    targets = navigationTargets,
                    currentTargetId = currentNavigationTargetId,
                    onJump = onJump,
                )
            }
        }
    }
}

@Composable
private fun PagedListLoadingItem() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(KatariTheme.spacing.rowInset),
        horizontalArrangement = Arrangement.Center,
    ) {
        LoadingIndicator()
    }
}

@Composable
internal fun RetryItem(onRetry: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(KatariTheme.spacing.rowInset),
        horizontalArrangement = Arrangement.Center,
    ) {
        Button(label = stringResource(MR.strings.action_retry), onClick = onRetry, style = ButtonStyle.Text)
    }
}
