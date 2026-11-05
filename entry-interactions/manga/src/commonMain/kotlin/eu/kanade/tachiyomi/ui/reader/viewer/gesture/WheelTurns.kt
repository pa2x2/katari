package eu.kanade.tachiyomi.ui.reader.viewer.gesture

import androidx.compose.ui.geometry.Offset
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Scrolling counted into page turns. A wheel turns a page for each notch. A touchpad scrolls in fractions of a notch,
 * many to a swipe, so a swipe turns one page and the next turn waits until scrolling has stopped for a moment, which
 * [scope] times.
 */
internal class WheelTurns(private val scope: CoroutineScope) {
    private var notches = Offset.Zero
    private var swipeTurned = false
    private var swipeEnd: Job? = null

    /** Forgets the notches counted so far, as when the scrolling went to something else. */
    fun reset() {
        notches = Offset.Zero
    }

    /**
     * Counts [delta], a scroll in notches, and hands what has been counted to [turn], which turns pages for whole
     * notches and returns the notches it used.
     */
    fun onScroll(delta: Offset, turn: (notches: Offset) -> Offset) {
        val fromTouchpad = delta.x % 1f != 0f || delta.y % 1f != 0f
        if (fromTouchpad) {
            swipeEnd?.cancel()
            swipeEnd = scope.launch {
                delay(SWIPE_END_MS)
                swipeTurned = false
                notches = Offset.Zero
            }
            if (swipeTurned) return
        }
        notches += delta
        val turned = turn(notches)
        if (turned != Offset.Zero) {
            notches -= turned
            if (fromTouchpad) swipeTurned = true
        }
    }
}

// How long after the last scroll a touchpad swipe counts as over, as the desktop player waits
private const val SWIPE_END_MS = 500L
