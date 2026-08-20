package eu.kanade.presentation.library.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyGridScope
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import eu.kanade.tachiyomi.source.entry.EntryItemOrientation
import tachiyomi.domain.library.model.LibraryItem
import tachiyomi.domain.library.model.LibraryPinnedDisplayStyle
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.i18n.stringResource
import kotlin.math.floor
import androidx.compose.foundation.lazy.grid.items as gridItems

internal const val LIBRARY_PINNED_HEADER_KEY = "library_pinned_header"
internal const val LIBRARY_PINNED_FOOTER_KEY = "library_pinned_footer"

internal fun libraryPinnedItemKey(item: LibraryItem): String = "library_pinned:${item.key}"

internal fun LazyGridScope.libraryPinnedGridItems(
    items: List<LibraryItem>,
    style: LibraryPinnedDisplayStyle,
    columns: Int,
    contentType: String,
    itemContent: @Composable (LibraryItem, Modifier) -> Unit,
) {
    if (items.isEmpty()) return
    if (style == LibraryPinnedDisplayStyle.Shelf) {
        item(
            key = "library_pinned_shelf",
            span = { GridItemSpan(maxLineSpan) },
            contentType = "library_pinned_shelf",
        ) {
            LibraryPinnedGridShelf(items, columns, itemContent)
        }
        return
    }

    item(
        key = LIBRARY_PINNED_HEADER_KEY,
        span = { GridItemSpan(maxLineSpan) },
        contentType = "library_pinned_header",
    ) {
        LibraryPinnedHeading(items.size)
    }
    gridItems(
        items = items,
        key = ::libraryPinnedItemKey,
        span = { libraryItem ->
            GridItemSpan(
                if (libraryItem.sourceItemOrientation == EntryItemOrientation.HORIZONTAL) {
                    minOf(2, maxLineSpan)
                } else {
                    1
                },
            )
        },
        contentType = { contentType },
    ) { libraryItem ->
        itemContent(libraryItem, Modifier)
    }
    item(
        key = LIBRARY_PINNED_FOOTER_KEY,
        span = { GridItemSpan(maxLineSpan) },
        contentType = "library_pinned_footer",
    ) {
        LibraryPinnedFooter()
    }
}

internal fun LazyListScope.libraryPinnedListItems(
    items: List<LibraryItem>,
    style: LibraryPinnedDisplayStyle,
    itemContent: @Composable (LibraryItem, Modifier) -> Unit,
) {
    if (items.isEmpty()) return
    if (style == LibraryPinnedDisplayStyle.Shelf) {
        item(
            key = "library_pinned_shelf",
            contentType = "library_pinned_shelf",
        ) {
            LibraryPinnedListShelf(items, itemContent)
        }
        return
    }

    item(
        key = LIBRARY_PINNED_HEADER_KEY,
        contentType = "library_pinned_header",
    ) {
        LibraryPinnedHeading(items.size)
    }
    items(
        items = items,
        key = ::libraryPinnedItemKey,
        contentType = { "library_pinned_list_item" },
    ) { libraryItem ->
        itemContent(libraryItem, Modifier)
    }
    item(
        key = LIBRARY_PINNED_FOOTER_KEY,
        contentType = "library_pinned_footer",
    ) {
        LibraryPinnedFooter()
    }
}

@Composable
internal fun LibraryPinnedGridShelf(
    items: List<LibraryItem>,
    columns: Int,
    itemContent: @Composable (LibraryItem, Modifier) -> Unit,
) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceContainer,
        shape = MaterialTheme.shapes.large,
    ) {
        Column(modifier = Modifier.padding(vertical = 8.dp)) {
            LibraryPinnedHeading(items.size)
            BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
                val resolvedColumns = resolveColumnCount(columns, maxWidth)
                val itemWidth = gridItemWidth(maxWidth, resolvedColumns)
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(CommonEntryItemDefaults.GridHorizontalSpacer),
                ) {
                    items(items, key = { libraryPinnedItemKey(it) }) { item ->
                        val span = item.gridSpan(resolvedColumns)
                        itemContent(
                            item,
                            Modifier.width(
                                itemWidth * span +
                                    CommonEntryItemDefaults.GridHorizontalSpacer * (span - 1),
                            ),
                        )
                    }
                }
            }
        }
    }
}

@Composable
internal fun LibraryPinnedListShelf(
    items: List<LibraryItem>,
    itemContent: @Composable (LibraryItem, Modifier) -> Unit,
) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceContainer,
        shape = MaterialTheme.shapes.large,
    ) {
        Column(modifier = Modifier.padding(vertical = 4.dp)) {
            LibraryPinnedHeading(items.size)
            LazyRow(
                contentPadding = PaddingValues(horizontal = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(items, key = { libraryPinnedItemKey(it) }) { item ->
                    itemContent(item, Modifier.width(280.dp))
                }
            }
        }
    }
}

@Composable
internal fun LibraryPinnedHeading(count: Int) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Icon(
            imageVector = Icons.Filled.PushPin,
            contentDescription = null,
            modifier = Modifier.size(16.dp),
            tint = MaterialTheme.colorScheme.primary,
        )
        Text(
            text = stringResource(MR.strings.label_pinned),
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary,
        )
        Text(
            text = count.toString(),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
internal fun LibraryPinnedFooter() {
    Spacer(
        modifier = Modifier
            .fillMaxWidth()
            .height(8.dp),
    )
}

private fun LibraryItem.gridSpan(columns: Int): Int {
    return if (sourceItemOrientation == EntryItemOrientation.HORIZONTAL) minOf(2, columns) else 1
}

private fun resolveColumnCount(columns: Int, availableWidth: Dp): Int {
    if (columns > 0) return columns
    val minimumWidth = 128.dp
    val spacing = CommonEntryItemDefaults.GridHorizontalSpacer
    return floor((availableWidth + spacing) / (minimumWidth + spacing)).toInt().coerceAtLeast(1)
}

private fun gridItemWidth(availableWidth: Dp, columns: Int): Dp {
    val spacing = CommonEntryItemDefaults.GridHorizontalSpacer
    return (availableWidth - spacing * (columns - 1)) / columns
}
