package app.katari.entry.interactions.book.document.reader.input

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.AwaitPointerEventScope
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerInputChange
import androidx.compose.ui.input.pointer.PointerInputScope
import androidx.compose.ui.input.pointer.PointerType
import androidx.compose.ui.input.pointer.changedToUp
import androidx.compose.ui.input.pointer.changedToUpIgnoreConsumed
import androidx.compose.ui.input.pointer.isOutOfBounds

/**
 * The reader's own clicks and taps on its pages, which turn pages or show the menu, as [onClick] with where they
 * landed.
 *
 * A press that moves past the touch slop is a drag, never a click. On a touch screen the pager or list takes such a
 * drag and so cancels a tap, but Compose's scrolling ignores a mouse's drags, which are left to text selection. A plain
 * tap detector would then take a mouse drag for a click wherever it was let go, turning a page by where the drag ended.
 *
 * A mouse press is left unconsumed, so the selection around still sees it: a drag that starts beside the text, in a
 * margin or between paragraphs, selects the text it crosses, as one that starts on the text does. That selection
 * consumes every move and release of a mouse press, whether it selects anything or not, so for a mouse only the
 * distance moved tells a click from a drag; a click that selected a little text on the way clears it as usual.
 */
internal suspend fun PointerInputScope.detectReaderClicks(onClick: (Offset) -> Unit) {
    awaitEachGesture {
        val down = awaitFirstDown()
        val mouse = down.type == PointerType.Mouse
        if (!mouse) down.consume()
        awaitReaderClickUp(down, mouse)?.let { up ->
            up.consume()
            onClick(up.position)
        }
    }
}

/** The change that lets [down] go as a click, or null once it moves away or, for a touch, something else takes it. */
private suspend fun AwaitPointerEventScope.awaitReaderClickUp(
    down: PointerInputChange,
    mouse: Boolean,
): PointerInputChange? {
    while (true) {
        val event = awaitPointerEvent()
        if (event.changes.all { if (mouse) it.changedToUpIgnoreConsumed() else it.changedToUp() }) {
            return event.changes.first()
        }
        val moved = event.changes.any { change ->
            change.id == down.id && (change.position - down.position).getDistance() > viewConfiguration.touchSlop
        }
        if (moved || event.changes.any { it.isOutOfBounds(size, extendedTouchPadding) }) return null
        if (!mouse) {
            if (event.changes.any { it.isConsumed }) return null
            // The pager sees the event after this does and consumes the swipe it starts then
            if (awaitPointerEvent(PointerEventPass.Final).changes.any { it.isConsumed }) return null
        }
    }
}
