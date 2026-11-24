package eu.kanade.presentation.browse

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.paging.LoadState
import androidx.paging.compose.LazyPagingItems
import app.katari.core.platform.currentPlatformContext
import app.katari.icons.materialsymbols.MaterialSymbols
import app.katari.icons.materialsymbols.rounded.OpenInBrowser
import app.katari.icons.materialsymbols.rounded.Refresh
import app.katari.icons.materialsymbols.rounded.Settings
import app.katari.presentation.kit.feedback.EmptyScreen
import app.katari.presentation.kit.feedback.EmptyScreenAction
import app.katari.presentation.kit.feedback.LoadingScreen
import app.katari.presentation.kit.feedback.SnackbarDuration
import app.katari.presentation.kit.feedback.SnackbarHostState
import app.katari.presentation.kit.feedback.SnackbarResult
import app.katari.presentation.kit.layout.CoverGrid
import app.katari.presentation.kit.layout.CoverSizing
import app.katari.presentation.kit.layout.RowList
import app.katari.presentation.kit.layout.coverGridCells
import app.katari.presentation.kit.layout.coverListCells
import app.katari.presentation.kit.list.RowGroupItem
import app.katari.presentation.kit.list.RowGroupPosition
import app.katari.presentation.kit.theme.KatariTheme
import app.katari.ui.di.LocalAppUiGraph
import eu.kanade.presentation.browse.components.BrowseSourceLoadingItem
import eu.kanade.presentation.browse.components.CatalogBadges
import eu.kanade.presentation.browse.immersive.EntryImmersivePositionState
import eu.kanade.presentation.entry.components.toGridCoverType
import eu.kanade.presentation.library.components.CommonEntryItemDefaults
import eu.kanade.presentation.library.components.EntryComfortableGridItem
import eu.kanade.presentation.library.components.EntryCompactGridItem
import eu.kanade.presentation.library.components.EntryListItem
import eu.kanade.tachiyomi.source.entry.EntryItemOrientation
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import tachiyomi.domain.library.model.LibraryDisplayMode
import tachiyomi.domain.source.model.CatalogListItem
import tachiyomi.i18n.*
import tachiyomi.presentation.core.i18n.stringResource
import tachiyomi.presentation.core.util.plus
import tachiyomi.i18n.platform.stringResource as coreStringResource

