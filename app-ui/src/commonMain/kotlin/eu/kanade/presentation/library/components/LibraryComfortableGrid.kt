package eu.kanade.presentation.library.components

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import eu.kanade.presentation.entry.components.toLibraryGridCoverType
import eu.kanade.tachiyomi.source.entry.EntryItemOrientation
import tachiyomi.domain.entry.model.asEntryCover
import tachiyomi.domain.library.model.LibraryItem
import tachiyomi.domain.library.model.LibraryItemKey
import tachiyomi.domain.library.model.LibraryPinnedDisplayStyle

@Composable
internal fun LibraryComfortableGrid(
    items: List<LibraryItem>,
    columns: GridCells,
    contentPadding: PaddingValues,
    selection: Set<LibraryItemKey>,
    clicks: LibraryItemClicks,
    onClickContinueReading: ((LibraryItem) -> Unit)?,
    isContinueReadingAvailable: (LibraryItem) -> Boolean,
    displaySettings: LibraryDisplaySettings,
    scrollToTopTarget: LibraryScrollToTopTarget?,
) {
    val gridState = rememberLazyGridState()
    val (pinnedItems, regularItems) = items.partition(LibraryItem::isPinned)
    BindScrollToTop(scrollToTopTarget, gridState) { gridState.animateScrollToItem(0) }
    LazyLibraryGrid(
        modifier = Modifier
            .fillMaxSize()
            .libraryPinnedGridDecoration(
                state = gridState,
                items = pinnedItems,
                style = displaySettings.pinnedDisplayStyle,
            ),
        state = gridState,
        columns = columns,
        contentPadding = contentPadding,
    ) {
        libraryPinnedGridItems(
            items = pinnedItems,
            style = displaySettings.pinnedDisplayStyle,
            columns = columns,
            contentType = "library_comfortable_grid_item",
        ) { libraryItem, modifier ->
            LibraryComfortableGridEntry(
                libraryItem = libraryItem,
                modifier = modifier,
                selection = selection,
                clicks = clicks,
                onClickContinueReading = onClickContinueReading,
                isContinueReadingAvailable = isContinueReadingAvailable,
                displaySettings = displaySettings,
            )
        }

        items(
            items = regularItems,
            key = { it.key.toString() },
            span = { libraryItem ->
                GridItemSpan(
                    if (libraryItem.sourceItemOrientation == EntryItemOrientation.HORIZONTAL) {
                        minOf(2, maxLineSpan)
                    } else {
                        1
                    },
                )
            },
            contentType = { "library_comfortable_grid_item" },
        ) { libraryItem ->
            LibraryComfortableGridEntry(
                libraryItem = libraryItem,
                modifier = if (displaySettings.pinnedDisplayStyle == LibraryPinnedDisplayStyle.TonalGroup) {
                    Modifier.animateItem(fadeInSpec = null, fadeOutSpec = null)
                } else {
                    Modifier.animateItem(placementSpec = null)
                },
                selection = selection,
                clicks = clicks,
                onClickContinueReading = onClickContinueReading,
                isContinueReadingAvailable = isContinueReadingAvailable,
                displaySettings = displaySettings,
            )
        }
    }
}

@Composable
internal fun LibraryComfortableGridEntry(
    libraryItem: LibraryItem,
    selection: Set<LibraryItemKey>,
    clicks: LibraryItemClicks,
    onClickContinueReading: ((LibraryItem) -> Unit)?,
    isContinueReadingAvailable: (LibraryItem) -> Boolean,
    displaySettings: LibraryDisplaySettings,
    modifier: Modifier = Modifier,
) {
    val useFitCover = libraryItem.sourceItemOrientation == EntryItemOrientation.HORIZONTAL
    EntryComfortableGridItem(
        modifier = modifier.libraryItemPointer(libraryItem, selection, clicks),
        isSelected = libraryItem.key in selection,
        selectionMode = selection.isNotEmpty(),
        title = libraryItem.title,
        coverData = libraryItem.entry.asEntryCover(),
        coverType = libraryItem.sourceItemOrientation.toLibraryGridCoverType(),
        coverContentScale = if (useFitCover) ContentScale.Fit else ContentScale.Crop,
        coverBadgeStart = {
            LibraryItemProgressBadges(libraryItem, displaySettings)
        },
        coverBadgeEnd = {
            LibraryItemOriginBadges(libraryItem, displaySettings)
        },
        onLongClick = { clicks.onLongClick(libraryItem) },
        onClick = { clicks.onClick(libraryItem) },
        continueReadingProgress = libraryItem.progressFraction.takeIf { libraryItem.hasInProgress },
        onClickContinueReading = if (
            onClickContinueReading != null &&
            isContinueReadingAvailable(libraryItem) &&
            (!libraryItem.hasProgressSummary || libraryItem.canContinue)
        ) {
            { onClickContinueReading(libraryItem) }
        } else {
            null
        },
    )
}
