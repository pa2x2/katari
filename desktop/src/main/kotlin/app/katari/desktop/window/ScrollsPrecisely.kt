package app.katari.desktop.window

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import app.katari.desktop.display.backend.DisplayBackend
import java.awt.Window
import java.awt.event.WindowAdapter
import java.awt.event.WindowEvent

/**
 * Scrolls [window] by the display's own account of scrolling while it is open
 * ([DisplayBackend.preciseScrolling]), from when it is first shown, as the display knows no window before that.
 */
@Composable
internal fun ScrollsPrecisely(window: Window, backend: DisplayBackend) {
    DisposableEffect(window, backend) {
        var scrolling: AutoCloseable? = null
        val shown = object : WindowAdapter() {
            override fun windowOpened(event: WindowEvent) {
                scrolling = backend.preciseScrolling(window)
            }
        }
        if (window.isShowing) scrolling = backend.preciseScrolling(window) else window.addWindowListener(shown)
        onDispose {
            window.removeWindowListener(shown)
            scrolling?.close()
        }
    }
}
