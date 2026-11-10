package app.katari.presentation.kit.pointer

import androidx.compose.ui.input.pointer.PointerEvent

/**
 * Where a scroll event lies in a touchpad's two-finger scroll: the fingers [Start] it, [Move] on, and once they lift
 * after a flick the scrolling runs out as a [Glide].
 */
enum class TouchpadScrollStage { Start, Move, Glide }

/**
 * The stage of the touchpad scroll this scroll event belongs to; null for a wheel's event, and where the platform
 * doesn't tell fingers from a wheel. What counts scrolling into steps, as page turns are, takes a [Start] for the
 * next swipe however soon it follows the last one's glide.
 */
expect val PointerEvent.touchpadScrollStage: TouchpadScrollStage?
