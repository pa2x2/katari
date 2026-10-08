package eu.kanade.tachiyomi.ui.browse.feed

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.snapshotFlow
import eu.kanade.domain.source.model.FeedItemRef

/**
 * Keeps the item the reader was looking at in place when a bridged refresh inserts newer items
 * above it.
 *
 * Lazy lists, grids and pagers follow their first visible item by key only while it stays within a
 * window of nearby indices (about 130 items for lists and pagers, 290 for grids). A refresh that
 * prepends more items than that leaves them at the old index, which then shows the newest items
 * instead of where the reader was.
 *
 * [firstVisibleItem] returns the first visible index and its scroll offset. [leadingItemCount] is the
 * number of non-item rows before the first entry of [itemRefs].
 */
@Composable
internal fun KeepFeedPositionAcrossPrepends(
    itemRefs: List<FeedItemRef>,
    leadingItemCount: Int,
    viewportKey: Any?,
    firstVisibleItem: () -> Pair<Int, Int>,
    scrollToItem: suspend (index: Int, scrollOffset: Int) -> Unit,
) {
    val lastVisible = remember(viewportKey) { LastVisibleFeedItem() }
    val currentFirstVisibleItem by rememberUpdatedState(firstVisibleItem)
    val currentScrollToItem by rememberUpdatedState(scrollToItem)

    LaunchedEffect(viewportKey, itemRefs, leadingItemCount) {
        lastVisible.ref?.let { ref ->
            val index = itemRefs.indexOf(ref)
            val visibleIndex = currentFirstVisibleItem().first - leadingItemCount
            if (index >= 0 && itemRefs.getOrNull(visibleIndex) != ref) {
                currentScrollToItem(index + leadingItemCount, lastVisible.scrollOffset)
            }
        }

        snapshotFlow { currentFirstVisibleItem() }
            .collect { (index, scrollOffset) ->
                val ref = itemRefs.getOrNull(index - leadingItemCount) ?: return@collect
                lastVisible.ref = ref
                lastVisible.scrollOffset = scrollOffset
            }
    }
}

private class LastVisibleFeedItem {
    var ref: FeedItemRef? = null
    var scrollOffset: Int = 0
}