@Composable
fun CatalogContent(
    catalogList: LazyPagingItems<StateFlow<CatalogListItem?>>,
    sizing: CoverSizing,
    displayMode: LibraryDisplayMode,
    sourceItemOrientation: EntryItemOrientation,
    showsTypeBadge: Boolean,
    snackbarHostState: SnackbarHostState,
    contentPadding: PaddingValues,
    onItemClick: (CatalogListItem) -> Unit,
    onItemLongClick: (CatalogListItem) -> Unit,
    itemMenu: @Composable (CatalogListItem) -> Unit = {},
    onWebViewClick: (() -> Unit)? = null,
    onSettingsClick: (() -> Unit)? = null,
    positionState: EntryImmersivePositionState? = null,
) {
    val context = currentPlatformContext()
    val errorMessages = LocalAppUiGraph.current.sourceErrorMessages

    val errorState = catalogList.loadState.refresh.takeIf { it is LoadState.Error }
        ?: catalogList.loadState.append.takeIf { it is LoadState.Error }

    val getErrorMessage: (LoadState.Error) -> String = { state ->
        errorMessages.message(context, state.error)
    }

    LaunchedEffect(errorState) {
        if (catalogList.itemCount > 0 && errorState is LoadState.Error) {
            val result = snackbarHostState.showSnackbar(
                message = getErrorMessage(errorState),
                actionLabel = context.coreStringResource(MR.strings.action_retry),
                duration = SnackbarDuration.Indefinite,
            )
            when (result) {
                SnackbarResult.Dismissed -> snackbarHostState.currentSnackbarData?.dismiss()
                SnackbarResult.ActionPerformed -> catalogList.retry()
            }
        }
    }

    if (catalogList.itemCount == 0 && catalogList.loadState.refresh is LoadState.Loading) {
        LoadingScreen(Modifier.padding(contentPadding))
        return
    }

    if (catalogList.itemCount == 0) {
        EmptyScreen(
            modifier = Modifier.padding(contentPadding),
            message = when (errorState) {
                is LoadState.Error -> getErrorMessage(errorState)
                else -> stringResource(MR.strings.no_results_found)
            },
            actions = buildList {
                add(
                    EmptyScreenAction(
                        stringRes = MR.strings.action_retry,
                        icon = MaterialSymbols.Rounded.Refresh,
                        onClick = catalogList::refresh,
                    ),
                )
                onWebViewClick?.let {
                    add(
                        EmptyScreenAction(
                            stringRes = MR.strings.action_open_in_web_view,
                            icon = MaterialSymbols.Rounded.OpenInBrowser,
                            onClick = it,
                        ),
                    )
                }
                onSettingsClick?.let {
                    add(
                        EmptyScreenAction(
                            stringRes = MR.strings.action_settings,
                            icon = MaterialSymbols.Rounded.Settings,
                            onClick = it,
                        ),
                    )
                }
            },
        )
        return
    }

    val columns = coverGridCells(sizing.size, wideCovers = sourceItemOrientation == EntryItemOrientation.HORIZONTAL)
    val badges: @Composable (CatalogListItem) -> Unit = { item ->
        CatalogBadges(isFavorite = item.favorite, entryType = item.entryType.takeIf { showsTypeBadge })
    }
    when (displayMode) {
        LibraryDisplayMode.ComfortableGrid -> CatalogComfortableGrid(
            catalogList = catalogList,
            columns = columns,
            sizing = sizing,
            contentPadding = contentPadding,
            sourceItemOrientation = sourceItemOrientation,
            badges = badges,
            onItemClick = onItemClick,
            onItemLongClick = onItemLongClick,
            itemMenu = itemMenu,
            positionState = positionState,
        )
        LibraryDisplayMode.ComfortableList -> CatalogComfortableGrid(
            catalogList = catalogList,
            columns = coverListCells(),
            sizing = null,
            contentPadding = contentPadding,
            sourceItemOrientation = sourceItemOrientation,
            badges = badges,
            onItemClick = onItemClick,
            onItemLongClick = onItemLongClick,
            itemMenu = itemMenu,
            positionState = positionState,
        )
        LibraryDisplayMode.List -> CatalogList(
            catalogList = catalogList,
            contentPadding = contentPadding,
            badges = badges,
            onItemClick = onItemClick,
            onItemLongClick = onItemLongClick,
            itemMenu = itemMenu,
            positionState = positionState,
        )
        LibraryDisplayMode.CompactGrid, LibraryDisplayMode.CoverOnlyGrid -> CatalogCompactGrid(
            catalogList = catalogList,
            columns = columns,
            sizing = sizing,
            showTitles = displayMode == LibraryDisplayMode.CompactGrid,
            contentPadding = contentPadding,
            sourceItemOrientation = sourceItemOrientation,
            badges = badges,
            onItemClick = onItemClick,
            onItemLongClick = onItemLongClick,
            itemMenu = itemMenu,
            positionState = positionState,
        )
    }
}

