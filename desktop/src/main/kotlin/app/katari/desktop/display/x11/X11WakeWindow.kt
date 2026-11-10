package app.katari.desktop.display.x11

import com.sun.jna.NativeLong
import com.sun.jna.Pointer

/**
 * An unmapped window that a connection reading events on a thread of its own makes and no one else knows. An Xlib
 * connection is used by one thread at a time, so another thread can't tell the reader anything through it; a message
 * sent to this window, from a connection of that thread's own, ends the reader's wait for the next event as a
 * [CLIENT_MESSAGE].
 */
internal class X11WakeWindow(private val xlib: Xlib, private val display: Pointer) {
    private val window: NativeLong =
        xlib.createSimpleWindow(display, xlib.defaultRootWindow(display), 0, 0, 1, 1, 0, NativeLong(0), NativeLong(0))

    /** Called by any thread but the reader's. */
    fun wake() {
        val other = xlib.openDisplay(null) ?: return
        try {
            val message = XClientMessageEvent().apply {
                type = CLIENT_MESSAGE
                this.display = other
                window = this@X11WakeWindow.window
            }
            message.writeBytes(ByteArray(0))
            // With no event mask the message goes to the window's creator, the reading connection
            xlib.sendEvent(other, window, false, NativeLong(0), message)
            xlib.flush(other)
        } finally {
            xlib.closeDisplay(other)
        }
    }

    /** Called by the reader's thread, before it closes its connection. */
    fun destroy() {
        xlib.destroyWindow(display, window)
    }
}
