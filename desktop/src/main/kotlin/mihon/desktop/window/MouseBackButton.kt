package mihon.desktop.window

import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.isBackPressed
import androidx.compose.ui.input.pointer.pointerInput
import mihon.presentation.kit.shortcut.KeyboardShortcutRegistry

/**
 * Hands the mouse's back button, pressed anywhere in the content, to the window's [shortcuts], where the window's own
 * back declares it and a screen can declare it for a way back of its own. Goes back through [input] if none runs.
 */
fun Modifier.mouseBackButtonGoesBack(shortcuts: KeyboardShortcutRegistry, input: WindowBackInput): Modifier =
    pointerInput(shortcuts, input) {
        awaitPointerEventScope {
            var backWasPressed = false
            while (true) {
                // Seen before the content, which doesn't consume this button, so it works over any screen
                val event = awaitPointerEvent(PointerEventPass.Initial)
                val backPressed = event.buttons.isBackPressed
                if (event.type == PointerEventType.Press && backPressed && !backWasPressed) {
                    if (!shortcuts.onMouseBack()) input.goBack()
                }
                backWasPressed = backPressed
            }
        }
    }
