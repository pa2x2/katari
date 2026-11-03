package mihon.desktop.portal

import mihon.desktop.display.backend.DisplayBackend
import mihon.desktop.display.x11.X11Windows
import java.awt.Window

/**
 * Linux: how a portal call names [window] as the parent of the dialog it shows, so the desktop keeps the dialog over
 * it and modal to it ("Window identifiers" in the portal documentation). On X11, which a Wayland session runs the
 * app's windows through by default (XWayland), it is `x11:` and the window's id in hex. A window of the native Wayland
 * toolkit would be named by a handle exported through `xdg_foreign`, which that toolkit doesn't export, so there, as
 * without a window, the name is empty, and the dialog has no parent.
 */
internal fun portalParentWindow(window: Window?, displayBackend: DisplayBackend): String {
    if (window == null || displayBackend.isNativeWayland) return ""
    val id = X11Windows.idOf(window) ?: return ""
    return "x11:${id.toString(16)}"
}
