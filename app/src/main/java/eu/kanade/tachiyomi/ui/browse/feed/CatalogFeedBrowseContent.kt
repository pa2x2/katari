package eu.kanade.tachiyomi.ui.browse.feed

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
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
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import eu.kanade.presentation.browse.components.BrowseSourceLoadingItem
import eu.kanade.tachiyomi.source.entry.EntryItemOrientation
import eu.kanade.tachiyomi.ui.browse.feed.timeline.FeedRow
import eu.kanade.tachiyomi.ui.browse.feed.timeline.feedRows
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import mihon.entry.interactions.catalogue.EntryCatalogueSourceInfo
import mihon.icons.materialsymbols.MaterialSymbols
import mihon.icons.materialsymbols.automirroredrounded.Help
import mihon.icons.materialsymbols.rounded.OpenInBrowser
import mihon.icons.materialsymbols.rounded.Refresh
import mihon.presentation.kit.feedback.EmptyScreen
import mihon.presentation.kit.feedback.EmptyScreenAction
import mihon.presentation.kit.feedback.LoadingScreen
import mihon.presentation.kit.feedback.SnackbarDuration
import mihon.presentation.kit.feedback.SnackbarHostState
import mihon.presentation.kit.feedback.SnackbarResult
import mihon.presentation.kit.layout.RowGridColumn
import mihon.presentation.kit.layout.coverGridCells
import mihon.presentation.kit.navigation.rememberAppBarExpander
import mihon.presentation.kit.theme.KatariTheme
import tachiyomi.domain.library.model.CoverSize
import tachiyomi.domain.library.model.LibraryDisplayMode
import tachiyomi.domain.source.model.CatalogListItem
import tachiyomi.i18n.*
import tachiyomi.presentation.core.i18n.stringResource
import tachiyomi.source.local.LocalSource
import tachiyomi.i18n.context.stringResource as coreStringResource

