package app.katari.webview.jcef.runtime

import app.katari.webview.engine.cookie.ChromiumCookieMirror
import app.katari.webview.engine.data.ChromiumProfileData
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.future.await
import logcat.LogPriority
import org.cef.CefApp
import tachiyomi.core.common.util.system.logcat
import java.io.IOException
import java.nio.file.Path
import java.util.concurrent.CompletableFuture
import java.util.concurrent.TimeUnit
import kotlin.concurrent.thread
import kotlin.time.Duration.Companion.seconds
import kotlin.time.TimeSource

/**
 * The runtime on the JetBrains Runtime's JCEF: one Chromium at a time, whose start every caller shares, and one
 * shutdown, run by the app as it ends or, failing that, as the process exits. Chromium counts as started once it has
 * the app's [cookies], and hands its own back before it shuts down. The calls into JCEF are [Chromium]'s, which only a
 * runtime with JCEF loads; this class is loaded on any runtime, as the app binds it on all of them.
 */
class JbrJcefRuntime(private val host: JcefHost, private val cookies: ChromiumCookieMirror) : JcefRuntime {
    private val presenceCheck = JcefPresenceCheck(host.chromiumDirectory)

    init {
        // JCEF reads where its server is once, as its classes load, and takes the runtime's own unless told
        if (!presenceCheck.hasRuntimeChromium) {
            System.setProperty(SERVER_PATH, host.chromiumDirectory.resolve(Chromium.SERVER_EXECUTABLE).toString())
        }
    }

    // Asked each time: the app installs Chromium, and removes it, while it runs
    override val presence: JcefPresence
        get() = presenceCheck.check()

    private val lock = Any()

    // All guarded by lock
    private var startup: Startup? = null
    private var hasEnded = false
    private var endsWithProcess = false

    override val isRunning: Boolean
        get() = synchronized(lock) { startup?.isOver == false }

    // A copy for each caller, since a cancelled caller cancels the future it awaits
    override suspend fun start(): RunningChromium = startup().copy().await()

    private fun startup(): CompletableFuture<RunningChromium> = synchronized(lock) {
        val current = startup
        if (current?.isOver == false) return current.started
        // Neither is a start of Chromium, so the next call finds things as they are then
        if (hasEnded) return failed("Chromium has shut down with the app")
        val present = when (val presence = presence) {
            is JcefPresence.Present -> presence
            JcefPresence.NoChromium -> return failed("No Chromium is installed")
            JcefPresence.Absent -> return failed("This Java runtime has no JCEF")
        }
        val next = Startup()
        startup = next
        if (!endsWithProcess) {
            endsWithProcess = true
            Runtime.getRuntime().addShutdownHook(Thread(::shutdown, "Chromium shutdown"))
        }
        // Ending the Chromium before takes seconds, which no caller's thread is held for
        thread(name = "Chromium start", isDaemon = true) { begin(next, present.chromium, previous = current) }
        next.started
    }

    private fun begin(startup: Startup, chromium: Path?, previous: Startup?) {
        val clock = TimeSource.Monotonic.markNow()
        startup.started.orTimeout(START_TIMEOUT.inWholeSeconds, TimeUnit.SECONDS).whenComplete { _, error ->
            if (error == null) {
                logcat(LogPriority.INFO) { "Chromium started in ${clock.elapsedNow().inWholeMilliseconds} ms" }
            } else {
                logcat(LogPriority.ERROR, error) { "Chromium didn't start" }
            }
        }
        try {
            // JCEF keeps a Chromium that failed or was lost until told to let go of it, and would hand it out again
            if (previous != null && !Chromium.shutDown(SHUTDOWN_TIMEOUT)) {
                logcat(LogPriority.WARN) { "The Chromium before didn't shut down in $SHUTDOWN_TIMEOUT" }
            }
            ChromiumProfileData(host.dataDirectory).deleteIfAsked()
            if (chromium != null) placeSharedMemoryLibrary(chromium)
            val initialized = CompletableFuture<CefApp>()
            initialized
                .thenCompose { app -> cookies.attach(Chromium.cookieManager()).thenApply { app } }
                .whenComplete { app, error ->
                    if (error == null) {
                        startup.started.complete(RunningChromium(app, startup.lost))
                    } else {
                        startup.fail(JcefStartException("Chromium didn't get the app's cookies", error))
                    }
                }
            Chromium.start(
                switches = { runtimeSwitches -> chromiumSwitches(runtimeSwitches, host.deviceScaleFactor) },
                dataDirectory = host.dataDirectory,
                userAgent = host.userAgent,
                started = initialized,
                lost = { onLost(startup) },
            )
        } catch (e: JcefStartException) {
            startup.fail(e)
        } catch (e: RuntimeException) {
            startup.fail(JcefStartException("Chromium didn't start", e))
        }
    }

    private fun placeSharedMemoryLibrary(chromium: Path) {
        val library = presenceCheck.sharedMemoryLibrary ?: return
        try {
            placeBesideChromium(library, chromium)
        } catch (e: IOException) {
            throw JcefStartException("Chromium's folder didn't take the runtime's ${library.fileName}", e)
        }
    }

    // JCEF calls it on the thread that found the server gone, and not as the app shuts Chromium down
    private fun onLost(startup: Startup) {
        logcat(LogPriority.ERROR) { "Chromium's server ended; its pages are gone" }
        startup.lost.complete(Unit)
        cookies.forget()
        // A start under way fails now rather than at its time limit
        startup.fail(JcefStartException("Chromium's server ended"))
    }

    override fun stop() {
        val current = synchronized(lock) { if (hasEnded) null else startup } ?: return
        end(current)
        // JCEF tells of no loss when the app itself ends Chromium, so its pages are told here
        current.lost.complete(Unit)
        synchronized(lock) { if (startup === current) startup = null }
    }

    override fun shutdown() {
        val current = synchronized(lock) {
            if (hasEnded) return
            hasEnded = true
            startup
        } ?: return
        end(current)
    }

    private fun end(current: Startup) {
        // Shutting Chromium down while it initialises leaves its threads racing, so a start under way ends first
        runCatching { current.started.get(START_TIMEOUT.inWholeSeconds, TimeUnit.SECONDS) }
        cookies.detach()
        val clock = TimeSource.Monotonic.markNow()
        if (Chromium.shutDown(SHUTDOWN_TIMEOUT)) {
            logcat(LogPriority.INFO) { "Chromium shut down in ${clock.elapsedNow().inWholeMilliseconds} ms" }
        } else {
            logcat(LogPriority.WARN) { "Chromium didn't shut down in $SHUTDOWN_TIMEOUT" }
        }
    }

    private fun failed(message: String): CompletableFuture<RunningChromium> =
        CompletableFuture.failedFuture(JcefStartException(message))

    /** One start of Chromium: over once it failed or its server was lost, which the next start follows. */
    private class Startup {
        val started = CompletableFuture<RunningChromium>()
        val lost = CompletableDeferred<Unit>()

        val isOver: Boolean
            get() = lost.isCompleted || started.isCompletedExceptionally

        fun fail(error: JcefStartException) {
            started.completeExceptionally(error)
        }
    }

    private companion object {
        // JCEF's system property for a server outside the Java runtime
        const val SERVER_PATH = "ALT_CEF_SERVER_PATH"

        val START_TIMEOUT = 30.seconds

        // JCEF waits a second after the last browser closes before it shuts Chromium down
        val SHUTDOWN_TIMEOUT = 5.seconds
    }
}
