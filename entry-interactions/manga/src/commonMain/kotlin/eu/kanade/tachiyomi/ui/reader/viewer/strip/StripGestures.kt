package eu.kanade.tachiyomi.ui.reader.viewer.strip

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculateCentroid
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.input.pointer.util.VelocityTracker
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

/**
 * The long strip's zoom gestures, seen ahead of the strip's own scrolling. Two fingers zoom about their middle and move
 * the strip with them, taking those touches from the scrolling. One finger on a strip zoomed in also pans it across,
 * keeps panning after letting go, and leaves the up and down part to the scrolling.
 *
 * [scrollBy] scrolls the strip by strip pixels; [onZoomStart] is told when two fingers start zooming.
 */
internal fun Modifier.stripZoomGestures(
    zoom: StripZoom,
    scope: CoroutineScope,
    scrollBy: (Float) -> Unit,
    onZoomStart: () -> Unit,
): Modifier = pointerInput(zoom) {
    awaitEachGesture {
        val down = awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
        zoom.stopAnimation()
        val velocity = VelocityTracker()
        velocity.addPosition(down.uptimeMillis, down.position)
        var pinched = false
        while (true) {
            val event = awaitPointerEvent(PointerEventPass.Initial)
            val pressed = event.changes.filter { it.pressed }
            if (pressed.isEmpty()) break
            if (pressed.size > 1) {
                if (!pinched) {
                    pinched = true
                    onZoomStart()
                }
                val centroid = event.calculateCentroid()
                zoom.zoomBy(event.calculateZoom(), centroid, scrollBy)
                val pan = event.calculatePan()
                zoom.panBy(pan.x)
                scrollBy(-pan.y / zoom.scale)
                event.changes.forEach { it.consume() }
            } else if (!pinched && zoom.isZoomedIn) {
                val change = pressed.first()
                if (change.isConsumed) continue
                zoom.panBy(change.positionChange().x)
                velocity.addPosition(change.uptimeMillis, change.position)
            }
        }
        if (!pinched && zoom.isZoomedIn) {
            val velocityX = velocity.calculateVelocity().x
            if (velocityX != 0f) scope.launch { zoom.flingX(velocityX) }
        }
    }
}
