package app.katari.presentation.kit.pointer

import androidx.compose.ui.input.pointer.PointerEvent

/**
 * The two-finger scroll of a touchpad that a scroll event is a step of. [swipe] numbers the scroll: every step has
 * the same from the fingers' first to the end of the glide they leave, and the next scroll has another. [gliding] is
 * set for the steps after the fingers lifted, while the content runs on by itself.
 */
class TouchpadScroll(val swipe: Int, val gliding: Boolean)

/**
 * The touchpad scroll this scroll event is a step of; null for a wheel's event, and where the platform doesn't tell
 * fingers from a wheel. What counts scrolling into steps, as page turns are, tells one swipe from the next by it,
 * whichever of a swipe's events it gets to see.
 */
expect val PointerEvent.touchpadScroll: TouchpadScroll?
