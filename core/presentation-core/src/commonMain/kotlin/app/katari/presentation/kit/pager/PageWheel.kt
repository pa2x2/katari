package app.katari.presentation.kit.pager

import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.pager.PagerState
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.pointerInput
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch

/**
 * Scrolling along the pages, by a wheel or a touchpad's two fingers, turns them one at a time as [turns] counts them.
 * Compose's pager would scroll a distance for each notch instead, and as on X11 a touchpad swipe arrives as many notches,
 * one swipe skipped several pages.
 *
 * Each page takes only the scrolling its own content left over, so a row or a list inside a page scrolls first. [turns]
 * belongs to the pager, not the page, so the rest of a swipe still counts as the same swipe once the next page has
 * slid under the pointer. [reverse] is set where the next page lies at the start of the axis, as in a right-to-left
 * layout. [onTurned] is told the page a turn reached once it has slid all the way in; a turn cut short by another
 * turn or a drag is not, as that one reports its own page.
 */
internal fun Modifier.pageWheel(
    state: PagerState,
    turns: WheelTurns,
    orientation: Orientation,
    reverse: Boolean,
    onTurned: (page: Int) -> Unit,
): Modifier = pointerInput(state, turns, orientation, reverse) {
    coroutineScope {
        awaitPointerEventScope {
            while (true) {
                val event = awaitPointerEvent()
                if (event.type != PointerEventType.Scroll) continue
                val change = event.changes.firstOrNull()?.takeUnless { it.isConsumed } ?: continue
                val delta = change.scrollDelta
                val along = if (orientation == Orientation.Vertical) delta.y else delta.x
                if (along == 0f) continue
                change.consume()
                turns.onScroll(Offset(along, 0f), change.uptimeMillis) { notches ->
                    val step = notches.x.toInt()
                    // A turn under way already counts, so a quick next swipe goes on from the page it is turning to
                    val from = if (state.isScrollInProgress) state.targetPage else state.currentPage
                    val target = from + if (reverse) -step else step
                    if (step == 0 || target !in 0 until state.pageCount) return@onScroll Offset.Zero
                    launch {
                        state.animateScrollToPage(target)
                        onTurned(target)
                    }
                    Offset(step.toFloat(), 0f)
                }
            }
        }
    }
}
