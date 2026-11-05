package eu.kanade.tachiyomi.ui.reader.viewer.pager

import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.isCtrlPressed
import androidx.compose.ui.input.pointer.pointerInput
import eu.kanade.tachiyomi.ui.reader.model.ReaderPage
import kotlinx.coroutines.Job
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.pow

/**
 * Scrolling over the paged viewer, by a wheel or a touchpad's two fingers, seen ahead of the pager, which would move
 * by the scrolled distance. A page that doesn't fit its slot, zoomed or wider than it, scrolls until its edge; then
 * scrolling down turns to the next page and up to the previous one, and sideways to the page on that side. A wheel
 * turns a page for each notch. A touchpad scrolls in fractions of a notch, many to a swipe, so a swipe turns one page
 * and the next turn waits until scrolling has stopped. With Ctrl held the wheel zooms the page under the pointer.
 * [pageAt] finds the page at a point of the viewer and where it lies there.
 */
internal fun Modifier.pagerWheel(
    viewer: PagedViewer,
    pageAt: (Offset) -> Pair<ReaderPage, Rect>?,
): Modifier = pointerInput(viewer) {
    coroutineScope {
        var notches = Offset.Zero
        var swipeTurned = false
        var swipeEnd: Job? = null
        awaitPointerEventScope {
            while (true) {
                val event = awaitPointerEvent(PointerEventPass.Initial)
                if (event.type != PointerEventType.Scroll) continue
                val change = event.changes.firstOrNull() ?: continue
                val delta = change.scrollDelta
                change.consume()
                val target = pageAt(change.position)
                val zoom = target?.let { viewer.zoomOf(it.first) }
                if (event.keyboardModifiers.isCtrlPressed) {
                    if (zoom == null || delta.y == 0f) continue
                    zoom.zoomBy(ZOOM_STEP.pow(-delta.y), change.position - target.second.topLeft)
                    viewer.onPageZoomed()
                    continue
                }
                if (zoom != null) {
                    val step = minOf(zoom.slotSize.width, zoom.slotSize.height) * PAN_SHARE
                    val pan = Offset(-delta.x * step, -delta.y * step)
                    if (zoom.canPan(pan)) {
                        zoom.panBy(pan)
                        notches = Offset.Zero
                        continue
                    }
                }
                val fromTouchpad = delta.x % 1f != 0f || delta.y % 1f != 0f
                if (fromTouchpad) {
                    swipeEnd?.cancel()
                    swipeEnd = launch {
                        delay(SWIPE_END_MS)
                        swipeTurned = false
                        notches = Offset.Zero
                    }
                    if (swipeTurned) continue
                }
                notches += delta
                val turned = viewer.turnBy(notches)
                if (turned != Offset.Zero) {
                    notches -= turned
                    if (fromTouchpad) swipeTurned = true
                }
            }
        }
    }
}

/**
 * Turns a page for each whole notch in [notches], down and right forward, along whichever axis has more, and returns
 * the notches used.
 */
private fun PagedViewer.turnBy(notches: Offset): Offset {
    val vertical = abs(notches.y) >= abs(notches.x)
    val along = if (vertical) notches.y else notches.x
    val whole = along.toInt()
    if (whole == 0) return Offset.Zero
    repeat(abs(whole)) {
        when {
            vertical && whole > 0 -> moveDown()
            vertical -> moveUp()
            whole > 0 -> moveRight()
            else -> moveLeft()
        }
    }
    return if (vertical) Offset(0f, whole.toFloat()) else Offset(whole.toFloat(), 0f)
}

// The share of the slot's shorter side a notch scrolls a page that doesn't fit
private const val PAN_SHARE = 0.15f
private const val ZOOM_STEP = 1.1f

// How long after the last scroll a touchpad swipe counts as over, as the desktop player waits
private const val SWIPE_END_MS = 500L
