package mihon.desktop.display.x11

import logcat.LogPriority
import tachiyomi.core.common.util.system.logcat

/**
 * Xlib's support for threads, which a library calling Xlib from threads of its own needs (`XInitThreads`). Xlib honours
 * it only as the process's first Xlib call, so it runs before AWT starts.
 */
internal object XlibThreads {
    /** Whether Xlib is ready for threads; false where it doesn't load. */
    fun initialize(): Boolean {
        val xlib = xlib
        if (xlib == null) {
            logcat(LogPriority.WARN) { "Can't prepare Xlib for threads: it doesn't load" }
            return false
        }
        return xlib.initThreads() != 0
    }
}
