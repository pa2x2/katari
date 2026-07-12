package eu.kanade.presentation.browse.components

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.unit.dp
import androidx.paging.LoadState
import androidx.paging.compose.LazyPagingItems
import eu.kanade.presentation.entry.components.toListCoverType
import eu.kanade.presentation.library.components.CommonEntryItemDefaults
import eu.kanade.presentation.library.components.EntryListItem
import eu.kanade.tachiyomi.source.entry.EntryItemOrientation
import kotlinx.coroutines.flow.StateFlow
import tachiyomi.domain.entry.model.Entry
import tachiyomi.domain.entry.model.EntryCover
import tachiyomi.presentation.core.util.plus

@Composable
fun BrowseSourceList(
    entries: LazyPagingItems<StateFlow<Entry>>,
    contentPadding: PaddingValues,
    sourceItemOrientation: EntryItemOrientation,
    onEntryClick: (Entry) -> Unit,
    onEntryLongClick: (Entry) -> Unit,
) {
    LazyColumn(
        contentPadding = contentPadding + PaddingValues(vertical = 8.dp),
    ) {
        item {
            if (entries.loadState.prepend is LoadState.Loading) {
                BrowseSourceLoadingItem()
            }
        }

        items(
            count = entries.itemCount,
            key = { index -> entries.peek(index)?.value?.id ?: "browse-list-$index" },
        ) { index ->
            val entry by entries[index]?.collectAsState() ?: return@items
            BrowseSourceListItem(
                entry = entry,
                sourceItemOrientation = sourceItemOrientation,
                onClick = { onEntryClick(entry) },
                onLongClick = { onEntryLongClick(entry) },
            )
        }

        item {
            if (entries.loadState.refresh is LoadState.Loading || entries.loadState.append is LoadState.Loading) {
                BrowseSourceLoadingItem()
            }
        }
    }
}

@Composable
private fun BrowseSourceListItem(
    entry: Entry,
    sourceItemOrientation: EntryItemOrientation,
    onClick: () -> Unit = {},
    onLongClick: () -> Unit = onClick,
) {
    EntryListItem(
        title = entry.title,
        coverData = EntryCover(
            entryId = entry.id,
            sourceId = entry.source,
            isFavorite = entry.favorite,
            url = entry.thumbnailUrl,
            lastModified = entry.coverLastModified,
        ),
        coverType = sourceItemOrientation.toListCoverType(),
        coverAlpha = if (entry.favorite) CommonEntryItemDefaults.BrowseFavoriteCoverAlpha else 1f,
        badge = {
            InLibraryBadge(enabled = entry.favorite)
        },
        onLongClick = onLongClick,
        onClick = onClick,
    )
}
