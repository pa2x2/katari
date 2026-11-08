package app.katari.entry.interactions.book.document.reader.input

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.isSecondaryPressed
import androidx.compose.ui.input.pointer.pointerInput

/**
 * Hands [onPress] where the mouse's right button goes down, for a menu of what is there. [onPress] returns whether to
 * keep the press from the selection around, whose own menu would open too; it gets the press even when one ahead kept
 * it. Seen in [pass], so [PointerEventPass.Initial] sees it ahead of what lies inside. Compose for desktop reports no pointer down for a button
 * other than the left one, only a press, so a gesture that starts at a pointer down never sees it.
 */
@Composable
internal fun Modifier.secondaryClicks(
    pass: PointerEventPass = PointerEventPass.Main,
    onPress: (Offset) -> Boolean,
): Modifier {
    val currentOnPress by rememberUpdatedState(onPress)
    return pointerInput(pass) {
        awaitPointerEventScope {
            var held = false
            while (true) {
                val event = awaitPointerEvent(pass)
                val pressed = event.buttons.isSecondaryPressed
                if (pressed && !held && event.type == PointerEventType.Press) {
                    event.changes.firstOrNull()?.let { change -> if (currentOnPress(change.position)) change.consume() }
                }
                held = pressed
            }
        }
    }
}
