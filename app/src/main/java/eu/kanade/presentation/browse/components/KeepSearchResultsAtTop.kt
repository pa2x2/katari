package eu.kanade.presentation.browse.components

import androidx.compose.foundation.lazy.staggeredgrid.LazyStaggeredGridState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.remember

/**
 * Keeps search results that are still at their top there when a source's results arrive and sort it above the
 * source shown first.
 *
 * Lazy layouts follow their first visible item by key, so without this the results would slide down after that source
 * and leave the sources that just answered above the screen, where nothing tells the reader they are. Results scrolled
 * away from the top keep following what the reader is looking at. The position is requested after composition but
 * before the next measure, while the state still describes the previous order.
 *
 * Takes the first source's id rather than the sources: a reorder keeps the same set of sources, which Compose would
 * take as unchanged and skip this.
 */
@Composable
internal fun KeepSearchResultsAtTop(firstSourceId: Long?, state: LazyStaggeredGridState) {
    val shown = remember { ShownFirstSource() }

    SideEffect {
        val previousId = shown.id
        shown.id = firstSourceId
        if (previousId == null || previousId == firstSourceId) return@SideEffect
        if (state.firstVisibleItemIndex == 0 && state.firstVisibleItemScrollOffset == 0) {
            state.requestScrollToItem(0)
        }
    }
}

private class ShownFirstSource {
    var id: Long? = null
}
