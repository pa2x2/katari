package eu.kanade.presentation.library.components

import androidx.compose.foundation.focusGroup
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
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.Velocity
import eu.kanade.tachiyomi.source.entry.EntryItemOrientation
import mihon.icons.materialsymbols.MaterialSymbols
import mihon.icons.materialsymbols.roundedfilled.Keep
import mihon.presentation.kit.content.BrowseMetrics
import mihon.presentation.kit.content.Icon
import mihon.presentation.kit.content.LibraryMetrics
import mihon.presentation.kit.content.Text
import mihon.presentation.kit.surface.GroupCard
import mihon.presentation.kit.theme.ContentTone
import mihon.presentation.kit.theme.KatariSpacing
import mihon.presentation.kit.theme.KatariTheme
import mihon.presentation.kit.theme.TextRole
import tachiyomi.domain.library.model.LibraryItem
import tachiyomi.domain.library.model.LibraryPinnedDisplayStyle
import tachiyomi.i18n.*
import tachiyomi.presentation.core.i18n.stringResource
import kotlin.math.floor
import androidx.compose.foundation.lazy.grid.items as gridItems

internal const val LIBRARY_PINNED_HEADER_KEY = "library_pinned_header"
internal const val LIBRARY_PINNED_FOOTER_KEY = "library_pinned_footer"

private val SHELF_HORIZONTAL_PADDING = KatariSpacing.s
private const val SHELF_NEXT_ITEM_PEEK_FRACTION = 0.18f

