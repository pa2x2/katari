package app.katari.desktop.display.x11

import com.sun.jna.NativeLong
import com.sun.jna.Pointer
import java.awt.Window

/**
 * X11: makes a window the one a later launch of the app opened, as the startup notification specification has an
 * application do that answers a new launch with a window it already shows. The launch's startup id goes on the
 * window's `_NET_STARTUP_ID`, which window managers such as Mutter and KWin take as a new launch of that window: they
 * activate it with the launch's user time, so their focus stealing prevention judges it by the click that started the
 * launch. Then a `remove` message ends the launch's startup notification, the busy cursor or launch feedback, since no
 * window of its own will appear. An Xwayland window is handed the id the same way, where the id is the launch's
 * Wayland activation token; whether the compositor honours it there is the compositor's choice.
 */
internal object X11LaunchHandover {
    fun handOver(window: Window, startupId: String) {
        val xlib = xlib ?: return
        val windowId = NativeLong(X11Windows.idOf(window) ?: return)
        val display = xlib.openDisplay(null) ?: return
        try {
            val id = startupId.encodeToByteArray()
            xlib.changeProperty(
                display,
                windowId,
                xlib.internAtom(display, "_NET_STARTUP_ID", false),
                xlib.internAtom(display, "UTF8_STRING", false),
                8,
                PROP_MODE_REPLACE,
                id,
                id.size,
            )
            // The display takes requests in order, so the window manager reads the id before the launch ends
            xlib.sendStartupMessage(display, windowId, "remove: ID=${quoted(startupId)}")
        } finally {
            xlib.closeDisplay(display)
        }
    }

    /**
     * Broadcasts [message] to the screen's root window, in the specification's pieces of 20 bytes: the first as
     * `_NET_STARTUP_INFO_BEGIN`, the rest as `_NET_STARTUP_INFO`, the last ending with a zero byte. [window] is the
     * sender, which the specification asks to exist until the message is sent.
     */
    private fun Xlib.sendStartupMessage(display: Pointer, window: NativeLong, message: String) {
        val root = defaultRootWindow(display)
        val begin = internAtom(display, "_NET_STARTUP_INFO_BEGIN", false)
        val continued = internAtom(display, "_NET_STARTUP_INFO", false)
        val bytes = message.encodeToByteArray() + 0.toByte()
        bytes.asList().chunked(XClientMessageEvent.MESSAGE_BYTES).forEachIndexed { index, piece ->
            val event = XClientMessageEvent().apply {
                type = CLIENT_MESSAGE
                this.display = display
                this.window = window
                messageType = if (index == 0) begin else continued
            }
            event.writeBytes(piece.toByteArray())
            sendEvent(display, root, false, NativeLong(PROPERTY_CHANGE_MASK), event)
        }
    }

    // The specification's quoting, which keeps an id with spaces, quotes or backslashes whole
    private fun quoted(value: String): String = buildString {
        append('"')
        value.forEach { char ->
            if (char == '"' || char == '\\') append('\\')
            append(char)
        }
        append('"')
    }
}
