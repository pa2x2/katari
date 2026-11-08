package app.katari.desktop.window

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.navigationevent.NavigationEventInput
import androidx.navigationevent.compose.LocalNavigationEventDispatcherOwner

/**
 * Goes back through the navigation event dispatcher Compose gives each window, the one the shared screens' back
 * handlers register with and Esc already reaches, for the window's other ways back.
 */
class WindowBackInput : NavigationEventInput() {
    fun goBack() = dispatchOnBackCompleted()
}

@Composable
fun rememberWindowBackInput(): WindowBackInput {
    val input = remember { WindowBackInput() }
    val dispatcher = LocalNavigationEventDispatcherOwner.current?.navigationEventDispatcher
    DisposableEffect(dispatcher, input) {
        dispatcher?.addInput(input)
        onDispose { dispatcher?.removeInput(input) }
    }
    return input
}
