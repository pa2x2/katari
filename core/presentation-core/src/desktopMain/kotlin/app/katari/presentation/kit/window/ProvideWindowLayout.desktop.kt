package app.katari.presentation.kit.window

import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.onPlaced

@Composable
internal actual fun rememberWindowFold(): WindowFold? = null

@Composable
internal actual fun rememberMultiWindowCheck(): () -> Boolean = { false }

@Composable
internal actual fun rememberWindowLastPress(): LastPress? = remember { LastPress() }

/*
 * A window's presses are read on their way down to the content, before anything under them handles them, and left
 * unconsumed, so every control still gets them. Keys are read the same way, ahead of the focused control.
 */
@Composable
internal actual fun RecordingPresses(lastPress: LastPress?, content: @Composable () -> Unit) {
    if (lastPress == null) {
        content()
        return
    }
    val root = remember { RootPlacement() }
    Box(
        modifier = Modifier
            .onPlaced { root.coordinates = it }
            .pointerInput(lastPress) {
                awaitPointerEventScope {
                    while (true) {
                        val event = awaitPointerEvent(PointerEventPass.Initial)
                        if (event.type != PointerEventType.Press) continue
                        val position = event.changes.firstOrNull()?.position ?: continue
                        lastPress.press(root.coordinates?.takeIf { it.isAttached }?.localToWindow(position) ?: position)
                    }
                }
            }
            .onPreviewKeyEvent { event ->
                if (event.type == KeyEventType.KeyDown && event.key !in ModifierKeys) lastPress.keyPressed()
                false
            },
        propagateMinConstraints = true,
    ) {
        content()
    }
}

/** Where the recording box sits in the window, to turn its presses into window positions. */
private class RootPlacement {
    var coordinates: LayoutCoordinates? = null
}

// Held for a shortcut or a modified click; only the key they go with counts as a key press
private val ModifierKeys = setOf(
    Key.CtrlLeft,
    Key.CtrlRight,
    Key.AltLeft,
    Key.AltRight,
    Key.ShiftLeft,
    Key.ShiftRight,
    Key.MetaLeft,
    Key.MetaRight,
)