@Composable
private fun CatalogList(
    catalogList: LazyPagingItems<StateFlow<CatalogListItem?>>,
    contentPadding: PaddingValues,
    badges: @Composable (CatalogListItem) -> Unit,
    onItemClick: (CatalogListItem) -> Unit,
    onItemLongClick: (CatalogListItem) -> Unit,
    itemMenu: @Composable (CatalogListItem) -> Unit,
    positionState: EntryImmersivePositionState?,
) {
    val prependItemCount = if (catalogList.loadState.prepend is LoadState.Loading) 1 else 0
    val listState = rememberLazyListState(
        initialFirstVisibleItemIndex = catalogLazyIndex(
            catalogItemIndex = positionState?.itemIndex ?: 0,
            prependItemCount = prependItemCount,
            itemCount = catalogList.itemCount,
        ),
    )
    TrackCatalogPosition(
        positionState = positionState,
        prependItemCount = prependItemCount,
        itemCount = catalogList.itemCount,
        firstVisibleItemIndex = { listState.firstVisibleItemIndex },
    )

    RowList(
        state = listState,
        contentPadding = contentPadding + PaddingValues(vertical = KatariTheme.spacing.s),
    ) {
        if (prependItemCount > 0) {
            item {
                BrowseSourceLoadingItem()
            }
        }

        items(
            count = catalogList.itemCount,
            key = { index -> catalogList.peek(index)?.value?.id ?: "catalog-list-$index" },
        ) { index ->
            val item = catalogList[index]?.collectAsState()?.value ?: return@items
            // Loaded pages form one card; the last row closes it until the next page arrives.
            RowGroupItem(position = RowGroupPosition.of(index, catalogList.itemCount)) {
                Box {
                    EntryListItem(
                        title = item.title,
                        coverData = item.cover,
                        coverAlpha = if (item.favorite) CommonEntryItemDefaults.BrowseFavoriteCoverAlpha else 1f,
                        badge = { badges(item) },
                        onLongClick = { onItemLongClick(item) },
                        onClick = { onItemClick(item) },
                    )
                    itemMenu(item)
                }
            }
        }

        item {
            if (catalogList.loadState.refresh is LoadState.Loading ||
                catalogList.loadState.append is LoadState.Loading
            ) {
                BrowseSourceLoadingItem()
            }
        }
    }
}

@Composable
private fun CatalogComfortableGrid(
    catalogList: LazyPagingItems<StateFlow<CatalogListItem?>>,
    columns: GridCells,
    sizing: CoverSizing?,
    contentPadding: PaddingValues,
    sourceItemOrientation: EntryItemOrientation,
    badges: @Composable (CatalogListItem) -> Unit,
    onItemClick: (CatalogListItem) -> Unit,
    onItemLongClick: (CatalogListItem) -> Unit,
    itemMenu: @Composable (CatalogListItem) -> Unit,
    positionState: EntryImmersivePositionState?,
) {
    val prependItemCount = if (catalogList.loadState.prepend is LoadState.Loading) 1 else 0
    val gridState = rememberLazyGridState(
        initialFirstVisibleItemIndex = catalogLazyIndex(
            catalogItemIndex = positionState?.itemIndex ?: 0,
            prependItemCount = prependItemCount,
            itemCount = catalogList.itemCount,
        ),
    )
    TrackCatalogPosition(
        positionState = positionState,
        prependItemCount = prependItemCount,
        itemCount = catalogList.itemCount,
        firstVisibleItemIndex = { gridState.firstVisibleItemIndex },
    )

    CoverGrid(
        columns = columns,
        state = gridState,
        contentPadding = contentPadding + PaddingValues(KatariTheme.spacing.s),
        verticalArrangement = Arrangement.spacedBy(CommonEntryItemDefaults.GridVerticalSpacer),
        horizontalArrangement = Arrangement.spacedBy(CommonEntryItemDefaults.GridHorizontalSpacer),
        sizing = sizing,
    ) {
        if (catalogList.loadState.prepend is LoadState.Loading) {
            item(span = { GridItemSpan(maxLineSpan) }) {
                BrowseSourceLoadingItem()
            }
        }

        items(
            count = catalogList.itemCount,
            key = { index -> catalogList.peek(index)?.value?.id ?: "catalog-comfortable-$index" },
        ) { index ->
            val item = catalogList[index]?.collectAsState()?.value ?: return@items
            Box {
                EntryComfortableGridItem(
                    title = item.title,
                    coverData = item.cover,
                    coverType = sourceItemOrientation.toGridCoverType(),
                    coverAlpha = if (item.favorite) CommonEntryItemDefaults.BrowseFavoriteCoverAlpha else 1f,
                    coverBadgeEnd = { badges(item) },
                    onLongClick = { onItemLongClick(item) },
                    onClick = { onItemClick(item) },
                )
                itemMenu(item)
            }
        }

        if (catalogList.loadState.refresh is LoadState.Loading ||
            catalogList.loadState.append is LoadState.Loading
        ) {
            item(span = { GridItemSpan(maxLineSpan) }) {
                BrowseSourceLoadingItem()
            }
        }
    }
}

