package eu.kanade.presentation.browse.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.unit.dp
import androidx.paging.LoadState
import androidx.paging.compose.LazyPagingItems
import eu.kanade.presentation.entry.components.toGridCoverType
import eu.kanade.presentation.library.components.CommonEntryItemDefaults
import eu.kanade.presentation.library.components.EntryComfortableGridItem
import eu.kanade.tachiyomi.source.entry.EntryItemOrientation
import kotlinx.coroutines.flow.StateFlow
import tachiyomi.domain.entry.model.Entry
import tachiyomi.domain.entry.model.EntryCover
import tachiyomi.presentation.core.util.plus

@Composable
fun BrowseSourceComfortableGrid(
    entries: LazyPagingItems<StateFlow<Entry>>,
    columns: GridCells,
    contentPadding: PaddingValues,
    sourceItemOrientation: EntryItemOrientation,
    onEntryClick: (Entry) -> Unit,
    onEntryLongClick: (Entry) -> Unit,
) {
    LazyVerticalGrid(
        columns = columns,
        contentPadding = contentPadding + PaddingValues(8.dp),
        verticalArrangement = Arrangement.spacedBy(CommonEntryItemDefaults.GridVerticalSpacer),
        horizontalArrangement = Arrangement.spacedBy(CommonEntryItemDefaults.GridHorizontalSpacer),
    ) {
        if (entries.loadState.prepend is LoadState.Loading) {
            item(span = { GridItemSpan(maxLineSpan) }) {
                BrowseSourceLoadingItem()
            }
        }

        items(
            count = entries.itemCount,
            key = { index -> entries.peek(index)?.value?.id ?: "browse-comfortable-grid-$index" },
        ) { index ->
            val entry by entries[index]?.collectAsState() ?: return@items
            BrowseSourceComfortableGridItem(
                entry = entry,
                sourceItemOrientation = sourceItemOrientation,
                onClick = { onEntryClick(entry) },
                onLongClick = { onEntryLongClick(entry) },
            )
        }

        if (entries.loadState.refresh is LoadState.Loading || entries.loadState.append is LoadState.Loading) {
            item(span = { GridItemSpan(maxLineSpan) }) {
                BrowseSourceLoadingItem()
            }
        }
    }
}

@Composable
private fun BrowseSourceComfortableGridItem(
    entry: Entry,
    sourceItemOrientation: EntryItemOrientation,
    onClick: () -> Unit = {},
    onLongClick: () -> Unit = onClick,
) {
    EntryComfortableGridItem(
        title = entry.title,
        coverData = EntryCover(
            entryId = entry.id,
            sourceId = entry.source,
            isFavorite = entry.favorite,
            url = entry.thumbnailUrl,
            lastModified = entry.coverLastModified,
        ),
        coverType = sourceItemOrientation.toGridCoverType(),
        coverAlpha = if (entry.favorite) CommonEntryItemDefaults.BrowseFavoriteCoverAlpha else 1f,
        coverBadgeStart = {
            InLibraryBadge(enabled = entry.favorite)
        },
        onLongClick = onLongClick,
        onClick = onClick,
    )
}
