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
import eu.kanade.tachiyomi.source.entry.EntryType
import tachiyomi.domain.entry.model.Entry
import tachiyomi.domain.entry.model.EntryCover
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.components.material.padding
import tachiyomi.presentation.core.i18n.stringResource

@Composable
fun GlobalSearchCardRow(
    titles: List<Entry>,
    getEntryState: @Composable (Entry) -> State<Entry>,
    sourceItemOrientation: EntryItemOrientation,
    onClick: (Entry) -> Unit,
    onLongClick: (Entry) -> Unit,
) {
    if (titles.isEmpty()) {
        EmptyResultItem()
        return
    }

    LazyRow(
        contentPadding = PaddingValues(MaterialTheme.padding.small),
        horizontalArrangement = Arrangement.spacedBy(MaterialTheme.padding.extraSmall),
    ) {
        items(titles) {
            val title by getEntryState(it)
            EntryItem(
                title = title.title,
                cover = EntryCover(
                    entryId = title.id,
                    sourceId = title.source,
                    isFavorite = title.favorite,
                    url = title.thumbnailUrl,
                    lastModified = title.coverLastModified,
                ),
                sourceItemOrientation = sourceItemOrientation,
                isFavorite = title.favorite,
                entryType = title.type,
                onClick = { onClick(title) },
                onLongClick = { onLongClick(title) },
            )
        }
    }
}

@Composable
private fun EntryItem(
    title: String,
    cover: EntryCover,
    sourceItemOrientation: EntryItemOrientation,
    isFavorite: Boolean,
    entryType: EntryType,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
) {
    Box(modifier = Modifier.width(96.dp)) {
        EntryComfortableGridItem(
            title = title,
            titleMaxLines = 3,
            coverData = cover,
            coverType = sourceItemOrientation.toGridCoverType(),
            coverBadgeEnd = {
                CatalogBadges(isFavorite = isFavorite, entryType = entryType)
            },
            coverAlpha = if (isFavorite) CommonEntryItemDefaults.BrowseFavoriteCoverAlpha else 1f,
            onClick = onClick,
            onLongClick = onLongClick,
        )
    }
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
