package mihon.webview.jcef.runtime

import kotlinx.coroutines.flow.StateFlow
import org.cef.CefApp

/**
 * Chromium for the desktop web views, through the JCEF of the JetBrains Runtime. Chromium is one per run: the first
 * web view, challenge or extension page that needs it starts it, every caller while it starts waits for the same
 * start, and nothing starts it at app start. It ends with the app, and its helper processes with it.
 */
interface JcefRuntime {
    /** Whether this run's Java runtime carries JCEF, known without loading Chromium. */
    val presence: JcefPresence

    /** Whether something has started Chromium in this process, whether or not it is still starting. */
    val hasStarted: Boolean

    /**
     * Whether Chromium's server ended while the app ran, as when it crashed or the system ended it. Every page went
     * with it, and nothing tells them but this.
     */
    val isLost: StateFlow<Boolean>

    /**
     * Chromium, started by the first call. Throws [JcefStartException] where [presence] isn't
     * [JcefPresence.Present], when Chromium doesn't start, once it [is lost][isLost], and after [shutdown]; Chromium
     * starts once per run, so a failed start fails every later call too.
     */
    suspend fun start(): CefApp

    /**
     * Ends Chromium and its helper processes if it started, waiting a bounded time for them. The app calls it before
     * it ends or starts again, so a new process never finds Chromium's profile still in use; a process that ends
     * otherwise, such as on a crash, runs it as it exits.
     */
    fun shutdown()
}

class JcefStartException(message: String, cause: Throwable? = null) : Exception(message, cause)
