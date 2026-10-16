package eu.kanade.presentation.browse.components

import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.remember

/**
 * Keeps a search result list that is still at its top there when a source's results arrive and sort it above the
 * source shown first.
 *
 * Lazy lists follow their first visible item by key, so without this the list would slide down after that source and
 * leave the sources that just answered above the screen, where nothing tells the reader they are. A list scrolled away
 * from the top keeps following what the reader is looking at. The position is requested after composition but before
 * the next measure, while the list state still describes the previous order.
 *
 * Takes the first source's id rather than the sources: a reorder keeps the same set of sources, which Compose would
 * take as unchanged and skip this.
 */
@Composable
internal fun KeepSearchResultsAtTop(firstSourceId: Long?, listState: LazyListState) {
    val shown = remember { ShownFirstSource() }

    SideEffect {
        val previousId = shown.id
        shown.id = firstSourceId
        if (previousId == null || previousId == firstSourceId) return@SideEffect
        if (listState.firstVisibleItemIndex == 0 && listState.firstVisibleItemScrollOffset == 0) {
            listState.requestScrollToItem(0)
        }
    }
}

private class ShownFirstSource {
    var id: Long? = null
}
