package app.katari.desktop.window

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import java.awt.KeyboardFocusManager
import java.beans.PropertyChangeListener

/**
 * Whether one of the app's windows has the focus, the main window, a dialog's or a picture-in-picture one, as AWT
 * knows only the app's own windows: what the user types or touches goes to the app.
 */
@Composable
internal fun appHasFocus(): Boolean {
    val focusManager = remember { KeyboardFocusManager.getCurrentKeyboardFocusManager() }
    var focused by remember { mutableStateOf(focusManager.activeWindow != null) }
    DisposableEffect(focusManager) {
        val listener = PropertyChangeListener { focused = it.newValue != null }
        focusManager.addPropertyChangeListener(ACTIVE_WINDOW, listener)
        onDispose { focusManager.removePropertyChangeListener(ACTIVE_WINDOW, listener) }
    }
    return focused
}

private const val ACTIVE_WINDOW = "activeWindow"
