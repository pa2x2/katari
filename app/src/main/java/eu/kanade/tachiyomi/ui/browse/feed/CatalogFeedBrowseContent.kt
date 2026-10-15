package eu.kanade.tachiyomi.ui.browse.feed

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import eu.kanade.domain.source.model.FeedItemRef
import eu.kanade.domain.source.model.FilterPresetRepairException
import eu.kanade.presentation.browse.components.BrowseSourceLoadingItem
import eu.kanade.presentation.library.components.CommonEntryItemDefaults
import eu.kanade.presentation.util.formattedMessage
import eu.kanade.tachiyomi.source.entry.EntryItemOrientation
import eu.kanade.tachiyomi.source.entry.filter.EntryFilterValidationException
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import mihon.entry.interactions.catalogue.EntryCatalogueSourceInfo
import mihon.icons.materialsymbols.MaterialSymbols
import mihon.icons.materialsymbols.automirroredrounded.Help
import mihon.icons.materialsymbols.rounded.KeyboardArrowUp
import mihon.icons.materialsymbols.rounded.OpenInBrowser
import mihon.icons.materialsymbols.rounded.Refresh
import mihon.presentation.kit.content.Icon
import mihon.presentation.kit.content.Text
import mihon.presentation.kit.feedback.EmptyScreen
import mihon.presentation.kit.feedback.EmptyScreenAction
import mihon.presentation.kit.feedback.LoadingScreen
import mihon.presentation.kit.feedback.SnackbarDuration
import mihon.presentation.kit.feedback.SnackbarHostState
import mihon.presentation.kit.feedback.SnackbarResult
import mihon.presentation.kit.progress.LoadingIndicator
import mihon.presentation.kit.surface.Surface
import mihon.presentation.kit.surface.SurfaceContainer
import mihon.presentation.kit.theme.KatariTheme
import mihon.presentation.kit.theme.TextRole
import tachiyomi.domain.library.model.LibraryDisplayMode
import tachiyomi.domain.source.model.CatalogListItem
import tachiyomi.i18n.*
import tachiyomi.presentation.core.i18n.stringResource
import tachiyomi.presentation.core.util.plus
import tachiyomi.source.local.LocalSource
import tachiyomi.core.common.i18n.stringResource as coreStringResource

