package mihon.desktop.window

import cafe.adriel.voyager.navigator.Navigator
import mihon.core.platform.PlatformContext
import javax.swing.JFrame

class WindowPlatformContext(val window: JFrame) : PlatformContext() {
    /**
     * The root navigator the window's home shows, which screens opened from outside a screen, such as from a sheet's
     * view model, go on; null until the home has started it. A profile switch starts a new one in its place.
     */
    var navigator: Navigator? = null
}
