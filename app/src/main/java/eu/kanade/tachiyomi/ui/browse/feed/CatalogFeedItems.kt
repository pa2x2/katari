package eu.kanade.tachiyomi.ui.browse.feed

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import eu.kanade.domain.source.model.FeedItemRef
import eu.kanade.presentation.browse.components.CatalogBadges
import eu.kanade.presentation.entry.components.toGridCoverType
import eu.kanade.presentation.entry.components.toListCoverType
import eu.kanade.presentation.library.components.CommonEntryItemDefaults
import eu.kanade.presentation.library.components.EntryComfortableGridItem
import eu.kanade.presentation.library.components.EntryCompactGridItem
import eu.kanade.presentation.library.components.EntryListItem
import eu.kanade.tachiyomi.source.entry.EntryItemOrientation
import mihon.presentation.kit.content.CoverBacking
import mihon.presentation.kit.content.coverFrame
import mihon.presentation.kit.theme.KatariTheme
import tachiyomi.domain.source.model.CatalogListItem
import eu.kanade.presentation.entry.components.EntryCover as CoverType

/** A feed's entries in each display mode, with a placeholder of the same shape while an entry is still loading. */
@Composable
internal fun CatalogFeedListItem(
    ref: FeedItemRef,
    viewModel: FeedViewModel<CatalogListItem>,
    sourceItemOrientation: EntryItemOrientation,
    onItemClick: (CatalogListItem) -> Unit,
    onItemLongClick: (CatalogListItem) -> Unit,
) {
    val item = rememberCatalogItem(ref, viewModel)
    if (item == null) {
        CatalogFeedListItemPlaceholder(sourceItemOrientation.toListCoverType())
        return
    }

    EntryListItem(
        title = item.title,
        coverData = item.cover,
        coverType = sourceItemOrientation.toListCoverType(),
        coverAlpha = browseCoverAlpha(item.favorite),
        badge = {
            CatalogBadges(isFavorite = item.favorite, entryType = item.entryType)
        },
        onLongClick = { onItemLongClick(item) },
        onClick = { onItemClick(item) },
    )
}

@Composable
internal fun CatalogFeedCompactGridItem(
    ref: FeedItemRef,
    viewModel: FeedViewModel<CatalogListItem>,
    sourceItemOrientation: EntryItemOrientation,
    onItemClick: (CatalogListItem) -> Unit,
    onItemLongClick: (CatalogListItem) -> Unit,
) {
    val item = rememberCatalogItem(ref, viewModel)
    if (item == null) {
        CatalogFeedCompactGridItemPlaceholder(sourceItemOrientation.toGridCoverType())
        return
    }

    EntryCompactGridItem(
        title = item.title,
        coverData = item.cover,
        coverType = sourceItemOrientation.toGridCoverType(),
        coverAlpha = browseCoverAlpha(item.favorite),
        coverBadgeStart = {
            CatalogBadges(isFavorite = item.favorite, entryType = item.entryType)
        },
        onLongClick = { onItemLongClick(item) },
        onClick = { onItemClick(item) },
    )
}

@Composable
internal fun CatalogFeedComfortableGridItem(
    ref: FeedItemRef,
    viewModel: FeedViewModel<CatalogListItem>,
    sourceItemOrientation: EntryItemOrientation,
    onItemClick: (CatalogListItem) -> Unit,
    onItemLongClick: (CatalogListItem) -> Unit,
) {
    val item = rememberCatalogItem(ref, viewModel)
    if (item == null) {
        CatalogFeedComfortableGridItemPlaceholder(sourceItemOrientation.toGridCoverType())
        return
    }

    EntryComfortableGridItem(
        title = item.title,
        coverData = item.cover,
        coverType = sourceItemOrientation.toGridCoverType(),
        coverAlpha = browseCoverAlpha(item.favorite),
        coverBadgeStart = {
            CatalogBadges(isFavorite = item.favorite, entryType = item.entryType)
        },
        onLongClick = { onItemLongClick(item) },
        onClick = { onItemClick(item) },
    )
}

@Composable
private fun rememberCatalogItem(
    ref: FeedItemRef,
    viewModel: FeedViewModel<CatalogListItem>,
): CatalogListItem? {
    return remember(ref, viewModel) { viewModel.itemState(ref) }.collectAsState().value
}

@Composable
private fun CatalogFeedListItemPlaceholder(coverType: CoverType) {
    Box(
        modifier = Modifier
            .height(KatariTheme.sizes.artwork)
            .padding(horizontal = KatariTheme.spacing.contentInset, vertical = KatariTheme.spacing.s),
    ) {
        CatalogFeedPlaceholderBlock(
            modifier = Modifier
                .fillMaxHeight()
                .aspectRatio(coverType.ratio),
        )
    }
}

@Composable
private fun CatalogFeedCompactGridItemPlaceholder(coverType: CoverType) {
    CatalogFeedPlaceholderBlock(
        modifier = Modifier
            .padding(KatariTheme.spacing.xs)
            .fillMaxWidth()
            .aspectRatio(coverType.ratio),
    )
}

@Composable
private fun CatalogFeedComfortableGridItemPlaceholder(coverType: CoverType) {
    Box(
        modifier = Modifier.padding(KatariTheme.spacing.xs),
    ) {
        CatalogFeedPlaceholderBlock(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(coverType.ratio),
        )
    }
}

@Composable
private fun CatalogFeedPlaceholderBlock(
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .coverFrame(backing = CoverBacking.Placeholder),
    )
}

private fun browseCoverAlpha(favorite: Boolean): Float {
    return if (favorite) CommonEntryItemDefaults.BrowseFavoriteCoverAlpha else 1f
}