@Composable
fun CatalogFeedBrowseContent(
    source: EntryCatalogueSourceInfo?,
    viewModel: FeedViewModel<CatalogListItem>,
    coverSize: CoverSize,
    displayMode: LibraryDisplayMode,
    snackbarHostState: SnackbarHostState,
    contentPadding: PaddingValues,
    onWebViewClick: () -> Unit,
    onHelpClick: () -> Unit,
    onLocalSourceHelpClick: () -> Unit,
    onItemClick: (CatalogListItem) -> Unit,
    onItemLongClick: (CatalogListItem) -> Unit,
    itemMenu: @Composable (CatalogListItem) -> Unit,
) {
    val context = LocalContext.current
    val state by viewModel.state.collectAsState()
    val listState = rememberLazyListState()
    val gridState = rememberLazyGridState()
    val scope = rememberCoroutineScope()
    val rows = remember(state.itemRefs, state.gaps, state.newItems) {
        feedRows(state.itemRefs, state.gaps, state.newItems)
    }
    // This content leaves composition while immersive mode is shown. Keep this guard
    // composition-local so returning to the regular feed restores its latest anchor.
    var restoredDisplayMode by remember { mutableStateOf<String?>(null) }

    val requiresRepair = state.error?.requiresFilterRepair() == true
    val savedAnchor = viewModel.savedAnchorSnapshot()

    LaunchedEffect(state.error) {
        val error = state.error ?: return@LaunchedEffect
        if (state.itemRefs.isEmpty() || requiresRepair) return@LaunchedEffect

        val result = snackbarHostState.showSnackbar(
            message = context.feedErrorMessage(error),
            actionLabel = context.coreStringResource(MR.strings.action_retry),
            duration = SnackbarDuration.Indefinite,
        )
        when (result) {
            SnackbarResult.Dismissed -> snackbarHostState.currentSnackbarData?.dismiss()
            SnackbarResult.ActionPerformed -> viewModel.refresh()
        }
    }

    LaunchedEffect(displayMode, rows, savedAnchor) {
        if (rows.isEmpty()) return@LaunchedEffect

        val modeKey = displayMode.serialize()
        if (restoredDisplayMode == modeKey) return@LaunchedEffect

        val anchorItem = savedAnchor.resolvedItem()
        val anchorIndex = rows.indexOfFirst { it is FeedRow.Item && it.ref == anchorItem }.coerceAtLeast(0)

        when (displayMode) {
            LibraryDisplayMode.List -> listState.scrollToItem(anchorIndex, savedAnchor.scrollOffset)
            else -> gridState.scrollToItem(anchorIndex, savedAnchor.scrollOffset)
        }

        restoredDisplayMode = modeKey
    }

    LaunchedEffect(displayMode, rows) {
        val firstVisible = when (displayMode) {
            LibraryDisplayMode.List -> snapshotFlow {
                listState.firstVisibleItemIndex to
                    listState.firstVisibleItemScrollOffset
            }
            else -> snapshotFlow { gridState.firstVisibleItemIndex to gridState.firstVisibleItemScrollOffset }
        }
        firstVisible
            .distinctUntilChanged()
            .debounce(ANCHOR_SAVE_DEBOUNCE_MILLIS)
            .collectLatest { (index, offset) ->
                val item = rows.getOrNull(index) as? FeedRow.Item ?: return@collectLatest
                viewModel.saveAnchor(itemRef = item.ref, scrollOffset = offset)
            }
    }

    KeepFeedPositionAcrossPrepends(
        rows = rows,
        viewportKey = displayMode,
        firstVisibleItem = { isItem ->
            when (displayMode) {
                LibraryDisplayMode.List -> listState.firstVisibleFeedItem(isItem)
                else -> gridState.firstVisibleFeedItem(isItem)
            }
        },
        requestScrollToItem = { index, scrollOffset ->
            when (displayMode) {
                LibraryDisplayMode.List -> listState.requestScrollToItem(index, scrollOffset)
                else -> gridState.requestScrollToItem(index, scrollOffset)
            }
        },
    )

    LaunchedEffect(
        displayMode,
        state.isRefreshing,
        state.isAppending,
        state.appendError,
        state.nextPageKey,
        rows.size,
    ) {
        val lastVisibleFlow = when (displayMode) {
            LibraryDisplayMode.List -> snapshotFlow { listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: -1 }
            else -> snapshotFlow { gridState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: -1 }
        }
        lastVisibleFlow
            .distinctUntilChanged()
            .collectLatest { lastVisibleItemIndex ->
                if (shouldLoadMore(lastVisibleItemIndex, rows.size, state)) {
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
            message = state.error?.let(context::feedErrorMessage) ?: stringResource(MR.strings.no_results_found),
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
    val columns = coverGridCells(coverSize, wideCovers = sourceItemOrientation == EntryItemOrientation.HORIZONTAL)
    val showFooter = state.isAppending || state.appendError != null
    val footer: @Composable () -> Unit = {
        val appendError = state.appendError
        if (state.isAppending || appendError == null) {
            BrowseSourceLoadingItem()
        } else {
            FeedAppendErrorRow(message = context.feedErrorMessage(appendError), onRetry = viewModel::loadMore)
        }
    }
    // In lists the boundary rows line up with the edges of the entry cards around them; in grids the content padding
    // already insets them.
    val boundaryModifier = if (displayMode == LibraryDisplayMode.List) {
        Modifier.padding(horizontal = KatariTheme.spacing.m)
    } else {
        Modifier
    }
    val rowContent: @Composable (FeedRow, @Composable (FeedRow.Item) -> Unit) -> Unit = { row, item ->
        when (row) {
            is FeedRow.Item -> item(row)
            is FeedRow.Gap -> FeedGapRow(
                status = state.gapStatus(row.gap, context),
                onLoad = { viewModel.loadGap(row.gap) },
                modifier = boundaryModifier,
            )
            FeedRow.SeenDivider -> FeedSeenDivider(modifier = boundaryModifier)
        }
    }

    val comfortableRowContent: @Composable (FeedRow) -> Unit = { row ->
        rowContent(row) {
            CatalogFeedComfortableGridItem(
                ref = it.ref,
                viewModel = viewModel,
                sourceItemOrientation = sourceItemOrientation,
                onItemClick = onItemClick,
                onItemLongClick = onItemLongClick,
                itemMenu = itemMenu,
            )
        }
    }

    val backToTop = rememberFeedBackToTopState(
        viewportHeight = {
            when (displayMode) {
                LibraryDisplayMode.List -> listState.layoutInfo.viewportSize.height
                else -> gridState.layoutInfo.viewportSize.height
            }
        },
    )

    Box(modifier = Modifier.nestedScroll(backToTop)) {
        when (displayMode) {
            LibraryDisplayMode.List -> FeedLazyList(
                rows = rows,
                listState = listState,
                contentPadding = contentPadding,
                showFooter = showFooter,
                rowContent = { row ->
                    rowContent(row) {
                        CatalogFeedListItem(
                            ref = it.ref,
                            viewModel = viewModel,
                            onItemClick = onItemClick,
                            onItemLongClick = onItemLongClick,
                            itemMenu = itemMenu,
                        )
                    }
                },
                footer = footer,
            )
            LibraryDisplayMode.ComfortableGrid -> FeedLazyGrid(
                rows = rows,
                columns = columns,
                gridState = gridState,
                contentPadding = contentPadding,
                showFooter = showFooter,
                rowContent = comfortableRowContent,
                footer = footer,
            )
            LibraryDisplayMode.ComfortableList -> RowGridColumn(contentPadding) { rowColumns, rowPadding ->
                FeedLazyGrid(
                    rows = rows,
                    columns = rowColumns,
                    gridState = gridState,
                    contentPadding = rowPadding,
                    showFooter = showFooter,
                    rowContent = comfortableRowContent,
                    footer = footer,
                )
            }
            LibraryDisplayMode.CompactGrid, LibraryDisplayMode.CoverOnlyGrid -> FeedLazyGrid(
                rows = rows,
                columns = columns,
                gridState = gridState,
                contentPadding = contentPadding,
                showFooter = showFooter,
                rowContent = { row ->
                    rowContent(row) {
                        CatalogFeedCompactGridItem(
                            ref = it.ref,
                            viewModel = viewModel,
                            sourceItemOrientation = sourceItemOrientation,
                            onItemClick = onItemClick,
                            onItemLongClick = onItemLongClick,
                            itemMenu = itemMenu,
                        )
                    }
                },
                footer = footer,
            )
        }

        val expandAppBar = rememberAppBarExpander()
        val scrollToTop: () -> Unit = {
            scope.launch {
                when (displayMode) {
                    LibraryDisplayMode.List -> listState.animateScrollToItem(0)
                    else -> gridState.animateScrollToItem(0)
                }
            }
            scope.launch { expandAppBar() }
        }

        // Within the first screen of rows the top is one flick away, so the button would only add noise there.
        val isPastFirstScreen by remember(displayMode, listState, gridState) {
            derivedStateOf {
                when (displayMode) {
                    LibraryDisplayMode.List ->
                        listState.firstVisibleItemIndex >= listState.layoutInfo.visibleItemsInfo.size
                    else -> gridState.firstVisibleItemIndex >= gridState.layoutInfo.visibleItemsInfo.size
                }
            }
        }
        if (backToTop.isRevealed && isPastFirstScreen) {
            FeedBackToTopButton(
                onClick = {
                    backToTop.hide()
                    scrollToTop()
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
            rowCount = rows.size,
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
            onScrollToNewest = scrollToTop,
            modifier = Modifier
                .padding(top = KatariTheme.spacing.l)
                .align(androidx.compose.ui.Alignment.TopCenter),
        )
    }
}

// A failed append waits for Retry instead of loading again as soon as the end of the feed is visible.
private fun shouldLoadMore(
    lastVisibleItemIndex: Int,
    rowCount: Int,
    state: FeedViewModel.State,
): Boolean {
    if (lastVisibleItemIndex < 0) return false
    if (state.isRefreshing || state.isAppending || state.appendError != null || state.nextPageKey == null) return false

    return lastVisibleItemIndex >= rowCount - 1 - LOAD_MORE_VISIBLE_THRESHOLD
}

private const val ANCHOR_SAVE_DEBOUNCE_MILLIS = 150L
private const val LOAD_MORE_VISIBLE_THRESHOLD = 3
