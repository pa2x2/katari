package app.katari.presentation.kit.pager

import androidx.compose.ui.geometry.Offset
import app.katari.presentation.kit.pointer.TouchpadScroll

/**
 * Scrolling counted into page turns: a swipe turns one page, as does a turned wheel's notch after a pause.
 *
 * Where the platform says which [TouchpadScroll] an event is a step of, a swipe is the steps the fingers make in one,
 * so the next swipe counts even when it cuts the last one's glide short. The glide itself turns nothing: the fingers
 * turn a page by how far they went, not by how fast they left, and fingers that only brush the touchpad as they leave
 * would otherwise turn a second one. Nor is a scroll the next swipe when it follows the last one's fingers sooner
 * than fingers lift and come down again, in [LIFT_MIN_MS]: that is one swipe reported in pieces.
 *
 * Where it doesn't, as for a wheel, and for a touchpad that X11 hands over as whole notches, many to a swipe, neither
 * the size nor the count of the steps tells a swipe from a turned wheel, and nothing says when fingers lift. The gaps
 * between steps tell swipes apart. Within a swipe steps come a few tens of milliseconds apart, and further apart as
 * the fingers slow down at its end; the next swipe starts after a gap and comes quickly again at once. So a run of
 * steps turns one page; after a gap of [LIFT_MIN_MS] the first quick step starts the next run, and a step after
 * [RUN_BREAK_MS], which no swipe pauses for, starts one by itself.
 */
class WheelTurns {
    private var notches = Offset.Zero
    private var turned = false
    private var lastStepAt: Long? = null

    // The touchpad scroll the last step of fingers was of
    private var swipe: Int? = null

    // A gap long enough for the fingers to have lifted came, and no quick step since
    private var liftSeen = false

    /** Forgets the notches counted so far, as when the scrolling went to something else. */
    fun reset() {
        notches = Offset.Zero
    }

    /**
     * Counts [delta], a scroll in notches made at [uptimeMillis] as a step of [touchpad]'s scroll, null when that
     * isn't known, and hands what has been counted to [turn], which turns pages for whole notches and returns the
     * notches it used.
     */
    fun onScroll(
        delta: Offset,
        uptimeMillis: Long,
        touchpad: TouchpadScroll?,
        turn: (notches: Offset) -> Offset,
    ) {
        if (touchpad?.gliding == true) return
        val gap = lastStepAt?.let { uptimeMillis - it }
        lastStepAt = uptimeMillis
        when {
            touchpad != null -> if (touchpad.swipe != swipe) {
                swipe = touchpad.swipe
                if (gap == null || gap >= LIFT_MIN_MS) startRun()
            }
            gap == null || gap > RUN_BREAK_MS -> startRun()
            gap >= LIFT_MIN_MS -> liftSeen = true
            liftSeen && gap <= QUICK_STEP_MS -> startRun()
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

    private fun startRun() {
        turned = false
        notches = Offset.Zero
        liftSeen = false
    }
}

// No swipe pauses this long between steps; a wheel turned notch by notch at this pace turns a page each
private const val RUN_BREAK_MS = 300L

// Lifting the fingers and putting them down again for the next swipe takes at least this long
private const val LIFT_MIN_MS = 90L

// Steps this close come from fingers moving in earnest, as a new swipe's do from its start
private const val QUICK_STEP_MS = 50L
