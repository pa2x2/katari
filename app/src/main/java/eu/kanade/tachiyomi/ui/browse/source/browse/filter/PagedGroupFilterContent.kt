package eu.kanade.tachiyomi.ui.browse.source.browse.filter

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
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
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.paging.LoadState
import androidx.paging.PagingSource
import androidx.paging.compose.collectAsLazyPagingItems
import eu.kanade.tachiyomi.source.entry.EntryFilter
import eu.kanade.tachiyomi.source.entry.EntryFilterPageItem
import eu.kanade.tachiyomi.source.entry.EntryFilterPageLoadReason
import eu.kanade.tachiyomi.source.entry.EntryFilterPageScope
import eu.kanade.tachiyomi.source.entry.EntryFilterTextInput
import eu.kanade.tachiyomi.source.filter.withProjectedState
import eu.kanade.tachiyomi.ui.browse.source.browse.SourceFilterPagedGroupHeader
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import mihon.entry.interactions.catalogue.EntryCatalogueFilterNavigationResult
import mihon.entry.interactions.catalogue.EntryCatalogueFilterSuggestionsResult
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.components.SettingsItemsPaddings
import tachiyomi.presentation.core.components.material.PullRefresh
import tachiyomi.presentation.core.i18n.stringResource

@Composable
internal fun PagedGroupFilterContent(
    filter: EntryFilter.PagedGroup<*>,
    filterRevision: Int,
    onBack: () -> Unit,
    onFilter: () -> Unit,
    onReset: () -> Unit,
    canApply: Boolean,
    onEditItem: (EntryFilterPageItem, EntryFilter<*>, (Boolean) -> Unit) -> Unit,
    onRequestSuggestions: suspend (
        EntryFilter.Autocomplete,
        EntryFilterTextInput,
    ) -> EntryCatalogueFilterSuggestionsResult,
    onRequestNavigation: suspend (
        EntryFilterPageScope,
        String?,
    ) -> EntryCatalogueFilterNavigationResult,
    browseSession: PagedFilterBrowseSession,
    pagingSourceFactory: (
        EntryFilterPageScope,
        String?,
        EntryFilterPageLoadReason,
        String?,
    ) -> PagingSource<String, EntryFilterPageItem>,
) {
    val scope = browseSession.scope
    val query = browseSession.query
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
    val viewKey = PagedFilterViewKey(scope, effectiveQuery)
    val pagerConfiguration = browseSession.pagerConfiguration(viewKey)
    val pagingData = remember(filter, browseSession, viewKey, pagerConfiguration) {
        browseSession.pagingData(
            viewKey = viewKey,
            configuration = pagerConfiguration,
            pageSize = filter.options.pageSize,
        ) { reason, initialAnchor ->
            pagingSourceFactory(scope, effectiveQuery, reason, initialAnchor)
        }
    }
    val items = pagingData.collectAsLazyPagingItems()
    val initialViewport = remember(browseSession, viewKey, pagerConfiguration) {
        browseSession.viewport(viewKey)
    }
    val listState = remember(browseSession, viewKey, pagerConfiguration) {
        LazyListState(
            firstVisibleItemIndex = initialViewport.firstVisibleItemIndex,
            firstVisibleItemScrollOffset = initialViewport.firstVisibleItemScrollOffset,
        )
    }
    val pendingJumpTargetId = browseSession.pendingJumpTargetId(viewKey)
    LaunchedEffect(pendingJumpTargetId, items.itemSnapshotList) {
        val targetId = pendingJumpTargetId ?: return@LaunchedEffect
        val targetIndex = items.itemSnapshotList.items.indexOfFirst { it.navigationTargetId == targetId }
        if (targetIndex >= 0) {
            listState.scrollToItem(items.itemSnapshotList.placeholdersBefore + targetIndex)
            browseSession.consumePendingJumpTarget(viewKey, targetId)
        }
    }
    LaunchedEffect(browseSession, viewKey, listState) {
        snapshotFlow {
            PagedFilterViewport(
                firstVisibleItemIndex = listState.firstVisibleItemIndex,
                firstVisibleItemScrollOffset = listState.firstVisibleItemScrollOffset,
            )
        }.collectLatest { browseSession.updateViewport(viewKey, it) }
    }
    val navigationResult by produceState<EntryCatalogueFilterNavigationResult?>(
        initialValue = null,
        filter,
        scope,
        effectiveQuery,
        pagerConfiguration.refreshGeneration,
        queryTooShort,
    ) {
        value = if (queryTooShort) null else onRequestNavigation(scope, effectiveQuery)
    }
    val navigationTargets = (
        navigationResult as? EntryCatalogueFilterNavigationResult.Available
        )?.navigation?.targets.orEmpty()
    val usesNavigationRail = navigationTargets.usesCompactNavigationRail()
    val currentNavigationTargetId by remember(items, listState) {
        derivedStateOf {
            val firstVisibleItemIndex = listState.firstVisibleItemIndex
            if (firstVisibleItemIndex < items.itemCount) {
                items.peek(firstVisibleItemIndex)?.navigationTargetId
            } else {
                null
            }
        }
    }
    val refreshing = items.loadState.refresh is LoadState.Loading && items.itemCount > 0
    // filterRevision forces recomposition when the mutable draft is republished in place.
    // Without it AnimatedContent keeps the paged sheet composition alive with the same filter
    // instance (equal by ===) and the same canApply, so the new durable state never renders.
    val encodedState = remember(filter, filterRevision) {
        runCatching(filter::encodeCurrentState).getOrNull()
    }

    Column(modifier = Modifier.fillMaxSize()) {
        SourceFilterPagedGroupHeader(
            title = filter.name,
            onBack = onBack,
            onReset = {
                onReset()
                items.refresh()
            },
            onRefresh = { browseSession.refresh(viewKey) },
            onFilter = onFilter,
            filterEnabled = canApply,
        )

        if (searchOptions != null) {
            OutlinedTextField(
                value = query,
                onValueChange = { browseSession.query = it },
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
                        IconButton(onClick = { browseSession.query = "" }) {
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
                onClick = { browseSession.scope = EntryFilterPageScope.AVAILABLE },
                label = { Text(stringResource(MR.strings.browse_filter_available)) },
            )
            FilterChip(
                selected = scope == EntryFilterPageScope.SELECTED,
                onClick = { browseSession.scope = EntryFilterPageScope.SELECTED },
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

        if (navigationTargets.isNotEmpty() && !usesNavigationRail) {
            PagedFilterNavigationMenu(
                targets = navigationTargets,
                onJump = { browseSession.jump(viewKey, it) },
            )
        }

        Row(modifier = Modifier.fillMaxSize()) {
            PullRefresh(
                refreshing = refreshing,
                enabled = items.loadState.refresh !is LoadState.Loading,
                onRefresh = { browseSession.refresh(viewKey) },
                modifier = Modifier.weight(1f),
            ) {
                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize(),
                ) {
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
                            onEditItem = onEditItem,
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
            if (usesNavigationRail) {
                PagedFilterNavigationRail(
                    targets = navigationTargets,
                    currentTargetId = currentNavigationTargetId,
                    onJump = { browseSession.jump(viewKey, it) },
                )
            }
        }
    }
}

@Composable
private fun ProjectedFilterItem(
    group: EntryFilter.PagedGroup<*>,
    item: EntryFilterPageItem,
    encodedState: String?,
    onEditItem: (EntryFilterPageItem, EntryFilter<*>, (Boolean) -> Unit) -> Unit,
    onSelectedItemUpdate: () -> Unit,
    selectedScope: Boolean,
    onRequestSuggestions: suspend (
        EntryFilter.Autocomplete,
        EntryFilterTextInput,
    ) -> EntryCatalogueFilterSuggestionsResult,
) {
    val scope = rememberCoroutineScope()
    var projection by remember(group, item.id) {
        mutableStateOf<ProjectedFilterItemState>(ProjectedFilterItemState.Loading)
    }
    LaunchedEffect(group, encodedState, item) {
        projection = projectFilterItemState(group, item, projection.filterOrNull())
    }

    when (val current = projection) {
        ProjectedFilterItemState.Loading -> CircularProgressIndicator(modifier = Modifier.padding(16.dp))
        ProjectedFilterItemState.Failed -> RetryItem {
            scope.launch { projection = projectFilterItemState(group, item, null) }
        }
        // Sources may update and return the previous leaf to preserve focus. It is the same
        // mutable instance, so FilterItem would otherwise be skipped as equal and keep showing
        // the previous visual. Keying on the durable encoded state forces a fresh FilterItem
        // composition that reads the current leaf state.
        is ProjectedFilterItemState.Ready -> key(encodedState) {
            FilterItem(
                filter = current.filter,
                onUpdate = {
                    onEditItem(item, current.filter) { success ->
                        if (!success) projection = ProjectedFilterItemState.Failed
                        if (success && selectedScope) onSelectedItemUpdate()
                    }
                },
                onOpenPagedGroup = {},
                onRequestSuggestions = onRequestSuggestions,
            )
        }
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
    data object Loading : ProjectedFilterItemState
    data class Ready(val filter: EntryFilter<*>) : ProjectedFilterItemState

    data object Failed : ProjectedFilterItemState
}

private fun ProjectedFilterItemState.filterOrNull(): EntryFilter<*>? =
    (this as? ProjectedFilterItemState.Ready)?.filter

private suspend fun projectFilterItemState(
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

private suspend fun projectFilterItem(
    group: EntryFilter.PagedGroup<*>,
    item: EntryFilterPageItem,
    previous: EntryFilter<*>?,
): EntryFilter<*> {
    val projected = group.withProjectedState { it.projectItem(item, previous) }
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
