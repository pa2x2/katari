package app.katari.webview.jcef.page.extension

import app.katari.core.common.web.ExtensionPage
import app.katari.core.common.web.ExtensionPageEvents
import app.katari.core.common.web.ExtensionPageResponse
import app.katari.core.common.web.ExtensionPageSettings
import app.katari.core.common.web.ExtensionPageTouch
import app.katari.webview.engine.cookie.ChromiumCookieMirror
import app.katari.webview.engine.devtools.DevToolsSession
import app.katari.webview.engine.devtools.PageUserAgent
import app.katari.webview.engine.devtools.evaluate
import app.katari.webview.engine.page.ExtensionBridges
import app.katari.webview.engine.page.PendingLoads
import app.katari.webview.jcef.devtools.JcefDevToolsSession
import app.katari.webview.jcef.page.HiddenBrowser
import app.katari.webview.jcef.runtime.JcefRuntime
import app.katari.webview.jcef.runtime.JcefStartException
import app.katari.webview.jcef.runtime.RunningChromium
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import logcat.LogPriority
import org.cef.browser.CefBrowser
import tachiyomi.core.common.util.system.logcat
import java.util.Base64
import java.util.concurrent.Executor
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.time.Duration.Companion.seconds
import kotlin.time.TimeSource

/**
 * One extension page: a [hidden browser][HiddenBrowser] that opens with the first call made on the page, starting
 * Chromium if it hasn't, and runs the calls in the order they were made on the pages' thread ([scope]) until the page
 * is closed or Chromium is lost. What Chromium tells of it reaches [events] through [ExtensionClient]. A dialog of the
 * app's can show it meanwhile ([display]).
 *
 * The page's user agent holds in all its frames ([PageUserAgent]). It sends the cookies the [cookies] mirror handed
 * Chromium, and those it sets are read back into the app's store before a finished load is told and before [close]
 * returns.
 *
 * @param executor where the browser's client is freed once Chromium has closed the browser, after the page closed.
 */
