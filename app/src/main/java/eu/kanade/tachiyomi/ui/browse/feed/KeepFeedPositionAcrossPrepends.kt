package eu.kanade.tachiyomi.ui.browse.feed

import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.remember
import eu.kanade.domain.source.model.FeedItemRef

/**
 * Keeps the feed item the reader is looking at in place when a bridged refresh inserts newer items
 * above it.
 *
 * Lazy lists, grids and pagers follow their first visible item by key only while it stays within a
 * window of nearby indices (about 130 items for lists and pagers, 290 for grids). A refresh that
 * prepends more items than that would leave the viewport at the old index, showing the newest items
 * instead. The position is requested after composition but before the next measure, while the layout
 * still describes the previous items, so the old index is never drawn against the new ones and a
 * scroll in progress cannot override it.
 *
 * [firstVisibleItem] receives the number of non-item rows before the first entry of the measured
 * items and returns the index of the first visible entry together with the scroll offset that keeps
 * it where it is.
 */
@Composable
internal fun KeepFeedPositionAcrossPrepends(
    itemRefs: List<FeedItemRef>,
    leadingItemCount: Int,
    viewportKey: Any?,
    firstVisibleItem: (leadingItemCount: Int) -> Pair<Int, Int>?,
    requestScrollToItem: (index: Int, scrollOffset: Int) -> Unit,
) {
    val measured = remember(viewportKey) { MeasuredFeedItems() }

    SideEffect {
        val previousRefs = measured.itemRefs
        val previousLeadingItemCount = measured.leadingItemCount
        measured.itemRefs = itemRefs
        measured.leadingItemCount = leadingItemCount
        if (previousRefs == null) return@SideEffect
        if (previousRefs === itemRefs && previousLeadingItemCount == leadingItemCount) return@SideEffect

        val (index, scrollOffset) = firstVisibleItem(previousLeadingItemCount) ?: return@SideEffect
        val ref = previousRefs.getOrNull(index - previousLeadingItemCount) ?: return@SideEffect
        val newIndex = itemRefs.indexOf(ref).takeIf { it >= 0 } ?: return@SideEffect
        if (newIndex + leadingItemCount != index) {
            requestScrollToItem(newIndex + leadingItemCount, scrollOffset)
        }
    }
}

internal fun LazyListState.firstVisibleFeedItem(leadingItemCount: Int): Pair<Int, Int>? {
    val visibleItems = layoutInfo.visibleItemsInfo
    val first = visibleItems.firstOrNull { it.index == firstVisibleItemIndex } ?: return null
    val item = visibleItems.firstOrNull { it.index >= leadingItemCount } ?: return null
    return item.index to firstVisibleItemScrollOffset + first.offset - item.offset
}

internal fun LazyGridState.firstVisibleFeedItem(leadingItemCount: Int): Pair<Int, Int>? {
    val visibleItems = layoutInfo.visibleItemsInfo
    val first = visibleItems.firstOrNull { it.index == firstVisibleItemIndex } ?: return null
    val item = visibleItems.firstOrNull { it.index >= leadingItemCount } ?: return null
    return item.index to firstVisibleItemScrollOffset + first.offset.y - item.offset.y
}

private class MeasuredFeedItems {
    var itemRefs: List<FeedItemRef>? = null
    var leadingItemCount: Int = 0
}