@Composable
fun CatalogFeedBrowseContent(
    source: EntryCatalogueSourceInfo?,
    viewModel: FeedViewModel<CatalogListItem>,
    columns: GridCells,
    displayMode: LibraryDisplayMode,
    snackbarHostState: SnackbarHostState,
    contentPadding: PaddingValues,
    onWebViewClick: () -> Unit,
    onHelpClick: () -> Unit,
    onLocalSourceHelpClick: () -> Unit,
    onItemClick: (CatalogListItem) -> Unit,
    onItemLongClick: (CatalogListItem) -> Unit,
) {
    val context = LocalContext.current
    val state by viewModel.state.collectAsState()
    val listState = rememberLazyListState()
    val gridState = rememberLazyGridState()
    val scope = rememberCoroutineScope()
    val bridgeItemCount = if (state.isBridgingRefresh) 1 else 0
    // This content leaves composition while immersive mode is shown. Keep this guard
    // composition-local so returning to the regular feed restores its latest anchor.
    var restoredDisplayMode by remember { mutableStateOf<String?>(null) }

    val requiresRepair = state.error is FilterPresetRepairException || state.error is EntryFilterValidationException
    val getErrorMessage: (Throwable) -> String = { throwable ->
        if (requiresRepair) {
            context.coreStringResource(MR.strings.filter_repair_help)
        } else {
            with(context) { throwable.formattedMessage }
        }
    }
    val savedAnchor = viewModel.savedAnchorSnapshot()

    LaunchedEffect(state.error) {
        val error = state.error ?: return@LaunchedEffect
        if (state.itemRefs.isEmpty() || requiresRepair) return@LaunchedEffect

        val result = snackbarHostState.showSnackbar(
            message = getErrorMessage(error),
            actionLabel = context.coreStringResource(MR.strings.action_retry),
            duration = SnackbarDuration.Indefinite,
        )
        when (result) {
            SnackbarResult.Dismissed -> snackbarHostState.currentSnackbarData?.dismiss()
            SnackbarResult.ActionPerformed -> viewModel.refresh()
        }
    }

    LaunchedEffect(displayMode, state.itemRefs, state.isBridgingRefresh, savedAnchor) {
        if (state.itemRefs.isEmpty()) return@LaunchedEffect

        val modeKey = displayMode.serialize()
        if (restoredDisplayMode == modeKey) return@LaunchedEffect

        val anchorIndex = (
            savedAnchor.resolvedItem()
                ?.let(state.itemRefs::indexOf)
                ?.takeIf { it >= 0 }
                ?: 0
            ) + bridgeItemCount

        when (displayMode) {
            LibraryDisplayMode.List -> listState.scrollToItem(anchorIndex, savedAnchor.scrollOffset)
            else -> gridState.scrollToItem(anchorIndex, savedAnchor.scrollOffset)
        }

        restoredDisplayMode = modeKey
    }

    LaunchedEffect(displayMode, state.itemRefs, state.isBridgingRefresh) {
        if (displayMode != LibraryDisplayMode.List) return@LaunchedEffect

        snapshotFlow { listState.firstVisibleItemIndex to listState.firstVisibleItemScrollOffset }
            .distinctUntilChanged()
            .debounce(ANCHOR_SAVE_DEBOUNCE_MILLIS)
            .collectLatest { (index, offset) ->
                val itemIndex = index - bridgeItemCount
                if (itemIndex < 0) return@collectLatest
                viewModel.saveAnchor(
                    itemRef = state.itemRefs.getOrNull(itemIndex),
                    scrollOffset = offset,
                )
            }
    }

    LaunchedEffect(displayMode, state.itemRefs, state.isBridgingRefresh) {
        if (displayMode == LibraryDisplayMode.List) return@LaunchedEffect

        snapshotFlow { gridState.firstVisibleItemIndex to gridState.firstVisibleItemScrollOffset }
            .distinctUntilChanged()
            .debounce(ANCHOR_SAVE_DEBOUNCE_MILLIS)
            .collectLatest { (index, offset) ->
                val itemIndex = index - bridgeItemCount
                if (itemIndex < 0) return@collectLatest
                viewModel.saveAnchor(
                    itemRef = state.itemRefs.getOrNull(itemIndex),
                    scrollOffset = offset,
                )
            }
    }

    KeepFeedPositionAcrossPrepends(
        itemRefs = state.itemRefs,
        leadingItemCount = bridgeItemCount,
        viewportKey = displayMode,
        firstVisibleItem = { leadingItemCount ->
            when (displayMode) {
                LibraryDisplayMode.List -> listState.firstVisibleFeedItem(leadingItemCount)
                else -> gridState.firstVisibleFeedItem(leadingItemCount)
            }
        },
        requestScrollToItem = { index, scrollOffset ->
            when (displayMode) {
                LibraryDisplayMode.List -> listState.requestScrollToItem(index, scrollOffset)
                else -> gridState.requestScrollToItem(index, scrollOffset)
            }
        },
    )

    LaunchedEffect(displayMode, state.isRefreshing, state.isAppending, state.nextPageKey, state.itemRefs.size) {
        val lastVisibleFlow = when (displayMode) {
            LibraryDisplayMode.List -> snapshotFlow { listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: -1 }
            else -> snapshotFlow { gridState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: -1 }
        }
        lastVisibleFlow
            .distinctUntilChanged()
            .collectLatest { lastVisibleItemIndex ->
                if (shouldLoadMore(lastVisibleItemIndex, state)) {
                    viewModel.loadMore()
                }
            }
    }

    if (!state.hasLoaded && state.isRefreshing && state.itemRefs.isEmpty()) {
        LoadingScreen(Modifier.padding(contentPadding))
        return
    }

    if (state.itemRefs.isEmpty()) {
        EmptyScreen(
            modifier = Modifier.padding(contentPadding),
            message = state.error?.let(getErrorMessage) ?: stringResource(MR.strings.no_results_found),
            actions = if (requiresRepair) {
                emptyList()
            } else if (source?.id == LocalSource.ID) {
                listOf(
                    EmptyScreenAction(
                        stringRes = MR.strings.local_source_help_guide,
                        icon = MaterialSymbols.AutoMirroredRounded.Help,
                        onClick = onLocalSourceHelpClick,
                    ),
                )
            } else {
                listOf(
                    EmptyScreenAction(
                        stringRes = MR.strings.action_retry,
                        icon = MaterialSymbols.Rounded.Refresh,
                        onClick = viewModel::refresh,
                    ),
                    EmptyScreenAction(
                        stringRes = MR.strings.action_open_in_web_view,
                        icon = MaterialSymbols.Rounded.OpenInBrowser,
                        onClick = onWebViewClick,
                    ),
                    EmptyScreenAction(
                        stringRes = MR.strings.label_help,
                        icon = MaterialSymbols.AutoMirroredRounded.Help,
                        onClick = onHelpClick,
                    ),
                )
            },
        )
        return
    }

    val sourceItemOrientation = source?.itemOrientation ?: EntryItemOrientation.VERTICAL

    Box {
        when (displayMode) {
            LibraryDisplayMode.ComfortableGrid -> {
                CatalogFeedComfortableGrid(
                    viewModel = viewModel,
                    itemRefs = state.itemRefs,
                    columns = columns,
                    contentPadding = contentPadding,
                    gridState = gridState,
                    sourceItemOrientation = sourceItemOrientation,
                    isBridgingRefresh = state.isBridgingRefresh,
                    isAppending = state.isAppending,
                    onItemClick = onItemClick,
                    onItemLongClick = onItemLongClick,
                )
            }
            LibraryDisplayMode.ComfortableList -> {
                CatalogFeedComfortableGrid(
                    viewModel = viewModel,
                    itemRefs = state.itemRefs,
                    columns = GridCells.Fixed(1),
                    contentPadding = contentPadding,
                    gridState = gridState,
                    sourceItemOrientation = sourceItemOrientation,
                    isBridgingRefresh = state.isBridgingRefresh,
                    isAppending = state.isAppending,
                    onItemClick = onItemClick,
                    onItemLongClick = onItemLongClick,
                )
            }
            LibraryDisplayMode.List -> {
                CatalogFeedList(
                    viewModel = viewModel,
                    itemRefs = state.itemRefs,
                    contentPadding = contentPadding,
                    listState = listState,
                    sourceItemOrientation = sourceItemOrientation,
                    isBridgingRefresh = state.isBridgingRefresh,
                    isAppending = state.isAppending,
                    onItemClick = onItemClick,
                    onItemLongClick = onItemLongClick,
                )
            }
            LibraryDisplayMode.CompactGrid, LibraryDisplayMode.CoverOnlyGrid -> {
                CatalogFeedCompactGrid(
                    viewModel = viewModel,
                    itemRefs = state.itemRefs,
                    columns = columns,
                    contentPadding = contentPadding,
                    gridState = gridState,
                    sourceItemOrientation = sourceItemOrientation,
                    isBridgingRefresh = state.isBridgingRefresh,
                    isAppending = state.isAppending,
                    onItemClick = onItemClick,
                    onItemLongClick = onItemLongClick,
                )
            }
        }

        val showBackToTop by remember(displayMode, listState, gridState) {
            derivedStateOf {
                when (displayMode) {
                    LibraryDisplayMode.List -> listState.firstVisibleItemIndex > 0 && listState.lastScrolledBackward
                    else -> gridState.firstVisibleItemIndex > 0 && gridState.lastScrolledBackward
                }
            }
        }
        if (showBackToTop) {
            FeedBackToTopButton(
                onClick = {
                    scope.launch {
                        when (displayMode) {
                            LibraryDisplayMode.List -> listState.animateScrollToItem(0)
                            else -> gridState.animateScrollToItem(0)
                        }
                    }
                },
                modifier = Modifier
                    .padding(
                        end = KatariTheme.spacing.l,
                        bottom = contentPadding.calculateBottomPadding() + KatariTheme.spacing.l,
                    )
                    .align(androidx.compose.ui.Alignment.BottomEnd),
            )
        }

        FeedNewItemsIndicator(
            state = state,
            viewModel = viewModel,
            viewportKey = displayMode,
            viewport = {
                when (displayMode) {
                    LibraryDisplayMode.List -> FeedViewport(
                        canScrollBackward = listState.canScrollBackward,
                        isScrollInProgress = listState.isScrollInProgress,
                        lastScrolledBackward = listState.lastScrolledBackward,
                        totalItemsCount = listState.layoutInfo.totalItemsCount,
                    )
                    else -> FeedViewport(
                        canScrollBackward = gridState.canScrollBackward,
                        isScrollInProgress = gridState.isScrollInProgress,
                        lastScrolledBackward = gridState.lastScrolledBackward,
                        totalItemsCount = gridState.layoutInfo.totalItemsCount,
                    )
                }
            },
            onScrollToNewest = {
                scope.launch {
                    when (displayMode) {
                        LibraryDisplayMode.List -> listState.animateScrollToItem(0)
                        else -> gridState.animateScrollToItem(0)
                    }
                }
            },
            modifier = Modifier
                .padding(top = KatariTheme.spacing.l)
                .align(androidx.compose.ui.Alignment.TopCenter),
        )
    }
}

