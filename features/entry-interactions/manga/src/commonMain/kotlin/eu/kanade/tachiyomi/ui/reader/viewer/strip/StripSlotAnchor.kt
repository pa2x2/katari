package eu.kanade.tachiyomi.ui.reader.viewer.strip

import androidx.compose.foundation.lazy.LazyListLayoutInfo
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.snapshots.Snapshot
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.layout

/**
 * Keeps what the reader sees still when the slot at the top of the screen changes height, as when the page above the
 * one being read loads and grows from its placeholder to its real height. The strip keeps its place as a distance below
 * the top of the first slot on screen, so the growth would push everything below that slot down and the page being
 * read would jump away. A slot the reader scrolled up into grows upward instead, off screen. A slot reached scrolling
 * down, or on screen when the strip opened or moved to a page, grows downward from where its reading starts.
 */
internal class StripSlotAnchor {
    // The keys of the slots on screen that came in at its top while the strip scrolled up
    private val enteredFromBelow = mutableSetOf<Any>()
    private var firstKey: Any? = null

    /**
     * Follows a measure of the strip. A slot came in from below when it shows above the slot that was first on screen
     * the measure before, which is still on screen; a move to a page shows none of the slots shown before.
     */
    fun onMeasured(info: LazyListLayoutInfo) {
        val visible = info.visibleItemsInfo
        val previousFirst = visible.indexOfFirst { it.key == firstKey }
        enteredFromBelow.retainAll { key -> visible.any { it.key == key } }
        for (index in 0 until previousFirst) enteredFromBelow += visible[index].key
        firstKey = visible.firstOrNull()?.key
    }

    /**
     * Moves [state] along with the slot [key] as it changes height by [delta] pixels, when that slot grows upward.
     * Called after the slot's new height is composed and before the strip measures it, so the move and the new
     * height show in the same frame.
     */
    fun onSlotResized(key: Any, delta: Int, state: LazyListState) {
        if (key !in enteredFromBelow) return
        val info = state.layoutInfo
        val first = info.visibleItemsInfo.firstOrNull()?.takeIf { it.key == key } ?: return
        val scrolledInto = info.viewportStartOffset - first.offset
        if (scrolledInto <= 0) return
        // A raw delta leaves a drag or fling running, which a requested position stops. The list ignores a raw delta
        // it can't scroll by yet, as at the end of the strip before the slot has grown, and then this asks for one
        val canMove = if (delta > 0) state.canScrollForward else state.canScrollBackward
        if (canMove) {
            state.dispatchRawDelta(delta.toFloat())
        } else {
            state.requestScrollToItem(first.index, scrolledInto + delta)
        }
    }
}

/**
 * Tells [anchor] of each measure of the lazy list this modifies, which [state] scrolls, as it happens: a slot that
 * comes in at the top may load in the very next frame.
 */
internal fun Modifier.followedBy(anchor: StripSlotAnchor, state: LazyListState): Modifier =
    layout { measurable, constraints ->
        val placeable = measurable.measure(constraints)
        // The list wrote its layout in the measure that just ran; observing the read would measure again each frame
        Snapshot.withoutReadObservation { anchor.onMeasured(state.layoutInfo) }
        layout(placeable.width, placeable.height) { placeable.place(0, 0) }
    }
