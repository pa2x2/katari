package eu.kanade.presentation.browse.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import eu.kanade.presentation.entry.components.toGridCoverType
import eu.kanade.presentation.library.components.CommonEntryItemDefaults
import eu.kanade.presentation.library.components.EntryComfortableGridItem
import eu.kanade.tachiyomi.source.entry.EntryItemOrientation
import eu.kanade.tachiyomi.ui.browse.source.globalsearch.GlobalSearchItem
import tachiyomi.domain.entry.model.EntryCover
import tachiyomi.domain.entry.model.asEntryCover
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.components.material.padding
import tachiyomi.presentation.core.i18n.stringResource

@Composable
fun GlobalSearchItemCardRow(
    titles: List<GlobalSearchItem>,
    getItem: @Composable (GlobalSearchItem) -> State<GlobalSearchItem>,
    sourceItemOrientation: EntryItemOrientation,
    onClick: (GlobalSearchItem) -> Unit,
    onLongClick: (GlobalSearchItem) -> Unit,
) {
    if (titles.isEmpty()) {
        EmptyResultItem()
        return
    }

    LazyRow(
        contentPadding = PaddingValues(MaterialTheme.padding.small),
        horizontalArrangement = Arrangement.spacedBy(MaterialTheme.padding.extraSmall),
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
    Box(modifier = Modifier.width(96.dp)) {
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

@Composable
private fun EmptyResultItem() {
    Text(
        text = stringResource(MR.strings.no_results_found),
        modifier = Modifier
            .padding(
                horizontal = MaterialTheme.padding.medium,
                vertical = MaterialTheme.padding.small,
            ),
    )
}
