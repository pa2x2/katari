package mihon.webview.jcef.page

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withTimeoutOrNull
import logcat.LogPriority
import mihon.core.common.web.ChallengePageEvents
import mihon.webview.jcef.cookie.ChromiumCookieMirror
import mihon.webview.jcef.devtools.DevToolsSession
import mihon.webview.jcef.runtime.JcefRuntime
import mihon.webview.jcef.runtime.JcefStartException
import okhttp3.HttpUrl
import org.cef.CefApp
import org.cef.CefBrowserSettings
import org.cef.CefClient
import org.cef.browser.CefBrowser
import org.cef.browser.CefRendering
import tachiyomi.core.common.util.system.logcat
import java.awt.Component
import java.util.concurrent.Executor
import kotlin.time.Duration.Companion.seconds
import kotlin.time.TimeSource

/**
 * One challenge page: a windowless Chromium browser that nothing draws, which loads [url] with [headers] as
 * [userAgent] in all its frames ([PageUserAgent]) once the challenge's listener is in place ([ChallengeListener]), and
 * tells [events] what happens until [run] is cancelled, which closes it. What it tells and when is [ChallengeClient]'s.
 *
 * @param executor where the browser's client is freed once Chromium has closed the browser, after [run] ended.
 */
internal class ChallengeBrowser(
    private val url: HttpUrl,
    private val headers: Map<String, String>,
    private val userAgent: String,
    private val events: ChallengePageEvents,
    private val runtime: JcefRuntime,
    private val cookies: ChromiumCookieMirror,
    private val executor: Executor,
) {
    // JCEF takes a browser to be shown in an AWT component; one in no window keeps AWT from acting for the page
    private val anchor = object : Component() {}

    /** Runs the page until cancelled, or until Chromium is lost; Chromium starts first if it hasn't. */
    suspend fun run() {
        val app = try {
            runtime.start()
        } catch (e: JcefStartException) {
            logcat(LogPriority.ERROR, e) { "A challenge page couldn't open" }
            events.onPageGone()
            return
        }
        logcat(LogPriority.INFO) { "Opening a challenge page for ${url.host}" }
        val clock = TimeSource.Monotonic.markNow()
        try {
            coroutineScope { show(app, scope = this) }
        } finally {
            logcat(LogPriority.INFO) {
                "Closed the challenge page for ${url.host} after ${clock.elapsedNow().inWholeMilliseconds} ms"
            }
        }
    }

    private suspend fun show(app: CefApp, scope: CoroutineScope) {
        val created = CompletableDeferred<CefBrowser>()
        val client = app.createClient()
        ChallengeClient(
            headers = headers,
            events = events,
            cookies = cookies,
            scope = scope,
            created = created,
            onClosed = { executor.execute(client::dispose) },
        ).attachTo(client)
        val listener = ChallengeListener(url, events).also { it.attachTo(client) }
        val browser = createBrowser(client)
        var devTools: DevToolsSession? = null
        try {
            browser.createImmediately()
            val session = DevToolsSession(created.await()).also { devTools = it }
            withTimeoutOrNull(SETUP_TIMEOUT) {
                PageUserAgent(session, userAgent).apply()
                listener.listen(session)
            } ?: logcat(LogPriority.WARN) { "The challenge page wasn't set up in time" }
            browser.loadURL(url.toString())
            // The browser goes with Chromium's server, which then tells JCEF nothing of it
            runtime.isLost.first { it }
            logcat(LogPriority.ERROR) { "A challenge page ended with Chromium" }
            events.onPageGone()
        } finally {
            devTools?.close()
            browser.close(true)
            // What the page changed until it closed reaches the app's store
            cookies.readBack()
        }
    }

    private fun createBrowser(client: CefClient): CefBrowser {
        val browser = client.createBrowser(
            BLANK_PAGE,
            CefRendering.CefRenderingWithHandler(UnpaintedView(), anchor),
            false,
            null,
            CefBrowserSettings(),
        )
        // Otherwise Chromium waits for the app to agree to the browser closing, and its shutdown waits for it
        browser.setCloseAllowed()
        return browser
    }

    companion object {
        /** The page a browser starts on, which gives it a page the user agent is set on before the first load. */
        const val BLANK_PAGE = "about:blank"

        private val SETUP_TIMEOUT = 5.seconds
    }
}
