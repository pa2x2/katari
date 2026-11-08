package app.katari.desktop.window

import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn

/**
 * The app's window while it is open, for what opens a screen from outside any screen and any window, such as a
 * translation engine's setup that the settings ask for through the engine.
 */
@SingleIn(AppScope::class)
@Inject
class OpenWindow {
    @Volatile
    var context: WindowPlatformContext? = null
        internal set
}
