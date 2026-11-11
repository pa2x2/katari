package eu.kanade.tachiyomi.ui.reader.viewer.pager

import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.isCtrlPressed
import androidx.compose.ui.input.pointer.pointerInput
import app.katari.presentation.kit.pager.WheelTurns
import app.katari.presentation.kit.pointer.TouchpadScroll
import app.katari.presentation.kit.pointer.touchpadScroll
import eu.kanade.tachiyomi.ui.reader.model.ReaderPage
import kotlin.math.abs
import kotlin.math.pow

/**
 * Scrolling over the paged viewer, by a wheel or a touchpad's two fingers, seen ahead of the pager, which would move
 * by the scrolled distance. A page that doesn't fit its slot, zoomed or wider than it, scrolls until its edge; then
 * scrolling down turns to the next page and up to the previous one, and sideways to the page on that side, counted as
 * [WheelTurns] counts them. With Ctrl held the wheel zooms the page under the pointer.
 * [pageAt] finds the page at a point of the viewer and where it lies there.
 */
internal fun Modifier.pagerWheel(
    viewer: PagedViewer,
    pageAt: (Offset) -> Pair<ReaderPage, Rect>?,
): Modifier = pointerInput(viewer) {
    val turns = WheelTurns()
    awaitPointerEventScope {
        while (true) {
            val event = awaitPointerEvent(PointerEventPass.Initial)
            if (event.type != PointerEventType.Scroll) continue
            val change = event.changes.firstOrNull() ?: continue
            val delta = change.scrollDelta
            change.consume()
            val target = pageAt(change.position)
            val zoom = target?.let { viewer.zoomOf(it.first) }
            val ctrl = event.keyboardModifiers.isCtrlPressed
            val focus = change.position - (target?.second?.topLeft ?: Offset.Zero)
            if (zoom != null && zoom.takeWheel(delta, event.touchpadScroll, focus, ctrl, viewer::onPageZoomed)) {
                turns.reset()
                continue
            }
            if (!ctrl) turns.onScroll(delta, change.uptimeMillis, event.touchpadScroll, viewer::turnBy)
        }
    }
}

/**
 * Uses a scroll of [delta] notches over this page, a step of [touchpad]'s scroll where it is one, at [focus] in its
 * slot: with [ctrl] held it zooms about [focus], telling [onZoomed]; otherwise it scrolls the page while it doesn't fit
 * its slot and can move that way, by a share of the slot a wheel's notch and as far as content scrolls under a
 * touchpad. Returns whether the page took the scroll, which then turns no page.
 */
internal fun PagerPageZoom.takeWheel(
    delta: Offset,
    touchpad: TouchpadScroll?,
    focus: Offset,
    ctrl: Boolean,
    onZoomed: () -> Unit,
): Boolean {
    if (ctrl) {
        // The fingers zoom as far as they go, not on with the glide they leave
        if (delta.y == 0f || touchpad?.fingersMoving == false) return true
        zoomBy(ZOOM_STEP.pow(-delta.y), focus)
        onZoomed()
        return true
    }
    val step = minOf(slotSize.width, slotSize.height) * PAN_SHARE
    val pan = touchpad?.let { -it.distance } ?: Offset(-delta.x * step, -delta.y * step)
    if (pan == Offset.Zero || !canPan(pan)) return false
    panBy(pan)
    return true
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

// The share of the slot's shorter side a wheel's notch scrolls a page that doesn't fit
private const val PAN_SHARE = 0.15f
private const val ZOOM_STEP = 1.1f
