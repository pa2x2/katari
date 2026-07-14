package eu.kanade.tachiyomi.ui.browse.feed

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.HelpOutline
import androidx.compose.material.icons.outlined.KeyboardArrowUp
import androidx.compose.material.icons.outlined.Public
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import eu.kanade.domain.source.model.FeedItemRef
import eu.kanade.presentation.browse.components.BrowseSourceLoadingItem
import eu.kanade.presentation.browse.components.CatalogBadges
import eu.kanade.presentation.entry.components.toGridCoverType
import eu.kanade.presentation.entry.components.toListCoverType
import eu.kanade.presentation.library.components.CommonEntryItemDefaults
import eu.kanade.presentation.library.components.EntryComfortableGridItem
import eu.kanade.presentation.library.components.EntryCompactGridItem
import eu.kanade.presentation.library.components.EntryListItem
import eu.kanade.presentation.util.formattedMessage
import eu.kanade.tachiyomi.source.entry.EntryItemOrientation
import eu.kanade.tachiyomi.source.sourceItemOrientation
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import tachiyomi.domain.library.model.LibraryDisplayMode
import tachiyomi.domain.source.model.CatalogListItem
import tachiyomi.domain.source.service.CatalogSource
import tachiyomi.domain.source.service.SourceManager
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.components.material.padding
import tachiyomi.presentation.core.i18n.pluralStringResource
import tachiyomi.presentation.core.i18n.stringResource
import tachiyomi.presentation.core.screens.EmptyScreen
import tachiyomi.presentation.core.screens.EmptyScreenAction
import tachiyomi.presentation.core.screens.LoadingScreen
import tachiyomi.presentation.core.util.plus
import tachiyomi.source.local.LocalSource
import eu.kanade.presentation.entry.components.EntryCover as CoverType
import tachiyomi.core.common.i18n.stringResource as coreStringResource

