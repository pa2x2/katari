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
import app.katari.presentation.kit.content.BrowseMetrics
import app.katari.presentation.kit.content.entryCoverScale
import app.katari.presentation.kit.list.RowGroupNote
import app.katari.presentation.kit.theme.KatariTheme
import eu.kanade.presentation.entry.components.toGridCoverType
import eu.kanade.presentation.library.components.CommonEntryItemDefaults
import eu.kanade.presentation.library.components.EntryComfortableGridItem
import eu.kanade.tachiyomi.source.entry.EntryItemOrientation
import eu.kanade.tachiyomi.source.entry.EntryType
import tachiyomi.domain.entry.model.Entry
import tachiyomi.domain.entry.model.EntryCover
import tachiyomi.i18n.*
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
        RowGroupNote(stringResource(MR.strings.no_results_found))
        return
    }

    LazyRow(
        contentPadding = PaddingValues(KatariTheme.spacing.s),
        horizontalArrangement = Arrangement.spacedBy(KatariTheme.spacing.xs),
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
    Box(modifier = Modifier.width(BrowseMetrics.searchResultCoverWidth * entryCoverScale())) {
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
