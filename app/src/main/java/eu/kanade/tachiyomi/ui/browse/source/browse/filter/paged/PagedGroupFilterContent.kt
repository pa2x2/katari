package eu.kanade.tachiyomi.ui.browse.source.browse.filter.paged

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.paging.LoadState
import androidx.paging.PagingSource
import androidx.paging.compose.collectAsLazyPagingItems
import eu.kanade.tachiyomi.source.entry.EntryFilter
import eu.kanade.tachiyomi.source.entry.EntryFilterPageItem
import eu.kanade.tachiyomi.source.entry.EntryFilterPageLoadReason
import eu.kanade.tachiyomi.source.entry.EntryFilterPageScope
import eu.kanade.tachiyomi.source.entry.EntryFilterTextInput
import eu.kanade.tachiyomi.ui.browse.source.browse.SourceFilterSheetFooter
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import mihon.entry.interactions.catalogue.EntryCatalogueFilterNavigationResult
import mihon.entry.interactions.catalogue.EntryCatalogueFilterSuggestionsResult
import mihon.presentation.kit.chip.FilterChip
import mihon.presentation.kit.content.Text
import mihon.presentation.kit.field.SearchField
import mihon.presentation.kit.theme.ContentTone
import mihon.presentation.kit.theme.KatariTheme
import mihon.presentation.kit.theme.TextRole
import tachiyomi.i18n.*
import tachiyomi.presentation.core.i18n.stringResource

@Composable
internal fun PagedGroupFilterContent(
    filter: EntryFilter.PagedGroup<*>,
    filterRevision: Int,
    onBack: () -> Unit,
    onReset: () -> Unit,
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
    banner: @Composable () -> Unit = {},
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
        PagedGroupFilterHeader(
            title = filter.name,
            selectedCount = filter.currentSelectedItemCount(),
            onBack = onBack,
        )
        banner()

        if (searchOptions != null) {
            SearchField(
                query = query,
                onQueryChange = { browseSession.query = it },
                placeholder = stringResource(MR.strings.action_search_hint),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = KatariTheme.spacing.rowInset, vertical = KatariTheme.spacing.xs),
            )
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = KatariTheme.spacing.rowInset),
            horizontalArrangement = Arrangement.spacedBy(KatariTheme.spacing.s),
        ) {
            FilterChip(
                label = stringResource(MR.strings.browse_filter_available),
                selected = scope == EntryFilterPageScope.AVAILABLE,
                onClick = { browseSession.scope = EntryFilterPageScope.AVAILABLE },
            )
            FilterChip(
                label = stringResource(MR.strings.browse_filter_selected_count, filter.currentSelectedItemCount()),
                selected = scope == EntryFilterPageScope.SELECTED,
                onClick = { browseSession.scope = EntryFilterPageScope.SELECTED },
            )
        }

        if (queryTooShort) {
            Text(
                text = stringResource(
                    MR.strings.browse_filter_type_more_characters,
                    searchOptions.minimumQueryLength,
                ),
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = KatariTheme.spacing.rowInset, vertical = KatariTheme.spacing.l),
                role = TextRole.Body,
                tone = ContentTone.Secondary,
            )
        } else {
            PagedGroupItemList(
                filter = filter,
                items = items,
                listState = listState,
                scope = scope,
                refreshing = refreshing,
                encodedState = encodedState,
                navigationTargets = navigationTargets,
                usesNavigationRail = usesNavigationRail,
                currentNavigationTargetId = currentNavigationTargetId,
                onRefresh = { browseSession.refresh(viewKey) },
                onJump = { browseSession.jump(viewKey, it) },
                onEditItem = onEditItem,
                onRequestSuggestions = onRequestSuggestions,
                modifier = Modifier.weight(1f),
            )
        }
        SourceFilterSheetFooter(
            onReset = {
                onReset()
                items.refresh()
            },
            resetEnabled = true,
            onApply = onBack,
            applyEnabled = true,
            applyLabel = stringResource(MR.strings.action_done),
        )
    }
}
