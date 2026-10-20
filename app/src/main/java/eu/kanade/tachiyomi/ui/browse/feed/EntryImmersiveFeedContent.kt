package eu.kanade.tachiyomi.ui.browse.feed

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsIgnoringVisibility
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import eu.kanade.domain.source.model.FeedItemRef
import eu.kanade.presentation.browse.components.SourceIcon
import eu.kanade.presentation.browse.immersive.rememberEntryImmersivePositionState
import eu.kanade.tachiyomi.ui.browse.feed.timeline.FeedRow
import eu.kanade.tachiyomi.ui.browse.feed.timeline.feedRows
import eu.kanade.tachiyomi.ui.browse.immersive.EntryImmersiveContent
import eu.kanade.tachiyomi.ui.browse.immersive.EntryImmersiveItemKey
import eu.kanade.tachiyomi.ui.browse.immersive.EntryImmersiveViewModel
import kotlinx.collections.immutable.persistentListOf
import kotlinx.coroutines.launch
import mihon.icons.materialsymbols.MaterialSymbols
import mihon.icons.materialsymbols.rounded.Refresh
import mihon.presentation.kit.feedback.EmptyScreen
import mihon.presentation.kit.feedback.EmptyScreenAction
import mihon.presentation.kit.feedback.LoadingScreen
import mihon.presentation.kit.feedback.SnackbarDuration
import mihon.presentation.kit.feedback.SnackbarHostState
import mihon.presentation.kit.feedback.SnackbarResult
import mihon.presentation.kit.surface.Pill
import mihon.presentation.kit.theme.KatariTheme
import tachiyomi.domain.entry.model.Entry
import tachiyomi.domain.source.model.CatalogListItem
import tachiyomi.domain.source.model.Source
import tachiyomi.i18n.*
import tachiyomi.presentation.core.i18n.stringResource

