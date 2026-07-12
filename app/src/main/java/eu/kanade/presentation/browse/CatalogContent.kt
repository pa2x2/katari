package eu.kanade.presentation.browse

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Public
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.paging.LoadState
import androidx.paging.compose.LazyPagingItems
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
import kotlinx.coroutines.flow.StateFlow
import tachiyomi.domain.library.model.LibraryDisplayMode
import tachiyomi.domain.source.model.CatalogListItem
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.i18n.stringResource
import tachiyomi.presentation.core.screens.EmptyScreen
import tachiyomi.presentation.core.screens.EmptyScreenAction
import tachiyomi.presentation.core.screens.LoadingScreen
import tachiyomi.presentation.core.util.plus
import tachiyomi.core.common.i18n.stringResource as coreStringResource

@Composable
fun CatalogContent(
    catalogList: LazyPagingItems<StateFlow<CatalogListItem>>,
    columns: GridCells,
    displayMode: LibraryDisplayMode,
    sourceItemOrientation: EntryItemOrientation,
    snackbarHostState: SnackbarHostState,
    contentPadding: PaddingValues,
    onItemClick: (CatalogListItem) -> Unit,
    onItemLongClick: (CatalogListItem) -> Unit,
    onWebViewClick: (() -> Unit)? = null,
    onSettingsClick: (() -> Unit)? = null,
) {
    val context = LocalContext.current

    val errorState = catalogList.loadState.refresh.takeIf { it is LoadState.Error }
        ?: catalogList.loadState.append.takeIf { it is LoadState.Error }

    val getErrorMessage: (LoadState.Error) -> String = { state ->
        with(context) { state.error.formattedMessage }
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
                        icon = Icons.Outlined.Refresh,
                        onClick = catalogList::refresh,
                    ),
                )
                onWebViewClick?.let {
                    add(
                        EmptyScreenAction(
                            stringRes = MR.strings.action_open_in_web_view,
                            icon = Icons.Outlined.Public,
                            onClick = it,
                        ),
                    )
                }
                onSettingsClick?.let {
                    add(
                        EmptyScreenAction(
                            stringRes = MR.strings.action_settings,
                            icon = Icons.Outlined.Settings,
                            onClick = it,
                        ),
                    )
                }
            },
        )
        return
    }

    when (displayMode) {
        LibraryDisplayMode.ComfortableGrid -> CatalogComfortableGrid(
            catalogList = catalogList,
            columns = columns,
            contentPadding = contentPadding,
            sourceItemOrientation = sourceItemOrientation,
            onItemClick = onItemClick,
            onItemLongClick = onItemLongClick,
        )
        LibraryDisplayMode.ComfortableList -> CatalogComfortableGrid(
            catalogList = catalogList,
            columns = GridCells.Fixed(1),
            contentPadding = contentPadding,
            sourceItemOrientation = sourceItemOrientation,
            onItemClick = onItemClick,
            onItemLongClick = onItemLongClick,
        )
        LibraryDisplayMode.List -> CatalogList(
            catalogList = catalogList,
            contentPadding = contentPadding,
            sourceItemOrientation = sourceItemOrientation,
            onItemClick = onItemClick,
            onItemLongClick = onItemLongClick,
        )
        LibraryDisplayMode.CompactGrid, LibraryDisplayMode.CoverOnlyGrid -> CatalogCompactGrid(
            catalogList = catalogList,
            columns = columns,
            contentPadding = contentPadding,
            sourceItemOrientation = sourceItemOrientation,
            onItemClick = onItemClick,
            onItemLongClick = onItemLongClick,
        )
    }
}

