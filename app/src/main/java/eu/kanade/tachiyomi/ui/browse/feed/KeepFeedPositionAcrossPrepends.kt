package eu.kanade.tachiyomi.ui.browse.feed

import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.remember
import eu.kanade.tachiyomi.ui.browse.feed.timeline.FeedRow

/**
 * Keeps the feed item the reader is looking at in place when items are inserted above it: by a refresh, or by a gap
 * loading above the viewport.
 *
 * Lazy lists, grids and pagers follow their first visible item by key only while it stays within a window of nearby
 * indices (about 130 items for lists and pagers, 290 for grids). A refresh that inserts more items than that would
 * leave the viewport at the old index, showing other items instead. The position is requested after composition but
 * before the next measure, while the layout still describes the previous rows, so the old index is never drawn
 * against the new ones and a scroll in progress cannot override it.
 *
 * [firstVisibleItem] receives which indices of the measured rows are items (gap and divider rows move with the
 * items around them, so they make poor anchors) and returns the index of the first visible item together with the
 * scroll offset that keeps it where it is.
 */
@Composable
internal fun KeepFeedPositionAcrossPrepends(
    rows: List<FeedRow>,
    viewportKey: Any?,
    firstVisibleItem: (isItem: (index: Int) -> Boolean) -> Pair<Int, Int>?,
    requestScrollToItem: (index: Int, scrollOffset: Int) -> Unit,
) {
    val measured = remember(viewportKey) { MeasuredFeedRows() }

    SideEffect {
        val previousRows = measured.rows
        measured.rows = rows
        if (previousRows == null || previousRows === rows) return@SideEffect

        val (index, scrollOffset) = firstVisibleItem { previousRows.getOrNull(it) is FeedRow.Item } ?: return@SideEffect
        val key = previousRows[index].key
        val newIndex = rows.indexOfFirst { it.key == key }.takeIf { it >= 0 } ?: return@SideEffect
        if (newIndex != index) {
            requestScrollToItem(newIndex, scrollOffset)
        }
    }
}

internal fun LazyListState.firstVisibleFeedItem(isItem: (index: Int) -> Boolean): Pair<Int, Int>? {
    val visibleItems = layoutInfo.visibleItemsInfo
    val first = visibleItems.firstOrNull { it.index == firstVisibleItemIndex } ?: return null
    val item = visibleItems.firstOrNull { isItem(it.index) } ?: return null
    return item.index to firstVisibleItemScrollOffset + first.offset - item.offset
}

internal fun LazyGridState.firstVisibleFeedItem(isItem: (index: Int) -> Boolean): Pair<Int, Int>? {
    val visibleItems = layoutInfo.visibleItemsInfo
    val first = visibleItems.firstOrNull { it.index == firstVisibleItemIndex } ?: return null
    val item = visibleItems.firstOrNull { isItem(it.index) } ?: return null
    return item.index to firstVisibleItemScrollOffset + first.offset.y - item.offset.y
}

private class MeasuredFeedRows {
    var rows: List<FeedRow>? = null
}
