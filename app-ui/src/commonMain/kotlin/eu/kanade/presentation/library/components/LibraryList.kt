package eu.kanade.presentation.library.components

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import app.katari.presentation.kit.layout.RowList
import app.katari.presentation.kit.list.RowGroupItem
import app.katari.presentation.kit.list.RowGroupPosition
import app.katari.presentation.kit.theme.KatariTheme
import tachiyomi.domain.entry.model.asEntryCover
import tachiyomi.domain.library.model.LibraryItem
import tachiyomi.domain.library.model.LibraryItemKey
import tachiyomi.domain.library.model.LibraryPinnedDisplayStyle
import tachiyomi.presentation.core.util.plus

@Composable
internal fun LibraryList(
    items: List<LibraryItem>,
    contentPadding: PaddingValues,
    selection: Set<LibraryItemKey>,
    clicks: LibraryItemClicks,
    onClickContinueReading: ((LibraryItem) -> Unit)?,
    isContinueReadingAvailable: (LibraryItem) -> Boolean,
    displaySettings: LibraryDisplaySettings,
    scrollToTopTarget: LibraryScrollToTopTarget?,
) {
    val listState = rememberLazyListState()
    val (pinnedItems, regularItems) = items.partition(LibraryItem::isPinned)
    BindScrollToTop(scrollToTopTarget, listState) { listState.animateScrollToItem(0) }
    RowList(
        fastScroll = true,
        modifier = Modifier.fillMaxSize(),
        state = listState,
        contentPadding = contentPadding + PaddingValues(vertical = KatariTheme.spacing.s),
    ) {
        libraryPinnedListItems(
            items = pinnedItems,
            style = displaySettings.pinnedDisplayStyle,
        ) { libraryItem, modifier ->
            LibraryListEntry(
                libraryItem = libraryItem,
                modifier = modifier,
                selection = selection,
                clicks = clicks,
                onClickContinueReading = onClickContinueReading,
                isContinueReadingAvailable = isContinueReadingAvailable,
                displaySettings = displaySettings,
            )
        }

        itemsIndexed(
            items = regularItems,
            key = { _, item -> item.key.toString() },
            contentType = { _, _ -> "library_list_item" },
        ) { index, libraryItem ->
            RowGroupItem(
                position = RowGroupPosition.of(index, regularItems.size),
                modifier = if (displaySettings.pinnedDisplayStyle == LibraryPinnedDisplayStyle.TonalGroup) {
                    Modifier.animateItem(fadeInSpec = null, fadeOutSpec = null)
                } else {
                    Modifier.animateItem(placementSpec = null)
                },
            ) {
                LibraryListEntry(
                    libraryItem = libraryItem,
                    selection = selection,
                    clicks = clicks,
                    onClickContinueReading = onClickContinueReading,
                    isContinueReadingAvailable = isContinueReadingAvailable,
                    displaySettings = displaySettings,
                )
            }
        }
    }
}

@Composable
internal fun LibraryListEntry(
    libraryItem: LibraryItem,
    selection: Set<LibraryItemKey>,
    clicks: LibraryItemClicks,
    onClickContinueReading: ((LibraryItem) -> Unit)?,
    isContinueReadingAvailable: (LibraryItem) -> Boolean,
    displaySettings: LibraryDisplaySettings,
    modifier: Modifier = Modifier,
) {
    EntryListItem(
        modifier = modifier.libraryItemPointer(libraryItem, selection, clicks),
        isSelected = libraryItem.key in selection,
        selectionMode = selection.isNotEmpty(),
        title = libraryItem.title,
        coverData = libraryItem.entry.asEntryCover(),
        badge = {
            LibraryItemProgressBadges(libraryItem, displaySettings)
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