internal class ExtensionBrowser(
    settings: ExtensionPageSettings,
    private val events: ExtensionPageEvents,
    private val runtime: JcefRuntime,
    private val cookies: ChromiumCookieMirror,
    private val executor: Executor,
    scope: CoroutineScope,
) : ExtensionPage {
    private val calls = Channel<Call>(Channel.UNLIMITED)
    private val closed = AtomicBoolean(false)
    private val bridges = ExtensionBridges()
    private val loads = PendingLoads()

    /** Where the browser paints: nowhere, or a dialog of the app's that shows the page. */
    val display = ExtensionPageDisplay()

    @Volatile
    private var settings = settings

    // The Chromium the page opened a browser on, which may have set cookies
    @Volatile
    private var chromium: RunningChromium? = null

    private val job = scope.launch { run() }

    val isOpen: Boolean
        get() = !closed.get()

    val blocksImages: Boolean
        get() = settings.blocksImages

    /** Whether events of the main frame at [url] are told: none once closed, and none of the page it starts on. */
    fun tells(url: String): Boolean = isOpen && url != HiddenBrowser.BLANK_PAGE

    override fun apply(settings: ExtensionPageSettings) {
        this.settings = settings
        // Settings alone open no page; an open one takes them on in turn with the calls made before
        call(opens = false) { applySettings() }
    }

    override fun load(url: String, headers: Map<String, String>) {
        call {
            loads.expectHeaders(url, headers)
            browser.loadURL(url)
        }
    }

    override fun loadContent(baseUrl: String?, content: String, mimeType: String, encoding: String) {
        call {
            val bytes = content.toByteArray(runCatching { charset(encoding) }.getOrDefault(Charsets.UTF_8))
            if (baseUrl == null || (!baseUrl.startsWith("http://") && !baseUrl.startsWith("https://"))) {
                // A document of no origin, as Android's web view shows one without a base URL of the web
                val data = Base64.getEncoder().encodeToString(bytes)
                browser.loadURL("data:$mimeType;charset=$encoding;base64,$data")
            } else {
                loads.expectContent(baseUrl) {
                    ExtensionPageResponse(
                        statusCode = 200,
                        reasonPhrase = "OK",
                        mimeType = mimeType,
                        encoding = encoding,
                        headers = emptyMap(),
                        body = bytes.inputStream(),
                    )
                }
                browser.loadURL(baseUrl)
            }
        }
    }

    override fun evaluate(script: String, result: ((String) -> Unit)?) {
        call {
            // Sent now, so later calls run after it; its value comes when the renderer has it
            pageScope.launch(start = CoroutineStart.UNDISPATCHED) {
                val value = devTools.evaluate(script) ?: NO_VALUE
                if (isOpen) result?.invoke(value)
            }
        }
    }

    override fun addBridge(name: String, methods: Set<String>) {
        call { bridges.add(name, methods, devTools) }
    }

    override fun removeBridge(name: String) {
        call { bridges.remove(name, devTools) }
    }

    override fun touch(touch: ExtensionPageTouch) {
        // Through the browser's own input, so the page's events are trusted as a person's are
        call { browser.sendTouchEvent(touch.toCefTouchEvent()) }
    }

    override fun stopLoading() {
        call { browser.stopLoad() }
    }

    override fun reload() {
        call { browser.reload() }
    }

    override fun close() {
        if (!closed.compareAndSet(false, true)) return
        calls.close()
        job.cancel()
        // An extension may take its view's end for its page's cookies being in the app's store, as on Android, where
        // the web view keeps them in the app's own. Lost Chromium has none to read.
        if (chromium?.isLost == false) {
            runBlocking { withTimeoutOrNull(COOKIES_TIMEOUT) { cookies.awaitReadBack() } }
                ?: logcat(LogPriority.WARN) { "A closed extension page's cookies weren't read in $COOKIES_TIMEOUT" }
        }
    }

    private fun call(opens: Boolean = true, block: suspend OpenPage.() -> Unit) {
        if (isOpen) calls.trySend(Call(opens, block))
    }

    private suspend fun run() {
        // A page closed before a call that opens it has nothing to close
        var first: Call
        do {
            first = calls.receiveCatching().getOrNull() ?: return
        } while (!first.opens)
        val chromium = try {
            runtime.start()
        } catch (e: JcefStartException) {
            logcat(LogPriority.ERROR, e) { "An extension page couldn't open" }
            if (isOpen) events.onPageGone(crashed = true)
            return
        }
        this.chromium = chromium
        logcat(LogPriority.INFO) { "Opening an extension page" }
        val clock = TimeSource.Monotonic.markNow()
        try {
            coroutineScope { show(chromium, first, pageScope = this) }
        } finally {
            logcat(LogPriority.INFO) { "Closed an extension page after ${clock.elapsedNow().inWholeMilliseconds} ms" }
        }
    }

    private suspend fun show(chromium: RunningChromium, first: Call, pageScope: CoroutineScope) {
        val created = CompletableDeferred<CefBrowser>()
        val client = chromium.app.createClient()
        ExtensionClient(
            page = this,
            events = events,
            bridges = bridges,
            loads = loads,
            cookies = cookies,
            scope = pageScope,
            created = created,
            onClosed = { executor.execute(client::dispose) },
        ).attachTo(client)
        val browser = HiddenBrowser.create(client, display)
        display.browser = browser
        var devTools: DevToolsSession? = null
        try {
            browser.createImmediately()
            val session = JcefDevToolsSession(created.await()).also { devTools = it }
            val page = OpenPage(browser, session, PageUserAgent(session, settings.userAgent), pageScope)
            withTimeoutOrNull(SETUP_TIMEOUT) {
                page.userAgent.apply()
                // The bridges' scripts are added through the Page domain
                session.call("Page.enable") ?: logcat(LogPriority.WARN) { "An extension page's bridges won't run" }
                page.applySettings()
            } ?: logcat(LogPriority.WARN) { "An extension page wasn't set up in time" }
            pageScope.launch {
                // The browser goes with Chromium's server, which then tells JCEF nothing of it
                chromium.awaitLost()
                logcat(LogPriority.ERROR) { "An extension page ended with Chromium" }
                if (isOpen) events.onPageGone(crashed = true)
                close()
            }
            first.block(page)
            for (call in calls) call.block(page)
        } finally {
            display.browser = null
            devTools?.close()
            browser.close(true)
            // What the page changed until it closed reaches the app's store
            cookies.readBack()
        }
    }

    private class Call(val opens: Boolean, val block: suspend OpenPage.() -> Unit)

    /** The page once its browser is open, which the calls made on it run on. */
    private inner class OpenPage(
        val browser: CefBrowser,
        val devTools: DevToolsSession,
        val userAgent: PageUserAgent,
        val pageScope: CoroutineScope,
    ) {
        // Chromium runs a page's scripts unless told otherwise
        private var scriptsEnabled = true

        /** Takes on the page's latest settings: its user agent, and whether scripts run. */
        fun applySettings() {
            val latest = settings
            userAgent.update(latest.userAgent)
            if (latest.javaScriptEnabled != scriptsEnabled) {
                scriptsEnabled = latest.javaScriptEnabled
                devTools.send(
                    "Emulation.setScriptExecutionDisabled",
                    buildJsonObject { put("value", !latest.javaScriptEnabled) },
                )
            }
        }
    }

    private companion object {
        val SETUP_TIMEOUT = 5.seconds
        val COOKIES_TIMEOUT = 2.seconds

        // What Android's web view hands a script's callback when the script has no value
        const val NO_VALUE = "null"
    }
}
