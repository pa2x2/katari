package eu.kanade.tachiyomi.ui.browse.source.browse.filter

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Clear
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.paging.LoadState
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingSource
import androidx.paging.compose.collectAsLazyPagingItems
import eu.kanade.tachiyomi.source.entry.EntryFilter
import eu.kanade.tachiyomi.source.entry.EntryFilterPageItem
import eu.kanade.tachiyomi.source.entry.EntryFilterPageLoadReason
import eu.kanade.tachiyomi.source.entry.EntryFilterPageScope
import eu.kanade.tachiyomi.source.entry.EntryFilterTextInput
import eu.kanade.tachiyomi.ui.browse.source.browse.SourceFilterPagedGroupHeader
import kotlinx.coroutines.delay
import mihon.entry.interactions.catalogue.EntryCatalogueFilterSuggestionsResult
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.components.SettingsItemsPaddings
import tachiyomi.presentation.core.components.material.PullRefresh
import tachiyomi.presentation.core.i18n.stringResource

@Composable
internal fun PagedGroupFilterContent(
    filter: EntryFilter.PagedGroup<*>,
    onBack: () -> Unit,
    onFilter: () -> Unit,
    onUpdate: () -> Unit,
    onRequestSuggestions: suspend (
        EntryFilter.Autocomplete,
        EntryFilterTextInput,
    ) -> EntryCatalogueFilterSuggestionsResult,
    pagingSourceFactory: (
        EntryFilterPageScope,
        String?,
        EntryFilterPageLoadReason,
    ) -> PagingSource<String, EntryFilterPageItem>,
) {
    var scope by remember(filter) { mutableStateOf(EntryFilterPageScope.AVAILABLE) }
    var query by remember(filter) { mutableStateOf("") }
    val searchOptions = filter.options.search
    val queryTooShort = query.isNotBlank() &&
        searchOptions != null &&
        query.trim().length < searchOptions.minimumQueryLength
    val effectiveQuery by produceState<String?>(initialValue = null, filter, query) {
        if (query.isBlank() || searchOptions == null) {
            value = null
        } else if (!queryTooShort) {
            delay(searchOptions.debounceMillis)
            value = query.trim()
        }
    }
    var refreshGeneration by remember(filter, scope, effectiveQuery) { mutableIntStateOf(0) }
    val pager = remember(filter, scope, effectiveQuery, refreshGeneration) {
        Pager(
            config = PagingConfig(
                pageSize = filter.options.pageSize,
                initialLoadSize = filter.options.pageSize,
                prefetchDistance = (filter.options.pageSize / 2).coerceAtLeast(1),
                enablePlaceholders = false,
            ),
            pagingSourceFactory = {
                pagingSourceFactory(
                    scope,
                    effectiveQuery,
                    if (refreshGeneration == 0) {
                        EntryFilterPageLoadReason.INITIAL
                    } else {
                        EntryFilterPageLoadReason.USER_REFRESH
                    },
                )
            },
        )
    }
    val items = pager.flow.collectAsLazyPagingItems()
    val refreshing = items.loadState.refresh is LoadState.Loading && items.itemCount > 0
    val encodedState = runCatching(filter::encodeCurrentState).getOrNull()

    Column(modifier = Modifier.fillMaxSize()) {
        SourceFilterPagedGroupHeader(
            title = filter.name,
            onBack = onBack,
            onReset = {
                filter.resetState()
                onUpdate()
                items.refresh()
            },
            onRefresh = { refreshGeneration += 1 },
            onFilter = onFilter,
        )

        if (searchOptions != null) {
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(
                        horizontal = SettingsItemsPaddings.Horizontal,
                        vertical = SettingsItemsPaddings.Vertical,
                    ),
                placeholder = { Text(stringResource(MR.strings.action_search_hint)) },
                leadingIcon = { Icon(Icons.Outlined.Search, contentDescription = null) },
                trailingIcon = {
                    if (query.isNotEmpty()) {
                        IconButton(onClick = { query = "" }) {
                            Icon(Icons.Outlined.Clear, stringResource(MR.strings.action_reset))
                        }
                    }
                },
                singleLine = true,
            )
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = SettingsItemsPaddings.Horizontal),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            FilterChip(
                selected = scope == EntryFilterPageScope.AVAILABLE,
                onClick = { scope = EntryFilterPageScope.AVAILABLE },
                label = { Text(stringResource(MR.strings.browse_filter_available)) },
            )
            FilterChip(
                selected = scope == EntryFilterPageScope.SELECTED,
                onClick = { scope = EntryFilterPageScope.SELECTED },
                label = {
                    Text(
                        stringResource(
                            MR.strings.browse_filter_selected_count,
                            filter.currentSelectedItemCount(),
                        ),
                    )
                },
            )
        }

        if (queryTooShort) {
            Text(
                text = stringResource(
                    MR.strings.browse_filter_type_more_characters,
                    searchOptions.minimumQueryLength,
                ),
                modifier = Modifier.padding(SettingsItemsPaddings.Horizontal),
            )
            return@Column
        }

        PullRefresh(
            refreshing = refreshing,
            enabled = items.loadState.refresh !is LoadState.Loading,
            onRefresh = { refreshGeneration += 1 },
            modifier = Modifier.fillMaxSize(),
        ) {
            LazyColumn(modifier = Modifier.fillMaxSize()) {
                if (items.loadState.refresh is LoadState.Loading && items.itemCount == 0) {
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(SettingsItemsPaddings.Horizontal),
                            horizontalArrangement = Arrangement.Center,
                        ) {
                            CircularProgressIndicator()
                        }
                    }
                } else if (items.loadState.refresh is LoadState.Error && items.itemCount == 0) {
                    item { RetryItem(onRetry = items::retry) }
                } else if (items.itemCount == 0) {
                    item {
                        Text(
                            text = stringResource(
                                if (scope == EntryFilterPageScope.SELECTED) {
                                    MR.strings.browse_filter_no_selected
                                } else {
                                    MR.strings.no_results_found
                                },
                            ),
                            modifier = Modifier.padding(SettingsItemsPaddings.Horizontal),
                        )
                    }
                }

                items(count = items.itemCount, key = { index -> items[index]?.id ?: index }) { index ->
                    val item = items[index] ?: return@items
                    ProjectedFilterItem(
                        group = filter,
                        item = item,
                        encodedState = encodedState,
                        onUpdate = onUpdate,
                        onSelectedItemUpdate = items::refresh,
                        selectedScope = scope == EntryFilterPageScope.SELECTED,
                        onRequestSuggestions = onRequestSuggestions,
                    )
                }

                if (items.loadState.append is LoadState.Loading) {
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(SettingsItemsPaddings.Horizontal),
                            horizontalArrangement = Arrangement.Center,
                        ) {
                            CircularProgressIndicator()
                        }
                    }
                } else if (items.loadState.append is LoadState.Error) {
                    item { RetryItem(onRetry = items::retry) }
                }
            }
        }
    }
}