@Composable
private fun FeedBackToTopButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        onClick = onClick,
        modifier = modifier,
        container = SurfaceContainer.SecondaryContainer,
        shape = KatariTheme.shapes.extraLarge,
        shadow = true,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = KatariTheme.spacing.m, vertical = KatariTheme.spacing.s),
            horizontalArrangement = Arrangement.spacedBy(KatariTheme.spacing.s),
            verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = MaterialSymbols.Rounded.KeyboardArrowUp,
                contentDescription = null,
            )
            Text(
                role = TextRole.Label,
                text = stringResource(MR.strings.action_move_to_top),
            )
        }
    }
}

@Composable
private fun CatalogFeedList(
    viewModel: FeedViewModel<CatalogListItem>,
    itemRefs: List<FeedItemRef>,
    contentPadding: PaddingValues,
    listState: androidx.compose.foundation.lazy.LazyListState,
    sourceItemOrientation: EntryItemOrientation,
    isBridgingRefresh: Boolean,
    isAppending: Boolean,
    onItemClick: (CatalogListItem) -> Unit,
    onItemLongClick: (CatalogListItem) -> Unit,
) {
    LazyColumn(
        state = listState,
        contentPadding = contentPadding + PaddingValues(vertical = KatariTheme.spacing.s),
    ) {
        if (isBridgingRefresh) {
            item(key = BRIDGE_LOADING_ITEM_KEY) {
                FeedBridgeLoadingItem()
            }
        }

        items(
            items = itemRefs,
            key = FeedItemRef::saveableKey,
        ) { ref ->
            CatalogFeedListItem(
                ref = ref,
                viewModel = viewModel,
                sourceItemOrientation = sourceItemOrientation,
                onItemClick = onItemClick,
                onItemLongClick = onItemLongClick,
            )
        }

        if (isAppending) {
            item {
                BrowseSourceLoadingItem()
            }
        }
    }
}

