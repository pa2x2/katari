package eu.kanade.tachiyomi.ui.reader.viewer.pager

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculateCentroid
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChanged
import androidx.compose.ui.input.pointer.util.VelocityTracker
import androidx.compose.ui.input.pointer.util.addPointerInputChange
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

/**
 * Touch on one page: a pinch zooms it, and a drag pans it while zoomed and can move that way. Any other drag is left to
 * the pager, which turns the page. Taps are the slot's, which knows the tap zones and the pages in it.
 */
internal fun Modifier.pagerPageGestures(
    zoom: PagerPageZoom,
    scope: CoroutineScope,
    onZoomed: () -> Unit,
): Modifier = this
    .pointerInput(zoom) {
        awaitEachGesture {
            awaitFirstDown(requireUnconsumed = false)
            val velocity = VelocityTracker()
            var dragged = Offset.Zero
            // Decided once the pointer moves past the slop: whether this gesture pans the page or turns it
            var pansPage: Boolean? = null
            do {
                val event = awaitPointerEvent()
                val pointers = event.changes.count { it.pressed }
                if (pointers > 1) {
                    pansPage = true
                    val zoomChange = event.calculateZoom()
                    if (zoomChange != 1f) {
                        zoom.zoomBy(zoomChange, event.calculateCentroid())
                        onZoomed()
                    }
                    zoom.panBy(event.calculatePan())
                    event.changes.forEach { if (it.positionChanged()) it.consume() }
                } else {
                    val pan = event.calculatePan()
                    if (pansPage == null) {
                        dragged += pan
                        if (dragged.getDistance() > viewConfiguration.touchSlop) {
                            pansPage = zoom.isZoomed && zoom.canPan(dragged)
                        }
                    }
                    if (pansPage == true) {
                        zoom.panBy(pan)
                        event.changes.forEach {
                            velocity.addPointerInputChange(it)
                            if (it.positionChanged()) it.consume()
                        }
                    }
                }
            } while (event.changes.any { it.pressed })
            if (pansPage == true) {
                val released = velocity.calculateVelocity()
                scope.launch { zoom.fling(released) }
            }
        }
    }
