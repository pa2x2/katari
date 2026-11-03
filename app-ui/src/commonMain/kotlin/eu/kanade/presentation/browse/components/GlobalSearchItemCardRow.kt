package eu.kanade.presentation.browse.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import eu.kanade.presentation.entry.components.toGridCoverType
import eu.kanade.presentation.library.components.CommonEntryItemDefaults
import eu.kanade.presentation.library.components.EntryComfortableGridItem
import eu.kanade.tachiyomi.source.entry.EntryItemOrientation
import eu.kanade.tachiyomi.ui.browse.source.globalsearch.GlobalSearchItem
import mihon.presentation.kit.content.BrowseMetrics
import mihon.presentation.kit.theme.KatariTheme
import tachiyomi.domain.entry.model.EntryCover
import tachiyomi.domain.entry.model.asEntryCover

@Composable
fun GlobalSearchItemCardRow(
    titles: List<GlobalSearchItem>,
    getItem: @Composable (GlobalSearchItem) -> State<GlobalSearchItem>,
    sourceItemOrientation: EntryItemOrientation,
    onClick: (GlobalSearchItem) -> Unit,
    onLongClick: (GlobalSearchItem) -> Unit,
) {
    if (titles.isEmpty()) {
        GlobalSearchEmptyResultItem()
        return
    }

    LazyRow(
        contentPadding = PaddingValues(KatariTheme.spacing.s),
        horizontalArrangement = Arrangement.spacedBy(KatariTheme.spacing.xs),
    ) {
        items(titles) { initialItem ->
            val item by getItem(initialItem)
            GlobalSearchItemCard(
                item = item,
                sourceItemOrientation = sourceItemOrientation,
                onClick = { onClick(item) },
                onLongClick = { onLongClick(item) },
            )
        }
    }
}

@Composable
private fun GlobalSearchItemCard(
    item: GlobalSearchItem,
    sourceItemOrientation: EntryItemOrientation,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
) {
    Box(modifier = Modifier.width(BrowseMetrics.searchResultCoverWidth)) {
        EntryComfortableGridItem(
            title = item.displayTitle,
            titleMaxLines = 3,
            coverData = item.toEntryCover(),
            coverType = sourceItemOrientation.toGridCoverType(),
            coverBadgeEnd = {
                CatalogBadges(isFavorite = item.favorite, entryType = item.entryType)
            },
            coverAlpha = if (item.favorite) {
                CommonEntryItemDefaults.BrowseFavoriteCoverAlpha
            } else {
                1f
            },
            onClick = onClick,
            onLongClick = onLongClick,
        )
    }
}

private fun GlobalSearchItem.toEntryCover(): EntryCover {
    return entry.asEntryCover()
}
