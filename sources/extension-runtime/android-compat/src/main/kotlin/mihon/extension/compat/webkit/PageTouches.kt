package mihon.extension.compat.webkit

import android.view.MotionEvent
import mihon.core.common.web.ExtensionPageTouch
import mihon.core.common.web.ExtensionPageTouch.Phase
import mihon.core.common.web.ExtensionPageTouch.Tool

/**
 * What [event], in its web view's pixels, did to each pointer it changed, as touches of the view's page in CSS pixels:
 * a web view lays its page out at [density] view pixels to a CSS pixel, as Android's does at its default scale. None
 * where the event is no touch, such as a hover or a scroll.
 */
internal fun pageTouches(event: MotionEvent, density: Float): List<ExtensionPageTouch> {
    val changed = when (event.actionMasked) {
        MotionEvent.ACTION_DOWN, MotionEvent.ACTION_POINTER_DOWN -> listOf(event.actionIndex to Phase.PRESSED)
        MotionEvent.ACTION_UP, MotionEvent.ACTION_POINTER_UP -> listOf(event.actionIndex to Phase.RELEASED)
        MotionEvent.ACTION_MOVE -> (0 until event.pointerCount).map { it to Phase.MOVED }
        MotionEvent.ACTION_CANCEL -> (0 until event.pointerCount).map { it to Phase.CANCELLED }
        else -> emptyList()
    }
    return changed.map { (index, phase) -> pageTouch(event, index, phase, density) }
}

/**
 * Android reports a contact's ellipse by its axes and the major axis's angle from vertical; Chromium takes radii across
 * and along the page and a clockwise rotation. The conversion is Chromium's own for Android's web view
 * (`CreateWebTouchPoint` in `ui/events/blink/blink_event_util.cc`), so a page reads the contact it would there.
 */
private fun pageTouch(event: MotionEvent, index: Int, phase: Phase, density: Float): ExtensionPageTouch {
    val majorRadius = event.getTouchMajor(index) / 2 / density
    val minorRadius = event.getTouchMinor(index) / 2 / density
    var degrees = Math.toDegrees(event.getOrientation(index).toDouble()).toFloat()
    // A stylus reports up to half a turn either way, and an ellipse half a turn round is the same ellipse
    if (degrees >= HALF_TURN / 2) {
        degrees -= HALF_TURN
    } else if (degrees < -HALF_TURN / 2) {
        degrees += HALF_TURN
    }
    val upright = degrees >= 0
    return ExtensionPageTouch(
        pointerId = event.getPointerId(index),
        phase = phase,
        tool = when (event.getToolType(index)) {
            MotionEvent.TOOL_TYPE_STYLUS -> Tool.STYLUS
            MotionEvent.TOOL_TYPE_ERASER -> Tool.ERASER
            MotionEvent.TOOL_TYPE_MOUSE -> Tool.MOUSE
            // Dispatched as a touch, a pointer of no known tool is taken for a finger, as obtain's simplest events are
            else -> Tool.FINGER
        },
        x = event.getX(index) / density,
        y = event.getY(index) / density,
        radiusX = if (upright) minorRadius else majorRadius,
        radiusY = if (upright) majorRadius else minorRadius,
        rotation = if (upright) degrees else degrees + HALF_TURN / 2,
        pressure = event.getPressure(index),
    )
}

private const val HALF_TURN = 180f