@Composable
fun CatalogFeedBrowseContent(
    source: CatalogSource?,
    screenModel: FeedScreenModel<CatalogListItem>,
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
    val state by screenModel.state.collectAsState()
    val listState = rememberLazyListState()
    val gridState = rememberLazyGridState()
    val scope = rememberCoroutineScope()
    val bridgeItemCount = if (state.isBridgingRefresh) 1 else 0
    // This content leaves composition while immersive mode is shown. Keep this guard
    // composition-local so returning to the regular feed restores its latest anchor.
    var restoredDisplayMode by remember { mutableStateOf<String?>(null) }

    val getErrorMessage: (Throwable) -> String = { throwable ->
        with(context) { throwable.formattedMessage }
    }
    val savedAnchor = screenModel.savedAnchorSnapshot()

    LaunchedEffect(state.error) {
        val error = state.error ?: return@LaunchedEffect
        if (state.itemRefs.isEmpty()) return@LaunchedEffect

        val result = snackbarHostState.showSnackbar(
            message = getErrorMessage(error),
            actionLabel = context.coreStringResource(MR.strings.action_retry),
            duration = SnackbarDuration.Indefinite,
        )
        when (result) {
            SnackbarResult.Dismissed -> snackbarHostState.currentSnackbarData?.dismiss()
            SnackbarResult.ActionPerformed -> screenModel.refresh()
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
                screenModel.saveAnchor(
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
                screenModel.saveAnchor(
                    itemRef = state.itemRefs.getOrNull(itemIndex),
                    scrollOffset = offset,
                )
            }
    }

    LaunchedEffect(displayMode, state.isRefreshing, state.isAppending, state.nextPageKey, state.itemRefs.size) {
        val lastVisibleFlow = when (displayMode) {
            LibraryDisplayMode.List -> snapshotFlow { listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: -1 }
            else -> snapshotFlow { gridState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: -1 }
        }
        lastVisibleFlow
            .distinctUntilChanged()
            .collectLatest { lastVisibleItemIndex ->
                if (shouldLoadMore(lastVisibleItemIndex, state)) {
                    screenModel.loadMore()
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
            actions = if (source?.source?.id == LocalSource.ID) {
                listOf(
                    EmptyScreenAction(
                        stringRes = MR.strings.local_source_help_guide,
                        icon = Icons.AutoMirrored.Outlined.HelpOutline,
                        onClick = onLocalSourceHelpClick,
                    ),
                )
            } else {
                listOf(
                    EmptyScreenAction(
                        stringRes = MR.strings.action_retry,
                        icon = Icons.Outlined.Refresh,
                        onClick = screenModel::refresh,
                    ),
                    EmptyScreenAction(
                        stringRes = MR.strings.action_open_in_web_view,
                        icon = Icons.Outlined.Public,
                        onClick = onWebViewClick,
                    ),
                    EmptyScreenAction(
                        stringRes = MR.strings.label_help,
                        icon = Icons.AutoMirrored.Outlined.HelpOutline,
                        onClick = onHelpClick,
                    ),
                )
            },
        )
        return
    }

    val sourceItemOrientation = source?.source?.sourceItemOrientation() ?: EntryItemOrientation.VERTICAL

    Box {
        when (displayMode) {
            LibraryDisplayMode.ComfortableGrid -> {
                CatalogFeedComfortableGrid(
                    screenModel = screenModel,
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
                    screenModel = screenModel,
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
                    screenModel = screenModel,
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
                    screenModel = screenModel,
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

        if (
            when (displayMode) {
                LibraryDisplayMode.List -> listState.firstVisibleItemIndex > 0 && listState.lastScrolledBackward
                else -> gridState.firstVisibleItemIndex > 0 && gridState.lastScrolledBackward
            }
        ) {
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
                        end = 16.dp,
                        bottom = contentPadding.calculateBottomPadding() + 16.dp,
                    )
                    .align(androidx.compose.ui.Alignment.BottomEnd),
            )
        }

        FeedNewItemsIndicator(
            state = state,
            screenModel = screenModel,
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
                .padding(top = 16.dp)
                .align(androidx.compose.ui.Alignment.TopCenter),
        )
    }
}

@Composable
internal fun FeedNewItemsIndicator(
    state: FeedScreenModel.State,
    screenModel: FeedScreenModel<*>,
    viewportKey: Any?,
    viewport: () -> FeedViewport,
    onScrollToNewest: () -> Unit,
    modifier: Modifier = Modifier,
) {
    LaunchedEffect(viewportKey, state.newItemsAvailableCount, state.pendingRefresh, state.itemRefs.size) {
        if (state.newItemsAvailableCount == 0) return@LaunchedEffect
        if (state.pendingRefresh != null) return@LaunchedEffect

        snapshotFlow(viewport)
            .distinctUntilChanged()
            .collectLatest { currentViewport ->
                if (shouldConsumeNewItemsIndicator(currentViewport, state.itemRefs.size)) {
                    screenModel.consumeNewItemsIndicator()
                }
            }
    }

    if (!shouldShowNewItemsChip(state, viewport().canScrollBackward)) return

    NewItemsChip(
        count = state.newItemsAvailableCount,
        countIsLowerBound = state.newItemsCountIsLowerBound,
        isBridging = state.isBridgingRefresh,
        onClick = {
            screenModel.showNewItems()
            onScrollToNewest()
        },
        modifier = modifier,
    )
}

internal fun shouldShowNewItemsChip(
    state: FeedScreenModel.State,
    canScrollBackward: Boolean,
): Boolean {
    return state.newItemsAvailableCount > 0 &&
        !state.isRefreshing &&
        (state.pendingRefresh != null || canScrollBackward)
}

internal data class FeedViewport(
    val canScrollBackward: Boolean,
    val isScrollInProgress: Boolean,
    val lastScrolledBackward: Boolean,
    val totalItemsCount: Int,
)

internal fun shouldConsumeNewItemsIndicator(
    viewport: FeedViewport,
    itemCount: Int,
): Boolean {
    if (viewport.totalItemsCount < itemCount) return false

    return !viewport.canScrollBackward ||
        (viewport.isScrollInProgress && viewport.lastScrolledBackward)
}

@Composable
internal fun NewItemsChip(
    count: Int,
    countIsLowerBound: Boolean,
    isBridging: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.clickable(onClick = onClick),
        color = MaterialTheme.colorScheme.secondaryContainer,
        contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
        shape = MaterialTheme.shapes.extraLarge,
        tonalElevation = 3.dp,
        shadowElevation = 3.dp,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
        ) {
            if (isBridging) {
                CircularProgressIndicator(
                    modifier = Modifier.size(18.dp),
                    strokeWidth = 2.dp,
                )
            } else {
                Icon(
                    imageVector = Icons.Outlined.KeyboardArrowUp,
                    contentDescription = null,
                )
            }
            Text(
                style = MaterialTheme.typography.labelLarge,
                text = pluralStringResource(
                    if (countIsLowerBound) {
                        MR.plurals.browse_feed_new_items_at_least
                    } else {
                        MR.plurals.browse_feed_new_items
                    },
                    count,
                    count,
                ),
            )
        }
    }
}

@Composable
private fun FeedBackToTopButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.clickable(onClick = onClick),
        color = MaterialTheme.colorScheme.secondaryContainer,
        contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
        shape = MaterialTheme.shapes.extraLarge,
        tonalElevation = 3.dp,
        shadowElevation = 3.dp,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = Icons.Outlined.KeyboardArrowUp,
                contentDescription = null,
            )
            Text(
                style = MaterialTheme.typography.labelLarge,
                text = stringResource(MR.strings.action_move_to_top),
            )
        }
    }
}

