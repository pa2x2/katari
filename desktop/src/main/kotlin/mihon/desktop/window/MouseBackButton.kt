package mihon.desktop.window

import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.isBackPressed
import androidx.compose.ui.input.pointer.pointerInput

/** Goes back through [input] when the mouse's back button is pressed anywhere in the content. */
fun Modifier.mouseBackButtonGoesBack(input: WindowBackInput): Modifier = pointerInput(input) {
    awaitPointerEventScope {
        var backWasPressed = false
        while (true) {
            // Seen before the content, which doesn't consume this button, so it works over any screen
            val event = awaitPointerEvent(PointerEventPass.Initial)
            val backPressed = event.buttons.isBackPressed
            if (event.type == PointerEventType.Press && backPressed && !backWasPressed) input.goBack()
            backWasPressed = backPressed
        }
    }
}
