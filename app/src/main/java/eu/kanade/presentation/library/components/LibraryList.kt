package eu.kanade.presentation.library.components

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import eu.kanade.presentation.entry.components.toListCoverType
import eu.kanade.tachiyomi.source.entry.EntryItemOrientation
import tachiyomi.domain.entry.model.asEntryCover
import tachiyomi.domain.library.model.LibraryItem
import tachiyomi.domain.library.model.LibraryItemKey
import tachiyomi.domain.library.model.LibraryPinnedDisplayStyle
import tachiyomi.presentation.core.components.FastScrollLazyColumn
import tachiyomi.presentation.core.util.plus

@Composable
internal fun LibraryList(
    items: List<LibraryItem>,
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
    val listState = rememberLazyListState()
    val (pinnedItems, regularItems) = items.partition(LibraryItem::isPinned)
    FastScrollLazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .libraryPinnedListDecoration(
                state = listState,
                items = pinnedItems,
                style = displaySettings.pinnedDisplayStyle,
                contentPadding = contentPadding,
            ),
        state = listState,
        contentPadding = contentPadding + PaddingValues(vertical = 8.dp),
    ) {
        item {
            if (!searchQuery.isNullOrEmpty()) {
                GlobalSearchItem(
                    modifier = Modifier.fillMaxWidth(),
                    searchQuery = searchQuery,
                    onClick = onGlobalSearchClicked,
                )
            }
        }

        libraryPinnedListItems(
            items = pinnedItems,
            style = displaySettings.pinnedDisplayStyle,
        ) { libraryItem, modifier ->
            LibraryListEntry(
                libraryItem = libraryItem,
                modifier = modifier,
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
            contentType = { "library_list_item" },
        ) { libraryItem ->
            LibraryListEntry(
                libraryItem = libraryItem,
                modifier = if (displaySettings.pinnedDisplayStyle == LibraryPinnedDisplayStyle.TonalGroup) {
                    Modifier.animateItem(fadeInSpec = null, fadeOutSpec = null)
                } else {
                    Modifier.animateItem(placementSpec = null)
                },
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
private fun LibraryListEntry(
    libraryItem: LibraryItem,
    selection: Set<LibraryItemKey>,
    onClick: (LibraryItem) -> Unit,
    onLongClick: (LibraryItem) -> Unit,
    onClickContinueReading: ((LibraryItem) -> Unit)?,
    isContinueReadingAvailable: (LibraryItem) -> Boolean,
    displaySettings: LibraryDisplaySettings,
    modifier: Modifier = Modifier,
) {
    val useFitCover = libraryItem.sourceItemOrientation == EntryItemOrientation.HORIZONTAL
    EntryListItem(
        modifier = modifier,
        isSelected = libraryItem.key in selection,
        title = libraryItem.title,
        coverData = libraryItem.entry.asEntryCover(),
        coverType = libraryItem.sourceItemOrientation.toListCoverType(),
        coverContentScale = if (useFitCover) ContentScale.Fit else ContentScale.Crop,
        coverBackgroundColor = if (useFitCover) {
            MaterialTheme.colorScheme.surfaceContainerHigh
        } else {
            Color.Transparent
        },
        badge = {
            if (displaySettings.downloadBadge) {
                DownloadsBadge(count = libraryItem.downloadCount)
            }
            if (displaySettings.unreadBadge) {
                libraryItem.unconsumedCount?.let { UnreadBadge(count = it) }
            }
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
