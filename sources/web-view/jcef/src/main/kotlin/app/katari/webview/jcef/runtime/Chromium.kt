package app.katari.webview.jcef.runtime

import app.katari.webview.jcef.cookie.ChromiumCookieManager
import app.katari.webview.jcef.cookie.JcefCookieManager
import com.jetbrains.cef.JCefAppConfig
import logcat.LogPriority
import org.cef.CefApp
import org.cef.CefSettings
import org.cef.handler.CefAppHandlerAdapter
import org.cef.network.CefCookieManager
import tachiyomi.core.common.util.system.logcat
import java.nio.file.Path
import java.util.concurrent.CompletableFuture
import java.util.concurrent.TimeUnit
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.TimeSource

/**
 * Chromium's start and shutdown through the JetBrains Runtime's own path, with Chromium's browser process in a process
 * of its own: the runtime's `cef_server`, which JCEF starts and talks to over a Unix socket. `JCefAppConfig` gives the
 * runtime's switches and settings, and `CefApp.getInstance` starts the server on a thread of its own, whose end
 * [CefApp.onInitialization] reports. Only a runtime with JCEF can load this object.
 *
 * Chromium's browser process is not run inside the app's: there, every Chromium crash would end the app, and Chromium's
 * memory report, which reads the process's malloc heap through glibc's 32-bit `mallinfo()` and crashes on purpose once
 * the heap passes 2 GiB, would count the app's own native memory too.
 */
internal object Chromium {
    /**
     * Starts Chromium with [switches] of the runtime's own and completes [started] once it runs. [lost] is called if
     * the server ends before the app shuts it down.
     */
    fun start(
        switches: (List<String>) -> List<String>,
        dataDirectory: Path,
        started: CompletableFuture<CefApp>,
        lost: () -> Unit,
    ) {
        // JCEF reads both as it starts its first server. Over the default, a TCP port on localhost, any process on the
        // machine could drive the browser and read its cookies; the socket is the user's alone
        System.setProperty(SERVER_USES_SOCKET, "true")
        CefApp.setIsRemoteEnabled(true)
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
        // The server takes its switches from the handler
        CefApp.addAppHandler(
            object : CefAppHandlerAdapter(arguments) {
                override fun stateHasChanged(state: CefApp.CefAppState) {
                    logcat(LogPriority.DEBUG) { "Chromium is $state" }
                }
            },
        )
        val app = CefApp.getInstance(arguments, settings, null)
        app.setDisconnectionCallback(lost)
        app.onInitialization {
            watchServer(app)
            started.complete(app)
        }
    }

    // JCEF learns that its server ended only from a call that fails, which it reports to the disconnection callback and
    // throws. The app may make none for long, and one it makes then, on whichever thread, would end the app with that
    // exception, so the server's exit is watched and a harmless call made at once. JCEF makes no call once it knows
    private fun watchServer(app: CefApp) {
        ProcessHandle.current().children().filter { it.isServerProcess() }.forEach { server ->
            server.onExit().thenRun {
                try {
                    // The app registers no scheme handlers, so there are none to clear
                    app.clearSchemeHandlerFactories()
                } catch (_: IllegalStateException) {
                    // The server's end, which JCEF has reported by now
                }
            }
        }
    }

    /**
     * The cookies of Chromium's profile, which every browser shares; Chromium must have started. Called once, for the
     * cookie mirror, which keeps the one manager for as long as Chromium runs.
     */
    fun cookieManager(): ChromiumCookieManager = JcefCookieManager(CefCookieManager.getGlobalManager())

    /**
     * Shuts Chromium down, if it was created, and waits up to [timeout] for its server and helper processes to end;
     * whether they did.
     */
    fun shutDown(timeout: Duration): Boolean {
        val app = CefApp.getInstanceIfAny() ?: return true
        val deadline = TimeSource.Monotonic.markNow() + timeout
        // The server's processes, known before the server is told to end
        val processes = ProcessHandle.current().descendants().filter { it.isServerProcess() }.toList()
        app.dispose()
        // The state changes on JCEF's threads but is reported on AWT's event thread, which may be the one ending the
        // app, so the state is read instead
        while (CefApp.getState() != CefApp.CefAppState.TERMINATED && deadline.hasNotPassedNow()) {
            Thread.sleep(POLL_INTERVAL.inWholeMilliseconds)
        }
        // JCEF counts Chromium as ended once it has asked the server to end, which ends its helpers and frees the
        // profile a moment later
        processes.forEach { process ->
            runCatching { process.onExit().get(deadline.remaining(), TimeUnit.MILLISECONDS) }
        }
        return CefApp.getState() == CefApp.CefAppState.TERMINATED && processes.none { it.isAlive }
    }

    private fun ProcessHandle.isServerProcess(): Boolean =
        info().command().map { it.endsWith(SERVER_EXECUTABLE) }.orElse(false)

    private fun TimeSource.Monotonic.ValueTimeMark.remaining(): Long =
        (-elapsedNow()).inWholeMilliseconds.coerceAtLeast(0)

    /** JCEF's switch to talk to its server over a Unix socket in the temporary folder instead of a TCP port. */
    private const val SERVER_USES_SOCKET = "CEF_SERVER_USE_PIPE"

    /** The runtime's Chromium server, which every process of Chromium's runs. */
    const val SERVER_EXECUTABLE = "cef_server"

    private const val LOG_FILE = "chromium.log"

    private val POLL_INTERVAL = 20.milliseconds
}
