package app.katari.webview.jcef.runtime

import kotlinx.coroutines.CompletableDeferred
import org.cef.CefApp

/**
 * One start of Chromium, which the pages opened on it live and end with. Chromium's server may end while the app runs,
 * as when it crashed or the system ended it; every page on it went with it then, and nothing tells them but this.
 */
class RunningChromium internal constructor(val app: CefApp, private val lost: CompletableDeferred<Unit>) {
    /** Whether this Chromium's server has ended. */
    val isLost: Boolean
        get() = lost.isCompleted

    /** Returns once this Chromium's server has ended; never, if it runs until the app shuts it down. */
    suspend fun awaitLost() = lost.await()
}
