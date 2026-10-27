package mihon.presentation.kit.window

import android.view.MotionEvent
import android.view.Window
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.geometry.Offset
import java.util.WeakHashMap

/**
 * Where the window was last touched, so a dialog or sheet can open on the side of a half-open fold the user is acting
 * on without each caller saying where it was opened from. Plain fields rather than state: it is read once, as a
 * surface opens, and nothing should recompose on every touch.
 *
 * Touches are noted at the window, before the view tree sees them, and passed on unchanged. A pointer-input node in
 * the composition can't do that: a ComposeView keeps every touch that hits such a node, so one spanning the content
 * would take touches from views drawn under it, such as a reader's pages under its controls.
 */
internal class LastTouch private constructor() {
    /** In window pixels; null until the first touch. */
    var positionInWindow: Offset? = null
        private set

    private class RecordingCallback(
        private val wrapped: Window.Callback,
        private val lastTouch: LastTouch,
    ) : Window.Callback by wrapped {
        override fun dispatchTouchEvent(event: MotionEvent): Boolean {
            if (event.actionMasked == MotionEvent.ACTION_DOWN) lastTouch.positionInWindow = Offset(event.x, event.y)
            return wrapped.dispatchTouchEvent(event)
        }
    }

    companion object {
        private val byWindow = WeakHashMap<Window, LastTouch>()

        /** The recorder of [window], which starts recording the first time it is asked for. */
        fun of(window: Window): LastTouch = byWindow.getOrPut(window) {
            LastTouch().also { lastTouch -> window.callback = RecordingCallback(window.callback, lastTouch) }
        }
    }
}

internal val LocalLastTouch = staticCompositionLocalOf<LastTouch?> { null }
