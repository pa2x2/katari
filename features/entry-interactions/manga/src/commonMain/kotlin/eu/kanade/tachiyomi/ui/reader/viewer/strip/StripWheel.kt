package eu.kanade.tachiyomi.ui.reader.viewer.strip

import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.isCtrlPressed
import androidx.compose.ui.input.pointer.pointerInput
import app.katari.presentation.kit.pointer.isFingerOrWheelScroll
import app.katari.presentation.kit.pointer.touchpadScroll
import kotlin.math.pow

/**
 * Scrolling over the long strip that its list leaves alone, seen ahead of it: with Ctrl held the wheel zooms the strip
 * about the pointer, and sideways scrolling, as a touchpad's, moves a zoomed strip across, a share of the strip's width
 * a wheel's notch and as far as content scrolls under a touchpad. Scrolling up and down is the list's own. [scrollBy]
 * scrolls the strip by strip pixels, as zooming keeps the point under the pointer still.
 */
internal fun Modifier.stripWheel(zoom: StripZoom, scrollBy: (Float) -> Unit): Modifier = pointerInput(zoom) {
    awaitPointerEventScope {
        while (true) {
            val event = awaitPointerEvent(PointerEventPass.Initial)
            if (event.type != PointerEventType.Scroll) continue
            val change = event.changes.firstOrNull() ?: continue
            val delta = change.scrollDelta
            val touchpad = event.touchpadScroll
            when {
                event.keyboardModifiers.isCtrlPressed -> {
                    // The fingers zoom as far as they go, not on with the glide they leave
                    if (delta.y != 0f && event.isFingerOrWheelScroll) {
                        zoom.zoomBy(ZOOM_STEP.pow(-delta.y), change.position, scrollBy)
                    }
                    change.consume()
                }
                delta.x != 0f && zoom.isZoomedIn -> {
                    zoom.panBy(touchpad?.let { -it.distance.x } ?: (-delta.x * size.width * PAN_SHARE))
                    if (delta.y == 0f) change.consume()
                }
            }
        }
    }
}

// The share of the strip's width a wheel's notch moves a zoomed strip across
private const val PAN_SHARE = 0.1f
private const val ZOOM_STEP = 1.1f
