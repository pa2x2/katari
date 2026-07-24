package eu.kanade.presentation.library.components

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.items
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
    FastScrollLazyColumn(
        modifier = Modifier.fillMaxSize(),
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

        items(
            items = items,
            contentType = { "library_list_item" },
        ) { libraryItem ->
            val useFitCover = libraryItem.sourceItemOrientation == EntryItemOrientation.HORIZONTAL
            EntryListItem(
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
    }
}
