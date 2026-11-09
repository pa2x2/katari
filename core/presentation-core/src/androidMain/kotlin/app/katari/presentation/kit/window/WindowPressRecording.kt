package app.katari.presentation.kit.window

import android.view.KeyEvent
import android.view.MotionEvent
import android.view.Window
import androidx.compose.ui.geometry.Offset
import java.util.WeakHashMap

/*
 * Presses are noted at the window, before the view tree sees them, and passed on unchanged. A pointer-input node in
 * the composition can't do that: a ComposeView keeps every touch that hits such a node, so one spanning the content
 * would take touches from views drawn under it, such as a reader's pages under its controls.
 */

private val lastPressByWindow = WeakHashMap<Window, LastPress>()

/** The [LastPress] of [window], which starts recording the first time it is asked for. */
internal fun lastPressOf(window: Window): LastPress = lastPressByWindow.getOrPut(window) {
    LastPress().also { lastPress -> window.callback = PressRecordingCallback(window.callback, lastPress) }
}

private class PressRecordingCallback(
    private val wrapped: Window.Callback,
    private val lastPress: LastPress,
) : Window.Callback by wrapped {
    override fun dispatchTouchEvent(event: MotionEvent): Boolean {
        if (event.actionMasked == MotionEvent.ACTION_DOWN) lastPress.press(Offset(event.x, event.y))
        return wrapped.dispatchTouchEvent(event)
    }

    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        if (event.action == KeyEvent.ACTION_DOWN && !KeyEvent.isModifierKey(event.keyCode)) lastPress.keyPressed()
        return wrapped.dispatchKeyEvent(event)
    }
}
