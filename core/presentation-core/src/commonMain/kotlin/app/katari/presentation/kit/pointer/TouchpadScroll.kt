package app.katari.presentation.kit.pointer

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.PointerEvent

/**
 * The two-finger scroll of a touchpad that a scroll event is a step of. [swipe] numbers the scroll: every step has
 * the same from the fingers' first to the end of the glide they leave, and the next scroll has another. [distance] is
 * how far the step scrolls content, in pixels, down and right positive as a scroll's delta is.
 *
 * A scroll event's delta is sized for content of the window's size, so what moves under the fingers other than such
 * content, as a zoomed image, moves by [distance]. Content scrolls on by itself once the fingers lift, so what a scroll
 * sets rather than moves, as a volume or a seek, takes its [distance] from the [fingersMoving] steps alone, as
 * [TouchpadSwipes] does.
 */
class TouchpadScroll(val swipe: Int, val stage: TouchpadScrollStage, val distance: Offset) {
    val fingersMoving: Boolean get() = stage == TouchpadScrollStage.Fingers
}

enum class TouchpadScrollStage {
    /** The fingers moved. */
    Fingers,

    /** The fingers lifted, which every swipe ends with; it scrolls by nothing. */
    Lifted,

    /** Content gliding on after a flick, until it slows to a stop or the user stops it. */
    Gliding,
}

/**
 * The touchpad scroll this scroll event is a step of; null for a wheel's event, and where the platform doesn't tell
 * fingers from a wheel. What counts scrolling into steps, as page turns are, tells one swipe from the next by it,
 * whichever of a swipe's events it gets to see.
 */
expect val PointerEvent.touchpadScroll: TouchpadScroll?

/**
 * Whether this scroll event is one the fingers made: a wheel's, or a touchpad's while its fingers move. What a scroll
 * adjusts rather than moves, as a zoom or a size, takes nothing from the steps after the fingers lifted.
 */
val PointerEvent.isFingerOrWheelScroll: Boolean
    get() = touchpadScroll?.fingersMoving ?: true