@Composable
private fun CatalogList(
    catalogList: LazyPagingItems<StateFlow<CatalogListItem>>,
    contentPadding: PaddingValues,
    sourceItemOrientation: EntryItemOrientation,
    onItemClick: (CatalogListItem) -> Unit,
    onItemLongClick: (CatalogListItem) -> Unit,
) {
    val listState = rememberLazyListState()

    LazyColumn(
        state = listState,
        contentPadding = contentPadding + PaddingValues(vertical = 8.dp),
    ) {
        item {
            if (catalogList.loadState.prepend is LoadState.Loading) {
                BrowseSourceLoadingItem()
            }
        }

        items(
            count = catalogList.itemCount,
            key = { index -> catalogList.peek(index)?.value?.id ?: "catalog-list-$index" },
        ) { index ->
            val item by catalogList[index]?.collectAsState() ?: return@items
            val entryItem = item as? CatalogListItem.EntryItem
            EntryListItem(
                title = item.title,
                coverData = item.cover,
                coverType = sourceItemOrientation.toListCoverType(),
                coverAlpha = if (item.favorite) CommonEntryItemDefaults.BrowseFavoriteCoverAlpha else 1f,
                badge = {
                    CatalogBadges(isFavorite = item.favorite, entryType = item.entryType)
                },
                onLongClick = { onItemLongClick(item) },
                onClick = { onItemClick(item) },
            )
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
    catalogList: LazyPagingItems<StateFlow<CatalogListItem>>,
    columns: GridCells,
    contentPadding: PaddingValues,
    sourceItemOrientation: EntryItemOrientation,
    onItemClick: (CatalogListItem) -> Unit,
    onItemLongClick: (CatalogListItem) -> Unit,
) {
    val gridState = rememberLazyGridState()

    LazyVerticalGrid(
        columns = columns,
        state = gridState,
        contentPadding = contentPadding + PaddingValues(8.dp),
        verticalArrangement = Arrangement.spacedBy(CommonEntryItemDefaults.GridVerticalSpacer),
        horizontalArrangement = Arrangement.spacedBy(CommonEntryItemDefaults.GridHorizontalSpacer),
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
            val item by catalogList[index]?.collectAsState() ?: return@items
            val entryItem = item as? CatalogListItem.EntryItem
            EntryComfortableGridItem(
                title = item.title,
                coverData = item.cover,
                coverType = sourceItemOrientation.toGridCoverType(),
                coverAlpha = if (item.favorite) CommonEntryItemDefaults.BrowseFavoriteCoverAlpha else 1f,
                coverBadgeEnd = {
                    CatalogBadges(isFavorite = item.favorite, entryType = item.entryType)
                },
                onLongClick = { onItemLongClick(item) },
                onClick = { onItemClick(item) },
            )
        }

        if (catalogList.loadState.refresh is LoadState.Loading || catalogList.loadState.append is LoadState.Loading) {
            item(span = { GridItemSpan(maxLineSpan) }) {
                BrowseSourceLoadingItem()
            }
        }
    }
}

@Composable
private fun CatalogCompactGrid(
    catalogList: LazyPagingItems<StateFlow<CatalogListItem>>,
    columns: GridCells,
    contentPadding: PaddingValues,
    sourceItemOrientation: EntryItemOrientation,
    onItemClick: (CatalogListItem) -> Unit,
    onItemLongClick: (CatalogListItem) -> Unit,
) {
    val gridState = rememberLazyGridState()

    LazyVerticalGrid(
        columns = columns,
        state = gridState,
        contentPadding = contentPadding + PaddingValues(8.dp),
        verticalArrangement = Arrangement.spacedBy(CommonEntryItemDefaults.GridVerticalSpacer),
        horizontalArrangement = Arrangement.spacedBy(CommonEntryItemDefaults.GridHorizontalSpacer),
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
            val item by catalogList[index]?.collectAsState() ?: return@items
            val entryItem = item as? CatalogListItem.EntryItem
            EntryCompactGridItem(
                coverData = item.cover,
                title = item.title,
                onClick = { onItemClick(item) },
                onLongClick = { onItemLongClick(item) },
                coverType = sourceItemOrientation.toGridCoverType(),
                coverAlpha = if (item.favorite) CommonEntryItemDefaults.BrowseFavoriteCoverAlpha else 1f,
                coverBadgeEnd = {
                    CatalogBadges(isFavorite = item.favorite, entryType = item.entryType)
                },
            )
        }

        if (catalogList.loadState.refresh is LoadState.Loading || catalogList.loadState.append is LoadState.Loading) {
            item(span = { GridItemSpan(maxLineSpan) }) {
                BrowseSourceLoadingItem()
            }
        }
    }
}
