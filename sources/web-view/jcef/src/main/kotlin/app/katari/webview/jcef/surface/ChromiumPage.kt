package app.katari.webview.jcef.surface

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import app.katari.webview.jcef.cookie.ChromiumCookieMirror
import app.katari.webview.jcef.data.SiteOrigins
import app.katari.webview.jcef.runtime.JcefRuntime
import app.katari.webview.jcef.runtime.JcefStartException
import eu.kanade.tachiyomi.ui.webview.browser.BrowserEvents
import eu.kanade.tachiyomi.ui.webview.browser.BrowserLoading
import eu.kanade.tachiyomi.ui.webview.browser.BrowserPage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import logcat.LogPriority
import org.cef.CefBrowserSettings
import org.cef.CefClient
import org.cef.browser.CefBrowser
import org.cef.browser.CefRendering
import tachiyomi.core.common.util.system.logcat
import java.awt.Component
import java.awt.event.KeyEvent
import java.awt.event.MouseEvent
import java.awt.event.MouseWheelEvent
import kotlin.time.Duration.Companion.seconds

/**
 * A page in an off-screen Chromium browser of its own, which [frame] gets the pictures of. Chromium starts, and the
 * browser opens and loads [firstUrl], when the page is first shown; the browser outlives the page's time on screen, so a
 * tab shown again keeps its history. Everything here runs on the main thread; [PageClient] brings Chromium's news over.
 */