@Composable
private fun ProjectedFilterItem(
    group: EntryFilter.PagedGroup<*>,
    item: EntryFilterPageItem,
    encodedState: String?,
    onUpdate: () -> Unit,
    onSelectedItemUpdate: () -> Unit,
    selectedScope: Boolean,
    onRequestSuggestions: suspend (
        EntryFilter.Autocomplete,
        EntryFilterTextInput,
    ) -> EntryCatalogueFilterSuggestionsResult,
) {
    var projection by remember(group, item.id) {
        mutableStateOf(projectFilterItemState(group, item, null))
    }
    LaunchedEffect(group, encodedState, item) {
        projection = projectFilterItemState(group, item, projection.filterOrNull())
    }

    when (val current = projection) {
        ProjectedFilterItemState.Failed -> RetryItem {
            projection = projectFilterItemState(group, item, null)
        }
        is ProjectedFilterItemState.Ready -> FilterItem(
            filter = current.filter,
            onUpdate = {
                val update = runCatching { group.applyItemUpdate(item, current.filter) }
                if (update.isFailure) {
                    projection = ProjectedFilterItemState.Failed
                } else {
                    projection = projectFilterItemState(group, item, current.filter)
                    onUpdate()
                    if (selectedScope) onSelectedItemUpdate()
                }
            },
            onOpenPagedGroup = {},
            onRequestSuggestions = onRequestSuggestions,
        )
    }
}

@Composable
private fun RetryItem(onRetry: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(SettingsItemsPaddings.Horizontal),
        horizontalArrangement = Arrangement.Center,
    ) {
        TextButton(onClick = onRetry) {
            Text(stringResource(MR.strings.action_retry))
        }
    }
}

private sealed interface ProjectedFilterItemState {
    data class Ready(val filter: EntryFilter<*>) : ProjectedFilterItemState

    data object Failed : ProjectedFilterItemState
}

private fun ProjectedFilterItemState.filterOrNull(): EntryFilter<*>? =
    (this as? ProjectedFilterItemState.Ready)?.filter

private fun projectFilterItemState(
    group: EntryFilter.PagedGroup<*>,
    item: EntryFilterPageItem,
    previous: EntryFilter<*>?,
): ProjectedFilterItemState {
    return runCatching { projectFilterItem(group, item, previous) }
        .fold(
            onSuccess = ProjectedFilterItemState::Ready,
            onFailure = { ProjectedFilterItemState.Failed },
        )
}

private fun projectFilterItem(
    group: EntryFilter.PagedGroup<*>,
    item: EntryFilterPageItem,
    previous: EntryFilter<*>?,
): EntryFilter<*> {
    val projected = group.projectItem(item, previous)
    require(
        projected !is EntryFilter.Header &&
            projected !is EntryFilter.Separator &&
            projected !is EntryFilter.Group<*> &&
            projected !is EntryFilter.PagedGroup<*>,
    ) {
        "Paged filter items must project to interactive leaf controls"
    }
    return projected
}
