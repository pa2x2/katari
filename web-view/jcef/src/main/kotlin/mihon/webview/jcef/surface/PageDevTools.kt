package mihon.webview.jcef.surface

import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import mihon.webview.jcef.devtools.DevToolsSession
import org.cef.browser.CefBrowser

/**
 * What a page asks of its browser through the DevTools protocol: a user agent of the page's own, which Android's web
 * view sets per page and JCEF only for the whole process, a script's value, which JCEF's own scripts don't return, and
 * a history without the blank page the browser starts on. The session lasts as long as the browser, as the user agent
 * holds only while it is open.
 */
internal class PageDevTools(browser: CefBrowser) {
    private val session = DevToolsSession(browser)

    /** Sends [userAgent] with every request the page makes and tells it to the page's scripts. */
    suspend fun setUserAgent(userAgent: String) {
        session.call("Emulation.setUserAgentOverride", buildJsonObject { put("userAgent", userAgent) })
    }

    /**
     * Runs [script] in the page's main frame as the page's own scripts run, and returns its value as JSON; null where it
     * threw or the page couldn't run it. A value JSON has no form for, such as `undefined`, is `null`.
     */
    suspend fun evaluate(script: String): String? {
        val response = session.call(
            "Runtime.evaluate",
            buildJsonObject {
                put("expression", script)
                put("returnByValue", true)
            },
        ) ?: return null
        if ("exceptionDetails" in response) return null
        val result = response["result"] as? JsonObject ?: return null
        return (result["value"] ?: JsonNull).toString()
    }

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