internal class ChromiumPage(
    private val firstUrl: String,
    val siteHeaders: SiteHeaders,
    private val userAgent: String,
    val scope: CoroutineScope,
    val events: BrowserEvents,
    private val runtime: JcefRuntime,
    val cookies: ChromiumCookieMirror,
    val origins: SiteOrigins,
) : BrowserPage,
    PagePointerTarget {
    val caret = PageCaret(scope) { PageCaret.parse(devTools?.evaluate(PageCaret.SCRIPT)) }
    val frame = PageFrame(caret)
    val input = PageInput()
    val composition = PageComposition { browser }

    override var title by mutableStateOf<String?>(null)
        private set

    override var loading by mutableStateOf<BrowserLoading>(BrowserLoading.Starting)
        private set

    override var canGoBack by mutableStateOf(false)
        private set

    override var canGoForward by mutableStateOf(false)
        private set

    /**
     * The AWT component JCEF takes the browser to be shown in. JCEF refuses the page focus, and keeps Tab leaving the
     * page from the focus handler, for a browser without one; the surface is no AWT component, so one in no window
     * stands for it. Being in no window keeps AWT from acting for the page: JCEF closes a component's window as its
     * browser closes, and moves AWT focus among its neighbours as Tab leaves the page.
     */
    private val focusAnchor = object : Component() {}

    private var opening: Job? = null
    private var client: CefClient? = null
    private var browser: CefBrowser? = null
    private var devTools: PageDevTools? = null
    private var shown = false
    private var closed = false
    private var released = false
    private var focused = false

    /** Whether the browser has left the blank page it starts on; what it reports before that isn't the screen's. */
    var showsSite = false
        private set

    override fun goBack() {
        browser?.goBack()
    }

    override fun goForward() {
        browser?.goForward()
    }

    override fun reload() {
        browser?.reload()
    }

    override suspend fun evaluateJavascript(script: String): String? = devTools?.evaluate(script)

    override fun close() {
        closed = true
        if (!shown) release()
    }

    /** A page this one opened in a new window, with the same site headers and user agent. */
    fun popup(url: String) = ChromiumPage(url, siteHeaders, userAgent, scope, events, runtime, cookies, origins)

    fun onShown() {
        shown = true
        if (opening == null && !closed) opening = scope.launch { open() }
    }

    fun onHidden() {
        shown = false
        if (closed) release()
    }

    fun setFocus(focused: Boolean) {
        this.focused = focused
        browser?.setFocus(focused)
        if (focused) caret.relocate()
    }

    /** The surface is [widthPx] by [heightPx] at [scale] pixels per CSS pixel. */
    fun layout(widthPx: Int, heightPx: Int, scale: Double) {
        val change = frame.resize(widthPx, heightPx, scale) ?: return
        val browser = browser ?: return
        if (change == ViewChange.SCALE) browser.notifyScreenInfoChanged()
        browser.wasResized(frame.size.width, frame.size.height)
    }

    override val scale: Double
        get() = frame.size.scale

    override fun send(event: MouseEvent) {
        if (event.id == MouseEvent.MOUSE_PRESSED) {
            input.userActed()
            // As a click into a window focuses it; a page that loaded since may have a view of its own unfocused
            if (focused) browser?.setFocus(true)
        }
        browser?.sendMouseEvent(event)
        // A click into another field may leave the selection as it was, at the start of an empty field, which Chromium
        // then reports no change of
        if (event.id == MouseEvent.MOUSE_RELEASED) caret.relocate()
    }

    override fun send(event: MouseWheelEvent) {
        browser?.sendMouseWheelEvent(event)
    }

    fun send(event: KeyEvent) {
        if (event.id == KeyEvent.KEY_PRESSED) {
            input.onKeyPressed(event, intoPopup = frame.popupBounds != null)
        }
        browser?.sendKeyEvent(event)
    }

    private suspend fun open() {
        val app = try {
            runtime.start()
        } catch (e: JcefStartException) {
            logcat(LogPriority.ERROR, e) { "The web view's page couldn't open" }
            // The screen closes and tells the web view failed, as when a page's renderer crashes
            events.onRenderProcessGone(crashed = true)
            return
        }
        val client = app.createClient().also { PageClient(this).attachTo(it) }
        // A blank page first, which gives the browser a page for the user agent to be set on before the first load
        val browser = client.createBrowser(
            BLANK_PAGE,
            CefRendering.CefRenderingWithHandler(frame, focusAnchor),
            false,
            null,
            CefBrowserSettings().apply { windowless_frame_rate = FRAME_RATE },
        )
        // Otherwise Chromium waits for the app to agree to the browser closing, and its shutdown waits for it
        browser.setCloseAllowed()
        this.client = client
        this.browser = browser
        browser.createImmediately()
        // The browser goes with Chromium's server, which then tells JCEF nothing of it
        runtime.isLost.first { it }
        logcat(LogPriority.ERROR) { "The web view's page ended with Chromium" }
        events.onRenderProcessGone(crashed = true)
    }

    /** Chromium has created the browser. */
    suspend fun onCreated(browser: CefBrowser) {
        if (browser !== this.browser) return
        val devTools = PageDevTools(browser).also { devTools = it }
        withTimeoutOrNull(USER_AGENT_TIMEOUT) { devTools.setUserAgent(userAgent) }
            ?: logcat(LogPriority.WARN) { "The page's user agent wasn't set in time" }
        if (this.browser !== browser) return
        browser.setFocus(focused)
        browser.wasResized(frame.size.width, frame.size.height)
        browser.loadURL(firstUrl)
    }

    /**
     * The main frame went to [url]; whether the screen is told, which it isn't of the blank page the browser starts on.
     */
    fun onAddressChange(url: String): Boolean {
        if (!showsSite) {
            if (url == BLANK_PAGE) return false
            showsSite = true
            scope.launch { forgetBlankPage() }
        }
        // The page a navigation commits may come with a view of its own, which doesn't have focus yet
        browser?.setFocus(focused)
        return true
    }

    // The blank page would otherwise stay in the history, before the first page. Chromium's reports from before the
    // reset, which still count it, have reached the main thread by the time it returns
    private suspend fun forgetBlankPage() {
        if (devTools?.resetHistory() != true) return
        canGoBack = false
        canGoForward = false
    }

    /** Chromium has freed the browser, after [close] or its renderer ending. */
    fun onClosed(browser: CefBrowser) {
        if (browser === this.browser) this.browser = null
        if (this.browser == null) {
            client?.dispose()
            client = null
        }
    }

    fun onLoadingStateChange(isLoading: Boolean, canGoBack: Boolean, canGoForward: Boolean) {
        if (!showsSite) return
        // Chromium doesn't tell JCEF how far a load is
        loading = if (isLoading) BrowserLoading.Loading(progress = null) else BrowserLoading.Finished
        // Such as into a field the page focuses as it loads
        if (!isLoading) caret.relocate()
        this.canGoBack = canGoBack
        this.canGoForward = canGoForward
    }

    fun onTitleChange(title: String) {
        if (showsSite) this.title = title
    }

    private fun release() {
        if (released) return
        released = true
        opening?.cancel()
        devTools?.close()
        devTools = null
        browser?.close(true)
        frame.release()
        // What the page changed until it closed reaches the app's store
        cookies.readBack()
    }

    private companion object {
        // Chromium's default is 30, which makes scrolling look uneven
        const val FRAME_RATE = 60

        const val BLANK_PAGE = "about:blank"

        val USER_AGENT_TIMEOUT = 5.seconds
    }
}
