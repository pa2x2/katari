package mihon.webview.jcef.runtime

import com.jetbrains.cef.JCefAppConfig
import logcat.LogPriority
import org.cef.CefApp
import org.cef.CefSettings
import org.cef.handler.CefAppHandlerAdapter
import sun.misc.Signal
import tachiyomi.core.common.util.system.logcat
import java.nio.file.Path
import java.util.concurrent.CompletableFuture
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.TimeSource

/**
 * Chromium's start and shutdown through the JetBrains Runtime's own path: `JCefAppConfig` gives the runtime's paths and
 * switches, `CefApp.startup` loads Chromium, and `CefApp.getInstance` initialises it on threads of its own, whose end
 * [CefApp.onInitialization] reports. Only a runtime with JCEF can load this object.
 */
internal object Chromium {
    /** Starts Chromium with [switches] of the runtime's own and completes [started] once it runs. */
    fun start(switches: (List<String>) -> List<String>, dataDirectory: Path, started: CompletableFuture<CefApp>) {
        val config = JCefAppConfig.getInstance()
        val arguments = switches(config.appArgsAsList).toTypedArray()
        val settings = config.cefSettings.apply {
            // Its profile, caches and the installation data Chromium keeps beside them, which would otherwise go to
            // ~/.config/cef_user_data
            cache_path = dataDirectory.toString()
            // The log would otherwise go to the working directory, at INFO, which takes in every page's console
            log_file = dataDirectory.resolve(LOG_FILE).toString()
            log_severity = CefSettings.LogSeverity.LOGSEVERITY_WARNING
        }
        logcat(LogPriority.INFO) { "Starting Chromium with ${arguments.joinToString(" ")}" }
        CefApp.addAppHandler(
            object : CefAppHandlerAdapter(arguments) {
                override fun stateHasChanged(state: CefApp.CefAppState) {
                    logcat(LogPriority.DEBUG) { "Chromium is $state" }
                }

                // Chromium's browser process has set itself up, its signal handlers included
                override fun onContextInitialized() {
                    returnTerminationSignalsToJvm()
                }
            },
        )
        CefApp.startup(arguments)
        val app = CefApp.getInstance(arguments, settings, null)
        app.onInitialization { started.complete(app) }
    }

    /**
     * Shuts Chromium down, if it was created, and waits up to [timeout] for it and its helper processes to end;
     * whether they did.
     */
    fun shutDown(timeout: Duration): Boolean {
        val app = CefApp.getInstanceIfAny() ?: return true
        app.dispose()
        // The state changes on Chromium's threads but is reported on AWT's event thread, which may be the one ending
        // the app, so the state is read instead
        val deadline = TimeSource.Monotonic.markNow() + timeout
        while (CefApp.getState() != CefApp.CefAppState.TERMINATED && deadline.hasNotPassedNow()) {
            Thread.sleep(POLL_INTERVAL.inWholeMilliseconds)
        }
        return CefApp.getState() == CefApp.CefAppState.TERMINATED
    }

    /**
     * Chromium takes over the signals that ask a process to end as its browser process sets up, and answers them by
     * ending the process itself, so the JVM's shutdown hooks, which save the app's preferences and cookies and shut
     * Chromium down, wouldn't run. They get the JVM's own answer back: exiting through the hooks with 128 plus the
     * signal's number. JCEF has no setting that keeps Chromium's handlers out.
     */
    private fun returnTerminationSignalsToJvm() {
        TERMINATION_SIGNALS.forEach { name ->
            val signal = try {
                Signal(name)
            } catch (_: IllegalArgumentException) {
                // A signal the operating system doesn't have
                return@forEach
            }
            Signal.handle(signal) { Runtime.getRuntime().exit(SIGNAL_EXIT_STATUS + it.number) }
        }
    }

    private const val LOG_FILE = "chromium.log"

    private val TERMINATION_SIGNALS = listOf("TERM", "INT", "HUP")

    private const val SIGNAL_EXIT_STATUS = 128

    private val POLL_INTERVAL = 20.milliseconds
}
