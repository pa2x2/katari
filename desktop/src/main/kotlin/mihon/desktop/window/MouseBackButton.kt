package mihon.desktop.window

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.isBackPressed
import androidx.compose.ui.input.pointer.pointerInput
import androidx.navigationevent.NavigationEventInput
import androidx.navigationevent.compose.LocalNavigationEventDispatcherOwner

/**
 * Goes back when the mouse's back button is pressed anywhere in the content, through the navigation event dispatcher
 * Compose gives each window, the one the shared screens' back handlers register with and Esc already reaches.
 */
@Composable
fun Modifier.mouseBackButtonGoesBack(): Modifier {
    val input = remember { MouseBackInput() }
    val dispatcher = LocalNavigationEventDispatcherOwner.current?.navigationEventDispatcher
    DisposableEffect(dispatcher, input) {
        dispatcher?.addInput(input)
        onDispose { dispatcher?.removeInput(input) }
    }
    return pointerInput(input) {
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
}

private class MouseBackInput : NavigationEventInput() {
    fun goBack() = dispatchOnBackCompleted()
}