@Composable
private fun CatalogFeedList(
    screenModel: FeedScreenModel<CatalogListItem>,
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
        contentPadding = contentPadding + PaddingValues(vertical = 8.dp),
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
                screenModel = screenModel,
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
    screenModel: FeedScreenModel<CatalogListItem>,
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
        contentPadding = contentPadding + PaddingValues(8.dp),
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
                screenModel = screenModel,
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
    screenModel: FeedScreenModel<CatalogListItem>,
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
        contentPadding = contentPadding + PaddingValues(8.dp),
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
                screenModel = screenModel,
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
            .padding(top = 64.dp, bottom = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp, androidx.compose.ui.Alignment.CenterHorizontally),
        verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
    ) {
        CircularProgressIndicator(
            modifier = Modifier.size(20.dp),
            strokeWidth = 2.dp,
        )
        Text(
            text = stringResource(MR.strings.browse_feed_loading_newer_items),
            style = MaterialTheme.typography.labelLarge,
        )
    }
}

private const val BRIDGE_LOADING_ITEM_KEY = "feed-bridge-loading"

private fun FeedItemRef.saveableKey(): String {
    return "feed-${type.name}-$id"
}

@Composable
private fun CatalogFeedListItem(
    ref: FeedItemRef,
    screenModel: FeedScreenModel<CatalogListItem>,
    sourceItemOrientation: EntryItemOrientation,
    onItemClick: (CatalogListItem) -> Unit,
    onItemLongClick: (CatalogListItem) -> Unit,
) {
    val item = rememberCatalogItem(ref, screenModel)
    if (item == null) {
        CatalogFeedListItemPlaceholder(sourceItemOrientation.toListCoverType())
        return
    }

    EntryListItem(
        title = item.title,
        coverData = item.cover,
        coverType = sourceItemOrientation.toListCoverType(),
        coverAlpha = browseCoverAlpha(item.favorite),
        badge = {
            CatalogBadges(isFavorite = item.favorite, entryType = item.entryType)
        },
        onLongClick = { onItemLongClick(item) },
        onClick = { onItemClick(item) },
    )
}

