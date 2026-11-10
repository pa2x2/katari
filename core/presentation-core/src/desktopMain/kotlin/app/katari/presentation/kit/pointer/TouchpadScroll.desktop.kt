package app.katari.presentation.kit.pointer

import androidx.compose.ui.awt.awtEventOrNull
import androidx.compose.ui.input.pointer.PointerEvent
import java.awt.Component
import java.awt.event.MouseWheelEvent

actual val PointerEvent.touchpadScrollStage: TouchpadScrollStage?
    get() = (awtEventOrNull as? TouchpadWheelEvent)?.stage

/**
 * The wheel event a step of a touchpad's two-finger scroll reaches the window as, made by the app from the display's
 * scroll events, as AWT's X11 toolkit only makes whole wheel notches of them. Its rotation is a fraction of a notch,
 * which Compose scrolls by at once rather than sliding to as it does for a wheel's notch.
 *
 * Compose turns a rotation into a distance that grows with the size of what scrolls, so [rotation] can only stand for
 * the fingers' travel in content of one size; [pixels] is the travel itself along the event's axis, for what scrolls
 * by pixels of its own accord, as a web page does.
 */
class TouchpadWheelEvent(
    source: Component,
    whenMillis: Long,
    modifiers: Int,
    x: Int,
    y: Int,
    xOnScreen: Int,
    yOnScreen: Int,
    rotation: Double,
    val pixels: Double,
    val stage: TouchpadScrollStage,
) : MouseWheelEvent(
    source,
    MOUSE_WHEEL,
    whenMillis,
    modifiers,
    x,
    y,
    xOnScreen,
    yOnScreen,
    0,
    false,
    WHEEL_UNIT_SCROLL,
    NOTCH_SCROLL_AMOUNT,
    // No whole notch, which is how Compose knows the rotation for a precise one
    0,
    rotation,
) {
    companion object {
        /** The scroll amount AWT's X11 toolkit gives a wheel's notch, by which Compose multiplies a rotation. */
        const val NOTCH_SCROLL_AMOUNT = 3
    }
}