internal fun libraryPinnedItemKey(item: LibraryItem): String = item.key.toString()

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
        val pinnedStateDescription = stringResource(MR.strings.label_pinned)
        itemContent(
            libraryItem,
            Modifier
                .animateItem(fadeInSpec = null, fadeOutSpec = null)
                .semantics { stateDescription = pinnedStateDescription },
        )
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
        contentType = { "library_list_item" },
    ) { libraryItem ->
        val pinnedStateDescription = stringResource(MR.strings.label_pinned)
        itemContent(
            libraryItem,
            Modifier
                .animateItem(fadeInSpec = null, fadeOutSpec = null)
                .semantics { stateDescription = pinnedStateDescription },
        )
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
    GroupCard {
        Column(modifier = Modifier.padding(vertical = KatariTheme.spacing.s)) {
            LibraryPinnedHeading(items.size)
            BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
                val contentWidth = (maxWidth - SHELF_HORIZONTAL_PADDING * 2).coerceAtLeast(KatariTheme.sizes.none)
                val resolvedColumns = resolveColumnCount(columns, contentWidth)
                val hasOverflow = items.sumOf { it.gridSpan(resolvedColumns) } > resolvedColumns
                val itemWidth = gridItemWidth(contentWidth, resolvedColumns, hasOverflow)
                LazyRow(
                    modifier = Modifier.shelfInputIsolation(),
                    contentPadding = PaddingValues(horizontal = SHELF_HORIZONTAL_PADDING),
                    horizontalArrangement = Arrangement.spacedBy(CommonEntryItemDefaults.GridHorizontalSpacer),
                ) {
                    items(items, key = { libraryPinnedItemKey(it) }) { item ->
                        val span = item.gridSpan(resolvedColumns)
                        val pinnedStateDescription = stringResource(MR.strings.label_pinned)
                        itemContent(
                            item,
                            Modifier
                                .width(
                                    itemWidth * span +
                                        CommonEntryItemDefaults.GridHorizontalSpacer * (span - 1),
                                )
                                .animateItem(placementSpec = null)
                                .semantics { stateDescription = pinnedStateDescription },
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
    GroupCard {
        Column(modifier = Modifier.padding(vertical = KatariTheme.spacing.xs)) {
            LibraryPinnedHeading(items.size)
            BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
                val contentWidth = (maxWidth - SHELF_HORIZONTAL_PADDING * 2).coerceAtLeast(KatariTheme.sizes.none)
                val itemWidth = listShelfItemWidth(contentWidth, items.size)
                LazyRow(
                    modifier = Modifier.shelfInputIsolation(),
                    contentPadding = PaddingValues(horizontal = SHELF_HORIZONTAL_PADDING),
                    horizontalArrangement = Arrangement.spacedBy(KatariTheme.spacing.s),
                ) {
                    items(items, key = { libraryPinnedItemKey(it) }) { item ->
                        val pinnedStateDescription = stringResource(MR.strings.label_pinned)
                        itemContent(
                            item,
                            Modifier
                                .width(itemWidth)
                                .animateItem(placementSpec = null)
                                .semantics { stateDescription = pinnedStateDescription },
                        )
                    }
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
            .semantics { heading() }
            .padding(KatariTheme.spacing.s),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(KatariTheme.spacing.s),
    ) {
        Icon(
            imageVector = MaterialSymbols.RoundedFilled.Keep,
            contentDescription = null,
            modifier = Modifier.size(KatariTheme.sizes.iconSmall),
            tone = ContentTone.Primary,
        )
        Text(
            text = stringResource(MR.strings.label_pinned),
            modifier = Modifier.weight(1f),
            role = TextRole.Label,
            tone = ContentTone.Primary,
        )
        Text(
            text = count.toString(),
            role = TextRole.Supporting,
        )
    }
}

@Composable
internal fun LibraryPinnedFooter() {
    Spacer(
        modifier = Modifier
            .fillMaxWidth()
            .height(KatariTheme.spacing.s),
    )
}

private fun LibraryItem.gridSpan(columns: Int): Int {
    return if (sourceItemOrientation == EntryItemOrientation.HORIZONTAL) minOf(2, columns) else 1
}

private fun resolveColumnCount(columns: Int, availableWidth: Dp): Int {
    if (columns > 0) return columns
    val minimumWidth = BrowseMetrics.coverGridMinCellWidth
    val spacing = CommonEntryItemDefaults.GridHorizontalSpacer
    return floor((availableWidth + spacing) / (minimumWidth + spacing)).toInt().coerceAtLeast(1)
}

private fun gridItemWidth(availableWidth: Dp, columns: Int, hasOverflow: Boolean): Dp {
    val spacing = CommonEntryItemDefaults.GridHorizontalSpacer
    return if (hasOverflow) {
        (availableWidth - spacing * columns) / (columns + SHELF_NEXT_ITEM_PEEK_FRACTION)
    } else {
        (availableWidth - spacing * (columns - 1)) / columns
    }
}

private fun listShelfItemWidth(availableWidth: Dp, itemCount: Int): Dp {
    val preferredWidth = LibraryMetrics.pinnedShelfRowWidth
    val spacing = KatariSpacing.s
    val fullyVisibleItems = floor((availableWidth + spacing) / (preferredWidth + spacing))
        .toInt()
        .coerceAtLeast(1)
    return if (itemCount > fullyVisibleItems) {
        (availableWidth - spacing * fullyVisibleItems) /
            (fullyVisibleItems + SHELF_NEXT_ITEM_PEEK_FRACTION)
    } else {
        minOf(
            preferredWidth,
            (availableWidth - spacing * (itemCount - 1)) / itemCount.coerceAtLeast(1),
        )
    }
}

private fun Modifier.shelfInputIsolation(): Modifier {
    return focusProperties {
        onExit = {
            when (requestedFocusDirection) {
                FocusDirection.Left,
                FocusDirection.Right,
                -> cancelFocusChange()
                else -> Unit
            }
        }
    }
        .focusGroup()
        .nestedScroll(ShelfPagerIsolation)
}

private object ShelfPagerIsolation : NestedScrollConnection {
    override fun onPostScroll(
        consumed: Offset,
        available: Offset,
        source: NestedScrollSource,
    ): Offset {
        return if (source == NestedScrollSource.UserInput) Offset(available.x, 0f) else Offset.Zero
    }

    override suspend fun onPostFling(consumed: Velocity, available: Velocity): Velocity {
        return Velocity(available.x, 0f)
    }
}
