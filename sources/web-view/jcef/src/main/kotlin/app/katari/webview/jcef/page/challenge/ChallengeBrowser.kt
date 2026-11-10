package app.katari.webview.jcef.page.challenge

import app.katari.core.common.web.ChallengePageEvents
import app.katari.webview.engine.cookie.ChromiumCookieMirror
import app.katari.webview.engine.devtools.DevToolsSession
import app.katari.webview.engine.devtools.PageUserAgent
import app.katari.webview.jcef.devtools.JcefDevToolsSession
import app.katari.webview.jcef.page.HiddenBrowser
import app.katari.webview.jcef.runtime.JcefRuntime
import app.katari.webview.jcef.runtime.JcefStartException
import app.katari.webview.jcef.runtime.RunningChromium
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withTimeoutOrNull
import logcat.LogPriority
import okhttp3.HttpUrl
import org.cef.browser.CefBrowser
import tachiyomi.core.common.util.system.logcat
import java.util.concurrent.Executor
import kotlin.time.Duration.Companion.seconds
import kotlin.time.TimeSource

/**
 * One challenge page: a [hidden browser][HiddenBrowser], which loads [url] with [headers] as
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
    /** Runs the page until cancelled, or until Chromium is lost; Chromium starts first if it hasn't. */
    suspend fun run() {
        val chromium = try {
            runtime.start()
        } catch (e: JcefStartException) {
            logcat(LogPriority.ERROR, e) { "A challenge page couldn't open" }
            events.onPageGone()
            return
        }
        logcat(LogPriority.INFO) { "Opening a challenge page for ${url.host}" }
        val clock = TimeSource.Monotonic.markNow()
        try {
            coroutineScope { show(chromium, scope = this) }
        } finally {
            logcat(LogPriority.INFO) {
                "Closed the challenge page for ${url.host} after ${clock.elapsedNow().inWholeMilliseconds} ms"
            }
        }
    }

    private suspend fun show(chromium: RunningChromium, scope: CoroutineScope) {
        val created = CompletableDeferred<CefBrowser>()
        val client = chromium.app.createClient()
        ChallengeClient(
            headers = headers,
            events = events,
            cookies = cookies,
            scope = scope,
            created = created,
            onClosed = { executor.execute(client::dispose) },
        ).attachTo(client)
        val listener = ChallengeListener(url, events).also { it.attachTo(client) }
        val browser = HiddenBrowser.create(client)
        var devTools: DevToolsSession? = null
        try {
            browser.createImmediately()
            val session = JcefDevToolsSession(created.await()).also { devTools = it }
            withTimeoutOrNull(SETUP_TIMEOUT) {
                PageUserAgent(session, userAgent).apply()
                listener.listen(session)
            } ?: logcat(LogPriority.WARN) { "The challenge page wasn't set up in time" }
            browser.loadURL(url.toString())
            // The browser goes with Chromium's server, which then tells JCEF nothing of it
            chromium.awaitLost()
            logcat(LogPriority.ERROR) { "A challenge page ended with Chromium" }
            events.onPageGone()
        } finally {
            devTools?.close()
            browser.close(true)
            // What the page changed until it closed reaches the app's store
            cookies.readBack()
        }
    }

    private companion object {
        val SETUP_TIMEOUT = 5.seconds
    }
}