@Composable
private fun CatalogFeedCompactGrid(
    viewModel: FeedViewModel<CatalogListItem>,
    itemRefs: List<FeedItemRef>,
    columns: GridCells,
    contentPadding: PaddingValues,
    gridState: androidx.compose.foundation.lazy.grid.LazyGridState,
    sourceItemOrientation: EntryItemOrientation,
    isBridgingRefresh: Boolean,
    isAppending: Boolean,
    onItemClick: (CatalogListItem) -> Unit,
    onItemLongClick: (CatalogListItem) -> Unit,
) {
    LazyVerticalGrid(
        columns = columns,
        state = gridState,
        contentPadding = contentPadding + PaddingValues(KatariTheme.spacing.s),
        verticalArrangement = Arrangement.spacedBy(CommonEntryItemDefaults.GridVerticalSpacer),
        horizontalArrangement = Arrangement.spacedBy(CommonEntryItemDefaults.GridHorizontalSpacer),
    ) {
        if (isBridgingRefresh) {
            item(
                key = BRIDGE_LOADING_ITEM_KEY,
                span = { GridItemSpan(maxLineSpan) },
            ) {
                FeedBridgeLoadingItem()
            }
        }

        items(
            items = itemRefs,
            key = FeedItemRef::saveableKey,
        ) { ref ->
            CatalogFeedCompactGridItem(
                ref = ref,
                viewModel = viewModel,
                sourceItemOrientation = sourceItemOrientation,
                onItemClick = onItemClick,
                onItemLongClick = onItemLongClick,
            )
        }

        if (isAppending) {
            item(span = { GridItemSpan(maxLineSpan) }) {
                BrowseSourceLoadingItem()
            }
        }
    }
}

