package app.katari.presentation.kit.layout

import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.snapshotFlow
import kotlinx.coroutines.flow.first

/**
 * Scrolls the list so the item at [index] sits in the middle of the viewport, as far as the list's ends allow, so the
 * items before and after it both show, such as the playing episode in the player's episode drawer or the open chapter
 * in a reader's table of contents. The items from [keptFrom] to [index], such as the headers naming it, stay in view:
 * when they fill more than the top half, they start at the viewport's top and [index] sits below the middle.
 *
 * A list that has not been laid out yet is scrolled once its first layout is in.
 */
suspend fun LazyListState.scrollToItemInMiddle(index: Int, keptFrom: Int = index) {
    scrollToItem(keptFrom)
    val layout = snapshotFlow { layoutInfo }.first { it.visibleItemsInfo.isNotEmpty() }
    val item = layout.visibleItemsInfo.firstOrNull { it.index == index } ?: return
    val viewportMiddle = (layout.viewportStartOffset + layout.viewportEndOffset) / 2
    val itemMiddle = item.offset + item.size / 2
    // Only ever back towards the start: scrolling forward would push the kept items above the viewport
    scrollBy((itemMiddle - viewportMiddle).coerceAtMost(0).toFloat())
}