@Composable
private fun CatalogFeedCompactGridItem(
    ref: FeedItemRef,
    screenModel: FeedScreenModel<CatalogListItem>,
    sourceItemOrientation: EntryItemOrientation,
    onItemClick: (CatalogListItem) -> Unit,
    onItemLongClick: (CatalogListItem) -> Unit,
) {
    val item = rememberCatalogItem(ref, screenModel)
    if (item == null) {
        CatalogFeedCompactGridItemPlaceholder(sourceItemOrientation.toGridCoverType())
        return
    }

    EntryCompactGridItem(
        title = item.title,
        coverData = item.cover,
        coverType = sourceItemOrientation.toGridCoverType(),
        coverAlpha = browseCoverAlpha(item.favorite),
        coverBadgeStart = {
            CatalogBadges(isFavorite = item.favorite, entryType = item.entryType)
        },
        onLongClick = { onItemLongClick(item) },
        onClick = { onItemClick(item) },
    )
}

@Composable
private fun CatalogFeedComfortableGridItem(
    ref: FeedItemRef,
    screenModel: FeedScreenModel<CatalogListItem>,
    sourceItemOrientation: EntryItemOrientation,
    onItemClick: (CatalogListItem) -> Unit,
    onItemLongClick: (CatalogListItem) -> Unit,
) {
    val item = rememberCatalogItem(ref, screenModel)
    if (item == null) {
        CatalogFeedComfortableGridItemPlaceholder(sourceItemOrientation.toGridCoverType())
        return
    }

    EntryComfortableGridItem(
        title = item.title,
        coverData = item.cover,
        coverType = sourceItemOrientation.toGridCoverType(),
        coverAlpha = browseCoverAlpha(item.favorite),
        coverBadgeStart = {
            CatalogBadges(isFavorite = item.favorite, entryType = item.entryType)
        },
        onLongClick = { onItemLongClick(item) },
        onClick = { onItemClick(item) },
    )
}

@Composable
private fun rememberCatalogItem(
    ref: FeedItemRef,
    screenModel: FeedScreenModel<CatalogListItem>,
): CatalogListItem? {
    var item by remember(ref) { mutableStateOf<CatalogListItem?>(null) }

    LaunchedEffect(ref, screenModel) {
        screenModel.subscribeItem(ref).collectLatest {
            item = it
        }
    }

    return item
}

@Composable
private fun CatalogFeedListItemPlaceholder(coverType: CoverType) {
    Box(
        modifier = Modifier
            .height(56.dp)
            .padding(horizontal = 16.dp, vertical = 8.dp),
    ) {
        CatalogFeedPlaceholderBlock(
            modifier = Modifier
                .fillMaxHeight()
                .aspectRatio(coverType.ratio),
        )
    }
}

@Composable
private fun CatalogFeedCompactGridItemPlaceholder(coverType: CoverType) {
    CatalogFeedPlaceholderBlock(
        modifier = Modifier
            .padding(4.dp)
            .fillMaxWidth()
            .aspectRatio(coverType.ratio),
    )
}

@Composable
private fun CatalogFeedComfortableGridItemPlaceholder(coverType: CoverType) {
    Box(
        modifier = Modifier.padding(4.dp),
    ) {
        CatalogFeedPlaceholderBlock(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(coverType.ratio),
        )
    }
}

@Composable
private fun CatalogFeedPlaceholderBlock(
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .clip(MaterialTheme.shapes.extraSmall)
            .background(MaterialTheme.colorScheme.surfaceContainerHighest),
    )
}

private fun browseCoverAlpha(favorite: Boolean): Float {
    return if (favorite) CommonEntryItemDefaults.BrowseFavoriteCoverAlpha else 1f
}

private fun shouldLoadMore(
    lastVisibleItemIndex: Int,
    state: FeedScreenModel.State,
): Boolean {
    if (lastVisibleItemIndex < 0) return false
    if (state.isRefreshing || state.isAppending || state.nextPageKey == null) return false

    return lastVisibleItemIndex >= state.itemRefs.lastIndex - LOAD_MORE_VISIBLE_THRESHOLD
}

private const val ANCHOR_SAVE_DEBOUNCE_MILLIS = 150L
private const val LOAD_MORE_VISIBLE_THRESHOLD = 3