@Composable
private fun CatalogFeedComfortableGrid(
    viewModel: FeedViewModel<CatalogListItem>,
    itemRefs: List<FeedItemRef>,
    columns: GridCells,
    contentPadding: PaddingValues,
    gridState: androidx.compose.foundation.lazy.grid.LazyGridState,
    sourceItemOrientation: EntryItemOrientation,
    isBridgingRefresh: Boolean,
    isAppending: Boolean,
    onItemClick: (CatalogListItem) -> Unit,
    onItemLongClick: (CatalogListItem) -> Unit,
) {
    LazyVerticalGrid(
        columns = columns,
        state = gridState,
        contentPadding = contentPadding + PaddingValues(KatariTheme.spacing.s),
        verticalArrangement = Arrangement.spacedBy(CommonEntryItemDefaults.GridVerticalSpacer),
        horizontalArrangement = Arrangement.spacedBy(CommonEntryItemDefaults.GridHorizontalSpacer),
    ) {
        if (isBridgingRefresh) {
            item(
                key = BRIDGE_LOADING_ITEM_KEY,
                span = { GridItemSpan(maxLineSpan) },
            ) {
                FeedBridgeLoadingItem()
            }
        }

        items(
            items = itemRefs,
            key = FeedItemRef::saveableKey,
        ) { ref ->
            CatalogFeedComfortableGridItem(
                ref = ref,
                viewModel = viewModel,
                sourceItemOrientation = sourceItemOrientation,
                onItemClick = onItemClick,
                onItemLongClick = onItemLongClick,
            )
        }

        if (isAppending) {
            item(span = { GridItemSpan(maxLineSpan) }) {
                BrowseSourceLoadingItem()
            }
        }
    }
}

@Composable
private fun FeedBridgeLoadingItem() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            // The top leaves room for the new-items chip that floats over the start of the feed.
            .padding(top = KatariTheme.spacing.xxl * 2, bottom = KatariTheme.spacing.l),
        horizontalArrangement = Arrangement.spacedBy(
            KatariTheme.spacing.s,
            androidx.compose.ui.Alignment.CenterHorizontally,
        ),
        verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
    ) {
        LoadingIndicator(
            modifier = Modifier.size(KatariTheme.sizes.iconSmall),
        )
        Text(
            text = stringResource(MR.strings.browse_feed_loading_newer_items),
            role = TextRole.Label,
        )
    }
}

private const val BRIDGE_LOADING_ITEM_KEY = "feed-bridge-loading"

private fun FeedItemRef.saveableKey(): String {
    return "feed-${type.name}-$id"
}

private fun shouldLoadMore(
    lastVisibleItemIndex: Int,
    state: FeedViewModel.State,
): Boolean {
    if (lastVisibleItemIndex < 0) return false
    if (state.isRefreshing || state.isAppending || state.nextPageKey == null) return false

    return lastVisibleItemIndex >= state.itemRefs.lastIndex - LOAD_MORE_VISIBLE_THRESHOLD
}

private const val ANCHOR_SAVE_DEBOUNCE_MILLIS = 150L
private const val LOAD_MORE_VISIBLE_THRESHOLD = 3
