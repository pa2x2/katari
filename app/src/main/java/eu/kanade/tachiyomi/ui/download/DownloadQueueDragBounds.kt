package eu.kanade.tachiyomi.ui.download

import androidx.compose.foundation.MutatePriority
import androidx.compose.foundation.gestures.ScrollScope
import androidx.compose.foundation.gestures.ScrollableState
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.onGloballyPositioned
import mihon.entry.interactions.download.EntryDownloadQueueGroup

/*
 * A dragged download can only be dropped within its source's card, so neither the drag's auto-scroll nor the row
 * itself may carry it past the card's edges.
 */

internal class ListCoordinatesHolder {
    var value: LayoutCoordinates? = null
}

/** [lazyListState], with every scroll step passed through [limit] first. */
internal class LimitedScrollableState(
    private val lazyListState: LazyListState,
    private val limit: (Float) -> Float,
) : ScrollableState by lazyListState {
    override suspend fun scroll(scrollPriority: MutatePriority, block: suspend ScrollScope.() -> Unit) {
        lazyListState.scroll(scrollPriority) {
            val scope = this
            object : ScrollScope {
                override fun scrollBy(pixels: Float) = scope.scrollBy(limit(pixels))
            }.block()
        }
    }
}

/**
 * Limits [pixels] so the list scrolls no further than the end of [group] reaching the bottom, or its header reaching the
 * top, while that edge is on screen.
 */
internal fun LazyListState.limitScrollToGroup(group: EntryDownloadQueueGroup, pixels: Float): Float {
    val items = layoutInfo.visibleItemsInfo
    return if (pixels > 0) {
        val last = items.firstOrNull { it.key == group.items.last().key } ?: return pixels
        val contentEnd = layoutInfo.viewportEndOffset - layoutInfo.afterContentPadding
        pixels.coerceAtMost((last.offset + last.size - contentEnd).coerceAtLeast(0).toFloat())
    } else {
        val header = items.firstOrNull { it.key == group.key } ?: return pixels
        pixels.coerceAtLeast(header.offset.coerceAtMost(0).toFloat())
    }
}

/** Stops a dragged download between its source's header and the card's last slot. */
@Composable
internal fun Modifier.keepInGroup(
    lazyListState: LazyListState,
    listCoordinates: ListCoordinatesHolder,
    groupKey: String,
    lastItemKey: String,
): Modifier {
    var correction by remember { mutableFloatStateOf(0f) }
    return this
        // Before the correction layer, so it sees where the drag put the row
        .onGloballyPositioned { coordinates ->
            val list = listCoordinates.value?.takeIf { it.isAttached } ?: return@onGloballyPositioned
            val top = list.localPositionOf(coordinates, Offset.Zero).y
            val layoutInfo = lazyListState.layoutInfo
            val itemStart = layoutInfo.beforeContentPadding
            val header = layoutInfo.visibleItemsInfo.firstOrNull { it.key == groupKey }
            val last = layoutInfo.visibleItemsInfo.firstOrNull { it.key == lastItemKey }
            val minTop = header?.let { (itemStart + it.offset + it.size).toFloat() } ?: Float.NEGATIVE_INFINITY
            val maxTop = last?.let { (itemStart + it.offset + it.size - coordinates.size.height).toFloat() }
                ?: Float.POSITIVE_INFINITY
            correction = top.coerceIn(minTop, maxOf(minTop, maxTop)) - top
        }
        .graphicsLayer { translationY = correction }
}
