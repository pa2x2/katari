package app.katari.desktop.window

import app.katari.core.platform.PlatformContext
import cafe.adriel.voyager.navigator.Navigator
import javax.swing.JFrame
import javax.swing.SwingUtilities

class WindowPlatformContext(val window: JFrame) : PlatformContext() {
    /**
     * The root navigator the window's home shows, which screens opened from outside a screen, such as from a sheet's
     * view model, go on; null until the home has started it. A profile switch starts a new one in its place.
     *
     * Read only on the UI thread: the screens opened on it build their lifecycles there, and only some of them check, so
     * a call from another thread fails here for every screen rather than for the one that happens to check.
     */
    var navigator: Navigator? = null
        get() {
            check(SwingUtilities.isEventDispatchThread()) { "The window's screens open only on the UI thread" }
            return field
        }
}
