package app.katari.presentation.kit.window

import android.view.MotionEvent
import android.view.Window
import androidx.compose.ui.geometry.Offset
import java.util.WeakHashMap

/*
 * Touches are noted at the window, before the view tree sees them, and passed on unchanged. A pointer-input node in
 * the composition can't do that: a ComposeView keeps every touch that hits such a node, so one spanning the content
 * would take touches from views drawn under it, such as a reader's pages under its controls.
 */

private val lastTouchByWindow = WeakHashMap<Window, LastTouch>()

/** The [LastTouch] of [window], which starts recording the first time it is asked for. */
internal fun lastTouchOf(window: Window): LastTouch = lastTouchByWindow.getOrPut(window) {
    LastTouch().also { lastTouch -> window.callback = TouchRecordingCallback(window.callback, lastTouch) }
}

private class TouchRecordingCallback(
    private val wrapped: Window.Callback,
    private val lastTouch: LastTouch,
) : Window.Callback by wrapped {
    override fun dispatchTouchEvent(event: MotionEvent): Boolean {
        if (event.actionMasked == MotionEvent.ACTION_DOWN) lastTouch.positionInWindow = Offset(event.x, event.y)
        return wrapped.dispatchTouchEvent(event)
    }
}
