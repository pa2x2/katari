package app.katari.entry.interactions.book.document.reader.input

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.AwaitPointerEventScope
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerInputChange
import androidx.compose.ui.input.pointer.PointerInputScope
import androidx.compose.ui.input.pointer.PointerType
import androidx.compose.ui.input.pointer.isPrimaryPressed

/**
 * Left clicks that follow one another closely. The text under them would count them as a double or triple click and
 * select a word or a paragraph, but in the reader each is a click of its own, as quick clicks on the side turn page
 * after page. So they are kept from the text, ahead of it, and each runs [onClick] with where it landed, as the first
 * click runs the reader's tap. Only a mouse's: on a touch screen a double tap selects a word as usual. While [takesAll],
 * as while pages turn under the mouse, every click is taken so.
 *
 * Text counts a press as repeated when it comes within the double tap timeout of a press it saw, and nearer than the
 * mouse's slop; a press it never saw, as one taken here, doesn't count. Taking every press that close in time and
 * within the larger touch slop of one let through keeps it from counting any.
 */
internal suspend fun PointerInputScope.detectRepeatedClicks(
    takesAll: () -> Boolean = { false },
    onClick: (Offset) -> Unit,
) {
    var passed = emptyList<PointerInputChange>()
    awaitEachGesture {
        val down = awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
        if (down.type != PointerType.Mouse || !currentEvent.buttons.isPrimaryPressed) return@awaitEachGesture
        val timeout = viewConfiguration.doubleTapTimeoutMillis
        passed = passed.filter { down.uptimeMillis - it.uptimeMillis < timeout }
        if (!takesAll() && passed.none { (down.position - it.position).getDistance() < viewConfiguration.touchSlop }) {
            passed = passed + down
            return@awaitEachGesture
        }
        down.consume()
        awaitClickUp(down)?.let(onClick)
    }
}

/** Where [down] is let go as a click, or null once it moves past the touch slop first. */
private suspend fun AwaitPointerEventScope.awaitClickUp(down: PointerInputChange): Offset? {
    while (true) {
        val change = awaitPointerEvent(PointerEventPass.Initial).changes.firstOrNull { it.id == down.id } ?: return null
        if ((change.position - down.position).getDistance() > viewConfiguration.touchSlop) return null
        if (!change.pressed) {
            change.consume()
            return change.position
        }
    }
}
