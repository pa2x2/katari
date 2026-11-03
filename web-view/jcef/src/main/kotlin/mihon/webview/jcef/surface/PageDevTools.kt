package mihon.webview.jcef.surface

import mihon.webview.jcef.devtools.DevToolsSession
import mihon.webview.jcef.devtools.PageUserAgent
import mihon.webview.jcef.devtools.evaluate
import org.cef.browser.CefBrowser

/**
 * What a page asks of its browser through the DevTools protocol: a user agent of the page's own, which Android's web
 * view sets per page and JCEF only for the whole process, a script's value, which JCEF's own scripts don't return, and
 * a history without the blank page the browser starts on. The session lasts as long as the browser, as the user agent
 * holds only while it is open.
 */
internal class PageDevTools(browser: CefBrowser) {
    private val session = DevToolsSession(browser)

    /**
     * Sends [userAgent] with every request the page and each of its frames make, other sites' frames among them, and
     * tells it to their scripts ([PageUserAgent]).
     */
    suspend fun setUserAgent(userAgent: String) {
        PageUserAgent(session, userAgent).apply()
    }

    suspend fun evaluate(script: String): String? = session.evaluate(script)

    /**
     * Removes every page from the history but the one shown, and returns once Chromium has; false where the page didn't
     * answer after, which leaves that unknown. What Chromium told of the page before the reset has reached the main
     * thread by then too, as JCEF tells it on the thread that answers the page's DevTools calls.
     */
    suspend fun resetHistory(): Boolean {
        session.send("Page.resetNavigationHistory")
        // A method the renderer answers, which Chromium runs after the reset
        return evaluate("0") != null
    }

    fun close() = session.close()
}