@Composable
private fun CatalogCompactGrid(
    catalogList: LazyPagingItems<StateFlow<CatalogListItem?>>,
    columns: GridCells,
    sizing: CoverSizing,
    showTitles: Boolean,
    contentPadding: PaddingValues,
    sourceItemOrientation: EntryItemOrientation,
    badges: @Composable (CatalogListItem) -> Unit,
    onItemClick: (CatalogListItem) -> Unit,
    onItemLongClick: (CatalogListItem) -> Unit,
    itemMenu: @Composable (CatalogListItem) -> Unit,
    positionState: EntryImmersivePositionState?,
) {
    val prependItemCount = if (catalogList.loadState.prepend is LoadState.Loading) 1 else 0
    val gridState = rememberLazyGridState(
        initialFirstVisibleItemIndex = catalogLazyIndex(
            catalogItemIndex = positionState?.itemIndex ?: 0,
            prependItemCount = prependItemCount,
            itemCount = catalogList.itemCount,
        ),
    )
    TrackCatalogPosition(
        positionState = positionState,
        prependItemCount = prependItemCount,
        itemCount = catalogList.itemCount,
        firstVisibleItemIndex = { gridState.firstVisibleItemIndex },
    )

    CoverGrid(
        columns = columns,
        state = gridState,
        contentPadding = contentPadding + PaddingValues(KatariTheme.spacing.s),
        verticalArrangement = Arrangement.spacedBy(CommonEntryItemDefaults.GridVerticalSpacer),
        horizontalArrangement = Arrangement.spacedBy(CommonEntryItemDefaults.GridHorizontalSpacer),
        sizing = sizing,
    ) {
        if (catalogList.loadState.prepend is LoadState.Loading) {
            item(span = { GridItemSpan(maxLineSpan) }) {
                BrowseSourceLoadingItem()
            }
        }

        items(
            count = catalogList.itemCount,
            key = { index -> catalogList.peek(index)?.value?.id ?: "catalog-compact-$index" },
        ) { index ->
            val item = catalogList[index]?.collectAsState()?.value ?: return@items
            Box {
                EntryCompactGridItem(
                    coverData = item.cover,
                    title = item.title.takeIf { showTitles },
                    onClick = { onItemClick(item) },
                    onLongClick = { onItemLongClick(item) },
                    coverType = sourceItemOrientation.toGridCoverType(),
                    coverAlpha = if (item.favorite) CommonEntryItemDefaults.BrowseFavoriteCoverAlpha else 1f,
                    coverBadgeEnd = { badges(item) },
                )
                itemMenu(item)
            }
        }

        if (catalogList.loadState.refresh is LoadState.Loading ||
            catalogList.loadState.append is LoadState.Loading
        ) {
            item(span = { GridItemSpan(maxLineSpan) }) {
                BrowseSourceLoadingItem()
            }
        }
    }
}

internal fun catalogLazyIndex(
    catalogItemIndex: Int,
    prependItemCount: Int,
    itemCount: Int,
): Int {
    val boundedItemIndex = catalogItemIndex.coerceIn(0, (itemCount - 1).coerceAtLeast(0))
    return boundedItemIndex + prependItemCount
}

@Composable
private fun TrackCatalogPosition(
    positionState: EntryImmersivePositionState?,
    prependItemCount: Int,
    itemCount: Int,
    firstVisibleItemIndex: () -> Int,
) {
    if (positionState == null) return

    LaunchedEffect(positionState, prependItemCount, itemCount) {
        snapshotFlow {
            catalogItemIndex(
                lazyItemIndex = firstVisibleItemIndex(),
                prependItemCount = prependItemCount,
                itemCount = itemCount,
            )
        }
            .distinctUntilChanged()
            .collect(positionState::updateItemIndex)
    }
}

internal fun catalogItemIndex(
    lazyItemIndex: Int,
    prependItemCount: Int,
    itemCount: Int,
): Int {
    return (lazyItemIndex - prependItemCount).coerceIn(0, (itemCount - 1).coerceAtLeast(0))
}
