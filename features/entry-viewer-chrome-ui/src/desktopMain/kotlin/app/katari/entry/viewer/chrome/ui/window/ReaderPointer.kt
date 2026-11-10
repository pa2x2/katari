package app.katari.entry.viewer.chrome.ui.window

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.isCtrlPressed
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.input.pointer.pointerInput
import app.katari.presentation.kit.pointer.HiddenPointer
import app.katari.presentation.kit.pointer.PointerMoves
import app.katari.presentation.kit.pointer.TouchpadPinch
import app.katari.presentation.kit.pointer.touchpadPinch
import kotlinx.coroutines.delay

/**
 * The mouse pointer over a reader's pages, which goes once the mouse has rested a moment while [hidesWhenStill], as the
 * player hides it over the picture, and comes back when the mouse moves ([PointerMoves]). Scrolling and clicking, which
 * turn the pages, leave it hidden. With [zoomsAboutPointer], zooming with Ctrl and the wheel or a touchpad pinch shows
 * it, as the player's zoom does, since the zoom keeps the point under it still. Hidden, it hides the pointers the pages
 * show of their own too, such as a text cursor or a link's hand.
 */
@Composable
fun rememberReaderPointer(hidesWhenStill: Boolean, zoomsAboutPointer: Boolean): Modifier {
    var activity by remember { mutableIntStateOf(0) }
    var hidden by remember { mutableStateOf(false) }
    LaunchedEffect(hidesWhenStill, activity) {
        hidden = false
        if (!hidesWhenStill) return@LaunchedEffect
        delay(IDLE_MS)
        hidden = true
    }
    val pinchZooms = if (zoomsAboutPointer) {
        Modifier.touchpadPinch { pinch, _ -> if (pinch is TouchpadPinch.Move) activity++ }
    } else {
        Modifier
    }
    return Modifier
        .pointerInput(zoomsAboutPointer) {
            val moves = PointerMoves()
            awaitPointerEventScope {
                while (true) {
                    val event = awaitPointerEvent(PointerEventPass.Initial)
                    val change = event.changes.firstOrNull() ?: continue
                    val shows = when (event.type) {
                        PointerEventType.Move -> moves.isMove(change.position, resting = hidden, density = this)
                        PointerEventType.Scroll -> zoomsAboutPointer && event.keyboardModifiers.isCtrlPressed
                        else -> false
                    }
                    if (shows) activity++
                }
            }
        }
        .then(pinchZooms)
        // Always there, changing its icon: a hover icon added under a resting pointer shows only once the pointer moves
        .pointerHoverIcon(if (hidden) HiddenPointer else PointerIcon.Default, overrideDescendants = hidden)
}

private const val IDLE_MS = 2_000L
