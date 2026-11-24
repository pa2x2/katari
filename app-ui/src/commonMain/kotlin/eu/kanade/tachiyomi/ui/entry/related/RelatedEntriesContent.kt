package eu.kanade.tachiyomi.ui.entry.related

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import app.katari.entry.interactions.child.EntryRelatedEntryGroup
import app.katari.presentation.kit.layout.CoverGrid
import app.katari.presentation.kit.layout.CoverSizing
import app.katari.presentation.kit.layout.RowList
import app.katari.presentation.kit.layout.coverGridCells
import app.katari.presentation.kit.layout.coverListCells
import app.katari.presentation.kit.layout.coverShelfWidth
import app.katari.presentation.kit.list.RowGroupGap
import app.katari.presentation.kit.list.RowGroupHeader
import app.katari.presentation.kit.list.RowGroupItem
import app.katari.presentation.kit.list.RowGroupPosition
import app.katari.presentation.kit.theme.KatariTheme
import eu.kanade.presentation.browse.components.CatalogBadges
import eu.kanade.presentation.entry.components.toGridCoverType
import eu.kanade.presentation.library.components.CommonEntryItemDefaults
import eu.kanade.presentation.library.components.EntryComfortableGridItem
import eu.kanade.presentation.library.components.EntryCompactGridItem
import eu.kanade.presentation.library.components.EntryListItem
import eu.kanade.tachiyomi.source.entry.EntryItemOrientation
import tachiyomi.domain.entry.model.Entry
import tachiyomi.domain.entry.model.EntryCover
import tachiyomi.domain.library.model.LibraryDisplayMode
import tachiyomi.i18n.*
import tachiyomi.presentation.core.i18n.stringResource

/** A source's related entries that aren't split into groups, laid out as the source's catalog is. */
@Composable
internal fun RelatedEntryGrid(
    entries: List<Entry>,
    displayMode: LibraryDisplayMode,
    sizing: CoverSizing,
    sourceItemOrientation: EntryItemOrientation,
    showsTypeBadge: Boolean,
    entryState: @Composable (Entry) -> State<Entry>,
    onEntryClick: (Entry) -> Unit,
) {
    if (displayMode == LibraryDisplayMode.List) {
        RowList(contentPadding = PaddingValues(vertical = KatariTheme.spacing.s)) {
            itemsIndexed(entries, key = { _, entry -> "${entry.type}:${entry.id}" }) { index, initialEntry ->
                val entry by entryState(initialEntry)
                RowGroupItem(position = RowGroupPosition.of(index, entries.size)) {
                    EntryListItem(
                        title = entry.displayTitle,
                        coverData = entry.coverData(),
                        coverAlpha = entry.coverAlpha(),
                        badge = { entry.Badges(showsTypeBadge) },
                        onLongClick = {},
                        onClick = { onEntryClick(entry) },
                    )
                }
            }
        }
        return
    }
    val gridState = rememberLazyGridState()
    CoverGrid(
        state = gridState,
        columns = if (displayMode.isCoverGrid) {
            coverGridCells(sizing.size, wideCovers = sourceItemOrientation == EntryItemOrientation.HORIZONTAL)
        } else {
            coverListCells()
        },
        contentPadding = PaddingValues(KatariTheme.spacing.s),
        verticalArrangement = Arrangement.spacedBy(CommonEntryItemDefaults.GridVerticalSpacer),
        horizontalArrangement = Arrangement.spacedBy(CommonEntryItemDefaults.GridHorizontalSpacer),
        sizing = sizing.takeIf { displayMode.isCoverGrid },
    ) {
        items(
            items = entries,
            key = { "${it.type}:${it.id}" },
        ) { initialEntry ->
            val entry by entryState(initialEntry)
            when (displayMode) {
                LibraryDisplayMode.CompactGrid, LibraryDisplayMode.CoverOnlyGrid -> EntryCompactGridItem(
                    title = entry.displayTitle.takeIf { displayMode == LibraryDisplayMode.CompactGrid },
                    coverData = entry.coverData(),
                    coverType = sourceItemOrientation.toGridCoverType(),
                    coverAlpha = entry.coverAlpha(),
                    coverBadgeEnd = { entry.Badges(showsTypeBadge) },
                    onLongClick = {},
                    onClick = { onEntryClick(entry) },
                )
                else -> RelatedEntryItem(entry, sourceItemOrientation, showsTypeBadge, onClick = {
                    onEntryClick(entry)
                })
            }
        }
    }
}

/**
 * A source's named groups of related entries, such as sequels and side stories, each a shelf of covers under its title,
 * as wide as the source's cover size. An untitled group among titled ones is headed as related entries in general.
 */
@Composable
internal fun RelatedEntryShelves(
    groups: List<EntryRelatedEntryGroup>,
    sizing: CoverSizing,
    sourceItemOrientation: EntryItemOrientation,
    showsTypeBadge: Boolean,
    entryState: @Composable (Entry) -> State<Entry>,
    onEntryClick: (Entry) -> Unit,
) {
    val coverWidth = coverShelfWidth(sizing.size)
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
                        Box(modifier = Modifier.width(coverWidth)) {
                            RelatedEntryItem(
                                entry,
                                sourceItemOrientation,
                                showsTypeBadge,
                                onClick = { onEntryClick(entry) },
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun RelatedEntryItem(
    entry: Entry,
    sourceItemOrientation: EntryItemOrientation,
    showsTypeBadge: Boolean,
    onClick: () -> Unit,
) {
    EntryComfortableGridItem(
        title = entry.displayTitle,
        coverData = entry.coverData(),
        coverType = sourceItemOrientation.toGridCoverType(),
        coverAlpha = entry.coverAlpha(),
        coverBadgeEnd = { entry.Badges(showsTypeBadge) },
        onLongClick = null,
        onClick = onClick,
    )
}

@Composable
private fun Entry.Badges(showsTypeBadge: Boolean) {
    CatalogBadges(isFavorite = favorite, entryType = type.takeIf { showsTypeBadge })
}

private fun Entry.coverData() = EntryCover(
    entryId = id,
    sourceId = source,
    isFavorite = favorite,
    url = thumbnailUrl,
    lastModified = coverLastModified,
)

private fun Entry.coverAlpha() = if (favorite) CommonEntryItemDefaults.BrowseFavoriteCoverAlpha else 1f
