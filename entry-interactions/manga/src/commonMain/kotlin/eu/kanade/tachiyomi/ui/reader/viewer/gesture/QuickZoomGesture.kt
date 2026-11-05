package eu.kanade.tachiyomi.ui.reader.viewer.gesture

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.isSpecified
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import kotlin.math.abs
import kotlin.math.exp

/**
 * One-finger zoom: a tap, then a second press held and dragged, zooms in while the finger moves down and out while it
 * moves up, about the point first pressed. Until the drag passes the touch slop nothing is taken, so a plain double
 * tap still reaches the tap detector; once quick zoom starts, its moves are consumed so nothing scrolls meanwhile.
 * [enabled] is read at each press; [onStart] is told when a quick zoom begins and [onZoom] each step's factor. A new
 * [key] restarts the gesture with the callbacks given then.
 */
internal fun Modifier.quickZoomGesture(
    key: Any?,
    enabled: () -> Boolean,
    onStart: (focus: Offset) -> Unit,
    onZoom: (factor: Float, focus: Offset) -> Unit,
): Modifier = pointerInput(key) {
    var lastUpMillis = Long.MIN_VALUE
    var lastUpPosition = Offset.Unspecified
    awaitEachGesture {
        val down = awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
        val isSecondPress = lastUpPosition.isSpecified &&
            down.uptimeMillis - lastUpMillis <= viewConfiguration.doubleTapTimeoutMillis &&
            (down.position - lastUpPosition).getDistance() <= viewConfiguration.touchSlop * DOUBLE_TAP_SLOP_TOUCH_SLOPS
        val focus = down.position
        var zooming = false
        var moved = false
        var dragged = 0f
        val pixelsPerZoomStep = size.height / ZOOM_STEPS_PER_HEIGHT
        while (true) {
            val event = awaitPointerEvent(PointerEventPass.Initial)
            val change = event.changes.firstOrNull { it.id == down.id } ?: break
            if (!change.pressed) {
                lastUpMillis = change.uptimeMillis
                // Only a tap starts a double tap; the end of a drag or of a second press does not
                lastUpPosition = if (isSecondPress || moved) Offset.Unspecified else change.position
                break
            }
            if ((change.position - down.position).getDistance() > viewConfiguration.touchSlop) moved = true
            if (event.changes.count { it.pressed } > 1) {
                lastUpPosition = Offset.Unspecified
                break
            }
            if (!isSecondPress || !enabled()) continue
            val dy = change.positionChange().y
            dragged += dy
            if (!zooming && abs(dragged) > viewConfiguration.touchSlop) {
                zooming = true
                onStart(focus)
            }
            if (zooming) {
                if (dy != 0f) onZoom(exp(dy / pixelsPerZoomStep), focus)
                event.changes.forEach { it.consume() }
            }
        }
    }
}

// Android's double tap slop is a few touch slops; the second press may land that far from the first
private const val DOUBLE_TAP_SLOP_TOUCH_SLOPS = 4

// Dragging a quarter of the height changes the zoom by a factor of e
private const val ZOOM_STEPS_PER_HEIGHT = 4f