@Composable
internal fun EntryImmersiveFeedContent(
    timelineModel: FeedViewModel<CatalogListItem>,
    immersiveModel: EntryImmersiveViewModel,
    snackbarHostState: SnackbarHostState,
    activeSource: Source,
    feedLabel: FeedLabel,
    onShowFeedPicker: () -> Unit,
    onExitImmersive: () -> Unit,
    onEntryClick: (Entry) -> Unit,
    onLibraryAction: (Entry) -> Unit,
    onPagingBlockedChange: (Boolean) -> Unit,
    jumpToNewestRequest: Int,
    modifier: Modifier = Modifier,
) {
    val timelineState by timelineModel.state.collectAsState()
    val coroutineScope = rememberCoroutineScope()
    val retryLabel = stringResource(MR.strings.action_retry)
    val unknownError = stringResource(MR.strings.unknown_error)

    LaunchedEffect(timelineState.error) {
        val error = timelineState.error ?: return@LaunchedEffect
        if (timelineState.itemRefs.isEmpty()) return@LaunchedEffect
        val result = snackbarHostState.showSnackbar(
            message = error.message ?: unknownError,
            actionLabel = retryLabel,
            duration = SnackbarDuration.Indefinite,
        )
        if (result == SnackbarResult.ActionPerformed) timelineModel.refresh()
    }

    if (!timelineState.hasLoaded && timelineState.isRefreshing && timelineState.itemRefs.isEmpty()) {
        LoadingScreen(modifier)
        return
    }

    if (timelineState.itemRefs.isEmpty()) {
        EmptyScreen(
            modifier = modifier,
            message = timelineState.error?.message ?: stringResource(MR.strings.no_results_found),
            actions = persistentListOf(
                EmptyScreenAction(
                    stringRes = MR.strings.action_retry,
                    icon = MaterialSymbols.Rounded.Refresh,
                    onClick = { timelineModel.refresh() },
                ),
            ),
        )
        return
    }

    val context = LocalContext.current
    // Every row but the divider is a page; the first seen entry carries the marker instead.
    val pages = remember(timelineState.itemRefs, timelineState.gaps, timelineState.newItems) {
        feedRows(timelineState.itemRefs, timelineState.gaps, timelineState.newItems)
            .filterNot { it is FeedRow.SeenDivider }
    }
    val firstSeenPage = pages.indexOfFirst { it is FeedRow.Item && it.ref == timelineState.newItems?.firstSeen }
        .takeIf { it > 0 && pages[it - 1] !is FeedRow.Gap }
    val initialSavedItem = remember(timelineModel) { timelineModel.savedAnchorSnapshot().resolvedItem() }
    val initialPage = pages.indexOfFirst { it is FeedRow.Item && it.ref == initialSavedItem }.coerceAtLeast(0)
    val positionState = rememberEntryImmersivePositionState(
        resetKey = timelineModel,
        initialItemIndex = initialPage,
    )

    EntryImmersiveContent(
        itemCount = pages.size,
        itemIdentity = { page -> pages.itemRef(page)?.toImmersiveItemKey() },
        itemContent = { page ->
            pages.itemRef(page)?.let { rememberFeedEntry(it, timelineModel) }
        },
        immersiveModel = immersiveModel,
        contextLabel = feedLabel.title,
        contextSupportingLabel = feedLabel.subtitle,
        contextLeadingContent = {
            SourceIcon(
                source = activeSource,
                modifier = Modifier
                    .size(KatariTheme.sizes.iconSmall)
                    .clip(KatariTheme.shapes.extraSmall),
            )
        },
        onContextClick = onShowFeedPicker,
        onExitImmersive = onExitImmersive,
        onEntryClick = onEntryClick,
        onLibraryAction = onLibraryAction,
        onPagingBlockedChange = onPagingBlockedChange,
        positionState = positionState,
        refreshing = timelineState.isRefreshing,
        onRefresh = { timelineModel.refresh() },
        pageRequest = jumpToNewestRequest,
        onPageSettled = { page ->
            pages.itemRef(page)?.let { timelineModel.saveAnchor(it, scrollOffset = 0) }
        },
        onNearEnd = { timelineModel.loadMore() },
        nonEntryPage = { page ->
            (pages.getOrNull(page) as? FeedRow.Gap)?.let { row ->
                {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = KatariTheme.spacing.l),
                        contentAlignment = Alignment.Center,
                    ) {
                        FeedGapRow(
                            status = timelineState.gapStatus(row.gap, context),
                            onLoad = { timelineModel.loadGap(row.gap) },
                        )
                    }
                }
            }
        },
        modifier = modifier,
    ) { pagerState ->
        val statusBarTop = WindowInsets.statusBarsIgnoringVisibility.asPaddingValues().calculateTopPadding()
        KeepFeedPositionAcrossPrepends(
            rows = pages,
            viewportKey = pagerState,
            firstVisibleItem = { isItem -> pagerState.currentPage.takeIf(isItem)?.let { it to 0 } },
            requestScrollToItem = { index, _ -> pagerState.requestScrollToPage(index) },
        )
        if (firstSeenPage != null && pagerState.currentPage == firstSeenPage) {
            Pill(
                text = stringResource(MR.strings.browse_feed_seen_before),
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(top = statusBarTop + KatariTheme.spacing.l, start = KatariTheme.spacing.l),
            )
        }
        FeedNewItemsIndicator(
            state = timelineState,
            viewModel = timelineModel,
            rowCount = pages.size,
            viewportKey = pagerState,
            viewport = {
                FeedViewport(
                    canScrollBackward = pagerState.canScrollBackward,
                    isScrollInProgress = pagerState.isScrollInProgress,
                    lastScrolledBackward = pagerState.lastScrolledBackward,
                    totalItemsCount = pagerState.pageCount,
                )
            },
            onScrollToNewest = { coroutineScope.launch { pagerState.scrollToPage(0) } },
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = statusBarTop + KatariTheme.spacing.l),
        )
    }
}

@Composable
private fun rememberFeedEntry(
    itemRef: FeedItemRef,
    timelineModel: FeedViewModel<CatalogListItem>,
): Entry? {
    val item by remember(itemRef, timelineModel) { timelineModel.itemState(itemRef) }.collectAsState()
    return (item as? CatalogListItem.EntryItem)?.entry
}

private fun List<FeedRow>.itemRef(page: Int): FeedItemRef? {
    return (getOrNull(page) as? FeedRow.Item)?.ref
}

private fun FeedItemRef.toImmersiveItemKey(): EntryImmersiveItemKey {
    return EntryImmersiveItemKey(id = id, type = type)
}
