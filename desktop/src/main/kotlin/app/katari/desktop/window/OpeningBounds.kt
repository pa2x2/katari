package app.katari.desktop.window

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.isSpecified
import androidx.compose.ui.window.WindowPlacement
import androidx.compose.ui.window.WindowPosition
import androidx.compose.ui.window.WindowState
import java.awt.Window
import java.awt.event.WindowAdapter
import java.awt.event.WindowEvent
import kotlin.math.roundToInt

/** Where and how large [WindowState] asked a window to open, before the window moved or resized it. */
internal class OpeningBounds(val position: WindowPosition, val size: DpSize) {
    constructor(state: WindowState) : this(state.position, state.size)
}

/**
 * Puts Compose's [window] back at [opening] once it is shown, while it still floats. Compose sizes a window that
 * isn't shown yet by packing it, and AWT then keeps the content size it worked out from a frame it can only guess
 * before the window manager has framed the window (25 pixels on top, 5 on the other sides). Once the real frame is
 * known, a window framed differently, as by KWin, which has no side borders, or by no window manager at all, would
 * open that much off its saved size, and further off at every start, as that size is saved again.
 */
@Composable
internal fun OpensAtBounds(window: Window, state: WindowState, opening: OpeningBounds) {
    DisposableEffect(window) {
        val listener = object : WindowAdapter() {
            override fun windowOpened(event: WindowEvent) {
                window.removeWindowListener(this)
                window.putBack(state, opening)
            }
        }
        if (window.isShowing) window.putBack(state, opening) else window.addWindowListener(listener)
        onDispose { window.removeWindowListener(listener) }
    }
}

// In AWT's units, one dp each, as Compose places its windows
private fun Window.putBack(state: WindowState, opening: OpeningBounds) {
    if (state.placement != WindowPlacement.Floating || !opening.size.isSpecified) return
    val width = opening.size.width.value.roundToInt()
    val height = opening.size.height.value.roundToInt()
    if (this.width == width && this.height == height) return
    val position = opening.position
    // The place it opened at, not where it is: KWin at fractional scales puts a framed window a pixel above where
    // it is asked to, so asking again from where it is would move it
    if (position is WindowPosition.Absolute) {
        setBounds(position.x.value.roundToInt(), position.y.value.roundToInt(), width, height)
    } else {
        setSize(width, height)
    }
}
