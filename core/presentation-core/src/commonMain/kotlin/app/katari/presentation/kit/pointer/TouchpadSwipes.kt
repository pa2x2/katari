package app.katari.presentation.kit.pointer

import androidx.compose.foundation.gestures.Orientation
import androidx.compose.ui.geometry.Offset
import kotlin.math.abs

/**
 * A touchpad's two-finger swipes read as drags of a control that a scroll sets rather than moves, as a player's volume
 * or seek: the control follows how far the fingers go along one axis and is done when they lift, as under a finger on
 * a touch screen, rather than taking the distance content would scroll and the glide content runs on with.
 *
 * A swipe keeps to the axis its fingers first go along by more than [slop] pixels, as a slanted one would otherwise
 * set two things at once. [onStart] is asked whether the control takes a swipe along that axis; [onMove] then gets the
 * pixels the fingers have gone along it since the swipe began, down and right positive, and [onEnd] comes once, when
 * the fingers lift, the next swipe begins or [end] is called.
 */
class TouchpadSwipes(
    private val slop: Float,
    private val onStart: (axis: Orientation) -> Boolean,
    private val onMove: (axis: Orientation, distance: Float) -> Unit,
    private val onEnd: () -> Unit = {},
) {
    private var swipe: Int? = null
    private var state = SwipeState.Refused
    private var travel = Offset.Zero
    private var axis = Orientation.Vertical

    /**
     * Takes [scroll], a step of a touchpad's scroll, and returns whether it belongs to a swipe the control takes, or
     * may yet take once it goes further, which nothing else should then scroll by.
     */
    fun onStep(scroll: TouchpadScroll): Boolean {
        if (scroll.swipe != swipe) {
            end()
            swipe = scroll.swipe
            travel = Offset.Zero
            // A swipe whose fingers moved over something else, of which only the end or the glide comes here
            state = if (scroll.stage == TouchpadScrollStage.Fingers) SwipeState.Undecided else SwipeState.Refused
        }
        when (scroll.stage) {
            TouchpadScrollStage.Fingers -> move(scroll.distance)
            TouchpadScrollStage.Lifted -> end()
            TouchpadScrollStage.Gliding -> Unit
        }
        return state != SwipeState.Refused
    }

    /** Ends the swipe under way, as when the pointer leaves the control before the fingers lift. */
    fun end() {
        if (state == SwipeState.Taken) onEnd()
        if (state != SwipeState.Refused) state = SwipeState.Over
    }

    private fun move(distance: Offset) {
        travel += distance
        if (state == SwipeState.Undecided) {
            if (travel.getDistance() <= slop) return
            axis = if (abs(travel.y) >= abs(travel.x)) Orientation.Vertical else Orientation.Horizontal
            state = if (onStart(axis)) SwipeState.Taken else SwipeState.Refused
        }
        if (state == SwipeState.Taken) onMove(axis, if (axis == Orientation.Vertical) travel.y else travel.x)
    }

    private enum class SwipeState {
        // Not past the slop yet
        Undecided,
        Taken,

        // Taken and done with, while the rest of the swipe, its glide, still comes
        Over,

        // Along an axis the control doesn't take, or never seen from its start
        Refused,
    }
}
