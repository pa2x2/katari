package mihon.webview.jcef.runtime

import kotlinx.coroutines.future.await
import logcat.LogPriority
import mihon.webview.jcef.cookie.ChromiumCookieMirror
import mihon.webview.jcef.data.ChromiumProfileData
import org.cef.CefApp
import tachiyomi.core.common.util.system.logcat
import java.util.concurrent.CompletableFuture
import java.util.concurrent.TimeUnit
import kotlin.time.Duration.Companion.seconds
import kotlin.time.TimeSource

/**
 * The runtime on the JetBrains Runtime's JCEF: one start per process, shared by every caller, and one shutdown, run by
 * the app as it ends or, failing that, as the process exits. Chromium counts as started once it has the app's
 * [cookies], and hands its own back before it shuts down. The calls into JCEF are [Chromium]'s, which only a runtime
 * with JCEF loads; this class is loaded on any runtime, as the app binds it on all of them.
 */
class JbrJcefRuntime(private val host: JcefHost, private val cookies: ChromiumCookieMirror) : JcefRuntime {
    override val presence: JcefPresence by lazy(JcefPresenceCheck::check)

    private val lock = Any()

    // Both guarded by lock
    private var startup: CompletableFuture<CefApp>? = null
    private var hasEnded = false

    override val hasStarted: Boolean
        get() = synchronized(lock) { startup != null }

    // A copy for each caller, since a cancelled caller cancels the future it awaits
    override suspend fun start(): CefApp = startup().copy().await()

    private fun startup(): CompletableFuture<CefApp> = synchronized(lock) {
        startup ?: CompletableFuture<CefApp>().also { started ->
            startup = started
            try {
                begin(started)
            } catch (e: JcefStartException) {
                started.completeExceptionally(e)
            } catch (e: RuntimeException) {
                started.completeExceptionally(JcefStartException("Chromium didn't start", e))
            }
        }
    }

    private fun begin(started: CompletableFuture<CefApp>) {
        if (hasEnded) throw JcefStartException("Chromium has shut down with the app")
        if (presence !is JcefPresence.Present) throw JcefStartException("This Java runtime has no JCEF")
        val display = host.display ?: throw JcefStartException("The app's windows weren't set up for Chromium")
        val clock = TimeSource.Monotonic.markNow()
        started.orTimeout(START_TIMEOUT.inWholeSeconds, TimeUnit.SECONDS).whenComplete { _, error ->
            if (error == null) {
                logcat(LogPriority.INFO) { "Chromium started in ${clock.elapsedNow().inWholeMilliseconds} ms" }
            } else {
                logcat(LogPriority.ERROR, error) { "Chromium didn't start" }
            }
        }
        Runtime.getRuntime().addShutdownHook(Thread(::shutdown, "Chromium shutdown"))
        ChromiumProfileData(host.dataDirectory).deleteIfAsked()
        val initialized = CompletableFuture<CefApp>()
        initialized
            .thenCompose { app -> cookies.attach(Chromium.cookieManager()).thenApply { app } }
            .whenComplete { app, error ->
                if (error == null) {
                    started.complete(app)
                } else {
                    started.completeExceptionally(JcefStartException("Chromium didn't get the app's cookies", error))
                }
            }
        Chromium.start(
            switches = { runtimeSwitches -> chromiumSwitches(runtimeSwitches, display, host.deviceScaleFactor) },
            dataDirectory = host.dataDirectory,
            started = initialized,
        )
    }

    override fun shutdown() {
        val started = synchronized(lock) {
            if (hasEnded) return
            hasEnded = true
            startup
        } ?: return
        // Shutting Chromium down while it initialises leaves its threads racing, so a start under way ends first
        runCatching { started.get(START_TIMEOUT.inWholeSeconds, TimeUnit.SECONDS) }
        cookies.detach()
        val clock = TimeSource.Monotonic.markNow()
        if (Chromium.shutDown(SHUTDOWN_TIMEOUT)) {
            logcat(LogPriority.INFO) { "Chromium shut down in ${clock.elapsedNow().inWholeMilliseconds} ms" }
        } else {
            logcat(LogPriority.WARN) { "Chromium didn't shut down in $SHUTDOWN_TIMEOUT" }
        }
    }

    private companion object {
        val START_TIMEOUT = 30.seconds

        // JCEF waits a second after the last browser closes before it shuts Chromium down
        val SHUTDOWN_TIMEOUT = 5.seconds
    }
}
