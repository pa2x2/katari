package mihon.entry.interactions.book.document.reader.input

import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.pointerInput
import mihon.entry.viewer.chrome.ui.wheel.WheelTurns
import kotlin.math.abs

/**
 * Scrolling over turned pages, by a wheel or a touchpad's two fingers, turns them, as [WheelTurns] counts turns, rather
 * than moving the pager by the distance scrolled: down to the next page and up to the previous one, sideways to the
 * page on that side. [move] turns pages in reading order, which [rtl] runs from right to left. [key] restarts the count
 * when the pages it turns are others.
 */
internal fun Modifier.pageWheel(key: Any?, rtl: Boolean, move: (pages: Int) -> Unit): Modifier =
    pointerInput(key, rtl) {
        val turns = WheelTurns()
        awaitPointerEventScope {
            while (true) {
                val event = awaitPointerEvent(PointerEventPass.Initial)
                if (event.type != PointerEventType.Scroll) continue
                // Taken already as Ctrl and the wheel change the text size
                val change = event.changes.firstOrNull()?.takeUnless { it.isConsumed } ?: continue
                change.consume()
                turns.onScroll(change.scrollDelta, change.uptimeMillis) { notches -> turnBy(notches, rtl, move) }
            }
        }
    }

/** Turns a page for each whole notch, along whichever axis has more, and returns the notches used. */
private fun turnBy(notches: Offset, rtl: Boolean, move: (Int) -> Unit): Offset {
    val vertical = abs(notches.y) >= abs(notches.x)
    val whole = (if (vertical) notches.y else notches.x).toInt()
    if (whole == 0) return Offset.Zero
    move(if (vertical || !rtl) whole else -whole)
    return if (vertical) Offset(0f, whole.toFloat()) else Offset(whole.toFloat(), 0f)
}
