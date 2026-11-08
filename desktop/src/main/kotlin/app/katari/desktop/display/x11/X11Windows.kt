package app.katari.desktop.display.x11

import com.sun.jna.Native
import logcat.LogPriority
import tachiyomi.core.common.util.system.logcat
import java.awt.Window

/** The app's windows as other X11 clients know them. */
internal object X11Windows {
    /** The X11 id of [window], a shown window of AWT's X11 toolkit; null when it can't be found. */
    fun idOf(window: Window): Long? = try {
        Native.getWindowID(window)
    } catch (e: UnsatisfiedLinkError) {
        logcat(LogPriority.WARN, e) { "Can't find the window's X11 id" }
        null
    }
}
