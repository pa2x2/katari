package eu.kanade.presentation.library.components

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import eu.kanade.presentation.entry.components.toLibraryGridCoverType
import eu.kanade.tachiyomi.source.entry.EntryItemOrientation
import tachiyomi.domain.entry.model.asEntryCover
import tachiyomi.domain.library.model.LibraryItem
import tachiyomi.domain.library.model.LibraryItemKey
import tachiyomi.domain.library.model.LibraryPinnedDisplayStyle

@Composable
internal fun LibraryCompactGrid(
    items: List<LibraryItem>,
    showTitle: Boolean,
    columns: Int,
    contentPadding: PaddingValues,
    selection: Set<LibraryItemKey>,
    onClick: (LibraryItem) -> Unit,
    onLongClick: (LibraryItem) -> Unit,
    onClickContinueReading: ((LibraryItem) -> Unit)?,
    isContinueReadingAvailable: (LibraryItem) -> Boolean,
    searchQuery: String?,
    onGlobalSearchClicked: () -> Unit,
    displaySettings: LibraryDisplaySettings,
) {
    val gridState = rememberLazyGridState()
    val (pinnedItems, regularItems) = items.partition(LibraryItem::isPinned)
    LazyLibraryGrid(
        modifier = Modifier
            .fillMaxSize()
            .libraryPinnedGridDecoration(
                state = gridState,
                items = pinnedItems,
                style = displaySettings.pinnedDisplayStyle,
                contentPadding = contentPadding,
            ),
        state = gridState,
        columns = columns,
        contentPadding = contentPadding,
    ) {
        globalSearchItem(searchQuery, onGlobalSearchClicked)

        libraryPinnedGridItems(
            items = pinnedItems,
            style = displaySettings.pinnedDisplayStyle,
            columns = columns,
            contentType = "library_compact_grid_item",
        ) { libraryItem, modifier ->
            LibraryCompactGridEntry(
                libraryItem = libraryItem,
                modifier = modifier,
                showTitle = showTitle,
                selection = selection,
                onClick = onClick,
                onLongClick = onLongClick,
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
            contentType = { "library_compact_grid_item" },
        ) { libraryItem ->
            LibraryCompactGridEntry(
                libraryItem = libraryItem,
                modifier = if (displaySettings.pinnedDisplayStyle == LibraryPinnedDisplayStyle.TonalGroup) {
                    Modifier.animateItem(fadeInSpec = null, fadeOutSpec = null)
                } else {
                    Modifier.animateItem(placementSpec = null)
                },
                showTitle = showTitle,
                selection = selection,
                onClick = onClick,
                onLongClick = onLongClick,
                onClickContinueReading = onClickContinueReading,
                isContinueReadingAvailable = isContinueReadingAvailable,
                displaySettings = displaySettings,
            )
        }
    }
}

@Composable
private fun LibraryCompactGridEntry(
    libraryItem: LibraryItem,
    showTitle: Boolean,
    selection: Set<LibraryItemKey>,
    onClick: (LibraryItem) -> Unit,
    onLongClick: (LibraryItem) -> Unit,
    onClickContinueReading: ((LibraryItem) -> Unit)?,
    isContinueReadingAvailable: (LibraryItem) -> Boolean,
    displaySettings: LibraryDisplaySettings,
    modifier: Modifier = Modifier,
) {
    val useFitCover = libraryItem.sourceItemOrientation == EntryItemOrientation.HORIZONTAL
    EntryCompactGridItem(
        modifier = modifier,
        isSelected = libraryItem.key in selection,
        title = libraryItem.title.takeIf { showTitle },
        coverData = libraryItem.entry.asEntryCover(),
        coverType = libraryItem.sourceItemOrientation.toLibraryGridCoverType(),
        coverContentScale = if (useFitCover) ContentScale.Fit else ContentScale.Crop,
        coverBackgroundColor = if (useFitCover) {
            MaterialTheme.colorScheme.surfaceContainerHigh
        } else {
            Color.Transparent
        },
        coverBadgeStart = {
            if (displaySettings.downloadBadge) {
                DownloadsBadge(count = libraryItem.downloadCount)
            }
            if (displaySettings.unreadBadge) {
                libraryItem.unconsumedCount?.let { UnreadBadge(count = it) }
            }
        },
        coverBadgeEnd = {
            if (displaySettings.entryTypeBadge) {
                EntryTypeBadge(entryType = libraryItem.entry.type)
            }
            if (displaySettings.localBadge) {
                LocalBadge(isLocal = libraryItem.isLocal)
            }
            if (displaySettings.languageBadge) {
                LanguageBadge(sourceLanguage = libraryItem.sourceLanguage)
            }
        },
        onLongClick = { onLongClick(libraryItem) },
        onClick = { onClick(libraryItem) },
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
