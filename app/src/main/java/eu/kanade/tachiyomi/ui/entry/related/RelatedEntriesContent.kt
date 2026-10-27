package eu.kanade.tachiyomi.ui.entry.related

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import eu.kanade.presentation.browse.components.CatalogBadges
import eu.kanade.presentation.entry.components.toGridCoverType
import eu.kanade.presentation.library.components.CommonEntryItemDefaults
import eu.kanade.presentation.library.components.EntryComfortableGridItem
import eu.kanade.tachiyomi.source.entry.EntryItemOrientation
import mihon.entry.interactions.child.EntryRelatedEntryGroup
import mihon.presentation.kit.content.BrowseMetrics
import mihon.presentation.kit.layout.RowList
import mihon.presentation.kit.layout.coverGridCells
import mihon.presentation.kit.list.RowGroupGap
import mihon.presentation.kit.list.RowGroupHeader
import mihon.presentation.kit.list.RowGroupItem
import mihon.presentation.kit.list.RowGroupPosition
import mihon.presentation.kit.theme.KatariTheme
import tachiyomi.domain.entry.model.Entry
import tachiyomi.domain.entry.model.EntryCover
import tachiyomi.domain.library.model.CoverSize
import tachiyomi.i18n.*
import tachiyomi.presentation.core.i18n.stringResource

/** A source's related entries that aren't split into groups, as a grid of covers. */
@Composable
internal fun RelatedEntryGrid(
    entries: List<Entry>,
    sourceItemOrientation: EntryItemOrientation,
    entryState: @Composable (Entry) -> State<Entry>,
    onEntryClick: (Entry) -> Unit,
) {
    LazyVerticalGrid(
        columns = coverGridCells(
            size = CoverSize.Auto,
            wideCovers = sourceItemOrientation == EntryItemOrientation.HORIZONTAL,
        ),
        contentPadding = PaddingValues(KatariTheme.spacing.s),
        verticalArrangement = Arrangement.spacedBy(CommonEntryItemDefaults.GridVerticalSpacer),
        horizontalArrangement = Arrangement.spacedBy(CommonEntryItemDefaults.GridHorizontalSpacer),
    ) {
        items(
            items = entries,
            key = { "${it.type}:${it.id}" },
        ) { initialEntry ->
            val entry by entryState(initialEntry)
            RelatedEntryItem(entry, sourceItemOrientation, onClick = { onEntryClick(entry) })
        }
    }
}

/**
 * A source's named groups of related entries, such as sequels and side stories, each a shelf of covers under its title.
 * An untitled group among titled ones is headed as related entries in general.
 */
@Composable
internal fun RelatedEntryShelves(
    groups: List<EntryRelatedEntryGroup>,
    sourceItemOrientation: EntryItemOrientation,
    entryState: @Composable (Entry) -> State<Entry>,
    onEntryClick: (Entry) -> Unit,
) {
    RowList(contentPadding = PaddingValues(vertical = KatariTheme.spacing.s)) {
        itemsIndexed(groups) { index, group ->
            if (index > 0) RowGroupGap()
            RowGroupHeader(text = group.title ?: stringResource(MR.strings.related_entries))
            RowGroupItem(position = RowGroupPosition.Alone) {
                LazyRow(
                    contentPadding = PaddingValues(KatariTheme.spacing.s),
                    horizontalArrangement = Arrangement.spacedBy(KatariTheme.spacing.xs),
                ) {
                    items(
                        items = group.entries,
                        key = { "${it.type}:${it.id}" },
                    ) { initialEntry ->
                        val entry by entryState(initialEntry)
                        Box(modifier = Modifier.width(BrowseMetrics.searchResultCoverWidth)) {
                            RelatedEntryItem(entry, sourceItemOrientation, onClick = { onEntryClick(entry) })
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun RelatedEntryItem(entry: Entry, sourceItemOrientation: EntryItemOrientation, onClick: () -> Unit) {
    EntryComfortableGridItem(
        title = entry.displayTitle,
        coverData = EntryCover(
            entryId = entry.id,
            sourceId = entry.source,
            isFavorite = entry.favorite,
            url = entry.thumbnailUrl,
            lastModified = entry.coverLastModified,
        ),
        coverType = sourceItemOrientation.toGridCoverType(),
        coverAlpha = if (entry.favorite) CommonEntryItemDefaults.BrowseFavoriteCoverAlpha else 1f,
        coverBadgeEnd = {
            CatalogBadges(isFavorite = entry.favorite, entryType = entry.type)
        },
        onLongClick = null,
        onClick = onClick,
    )
}
