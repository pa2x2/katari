package app.katari.webview.jcef.runtime

/**
 * Chromium for the desktop web views, through the JCEF of the JetBrains Runtime. One Chromium runs at a time: the
 * first web view, challenge or extension page that needs it starts it, every caller while it starts waits for the same
 * start, and nothing starts it at app start. It ends with the app, and its helper processes with it. A start that
 * failed, or a Chromium that was lost, is over, and the next page that needs Chromium starts it anew.
 */
interface JcefRuntime {
    /** Whether the Java runtime carries JCEF right now, known without loading Chromium. */
    val presence: JcefPresence

    /** Whether Chromium is starting or running in this process. */
    val isRunning: Boolean

    /**
     * Chromium, started by this call unless it is starting or running. Throws [JcefStartException] where [presence]
     * isn't [JcefPresence.Present], when Chromium doesn't start, and after [shutdown].
     */
    suspend fun start(): RunningChromium

    /**
     * Ends the Chromium that runs, waiting a bounded time for it and its helper processes: its pages end as they do
     * when Chromium is lost, and the next [start] begins another. Called before the app removes Chromium's files.
     */
    fun stop()

    /**
     * Ends Chromium and its helper processes if it started, waiting a bounded time for them. The app calls it before
     * it ends or starts again, so a new process never finds Chromium's profile still in use; a process that ends
     * otherwise, such as on a crash, runs it as it exits.
     */
    fun shutdown()
}

class JcefStartException(message: String, cause: Throwable? = null) : Exception(message, cause)
