package mihon.desktop.portal

import mihon.desktop.display.x11.X11Windows
import java.awt.Window

/**
 * Linux: how a portal call names [window] as the parent of the dialog it shows, so the desktop keeps the dialog over
 * it and modal to it ("Window identifiers" in the portal documentation): `x11:` and the window's id in hex, as the
 * app's windows go through X11, which a Wayland session runs them on through XWayland. Without a window the name is
 * empty, and the dialog has no parent.
 */
internal fun portalParentWindow(window: Window?): String {
    if (window == null) return ""
    val id = X11Windows.idOf(window) ?: return ""
    return "x11:${id.toString(16)}"
}
