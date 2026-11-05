package mihon.entry.viewer.chrome.ui.wheel

import androidx.compose.ui.geometry.Offset
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Scrolling counted into page turns: a run of scrolling turns one page, and the next turn waits until scrolling has
 * stopped for a moment, which [scope] times. A touchpad scrolls many times to a swipe, and on X11, Xwayland included,
 * the runtime reports a touchpad's scrolling as whole wheel notches, so neither the size nor the count of the steps
 * tells a swipe from a turned wheel; a notch per page, each after a pause, still turns a page each.
 */
class WheelTurns(private val scope: CoroutineScope) {
    private var notches = Offset.Zero
    private var turned = false
    private var runEnd: Job? = null

    /** Forgets the notches counted so far, as when the scrolling went to something else. */
    fun reset() {
        notches = Offset.Zero
    }

    /**
     * Counts [delta], a scroll in notches, and hands what has been counted to [turn], which turns pages for whole
     * notches and returns the notches it used.
     */
    fun onScroll(delta: Offset, turn: (notches: Offset) -> Offset) {
        runEnd?.cancel()
        runEnd = scope.launch {
            delay(RUN_END_MS)
            turned = false
            notches = Offset.Zero
        }
        if (turned) return
        notches += delta
        // One page however far the first count of whole notches reaches
        val used = turn(Offset(notches.x.coerceIn(-1f, 1f), notches.y.coerceIn(-1f, 1f)))
        if (used != Offset.Zero) {
            notches = Offset.Zero
            turned = true
        }
    }
}

// How long after the last scroll a run of scrolling counts as over, as the desktop player waits
private const val RUN_END_MS = 500L
