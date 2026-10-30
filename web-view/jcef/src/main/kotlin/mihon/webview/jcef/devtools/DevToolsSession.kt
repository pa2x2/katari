package mihon.webview.jcef.devtools

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.future.await
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonObject
import logcat.LogPriority
import org.cef.browser.CefBrowser
import org.cef.browser.CefDevToolsClient
import tachiyomi.core.common.util.system.logcat

/**
 * A DevTools protocol session with a browser, for what Chromium offers through it but JCEF's own API doesn't. The
 * browser must have been created. What a call sets, such as a user agent, holds while the session is open. Chromium
 * runs a session's methods in the order they were sent.
 *
 * JCEF loses the answer to a method Chromium answers before the method's sending returns, as it does for methods the
 * browser process answers on the spot, such as Page.resetNavigationHistory, and waiting for one waits forever. Such
 * methods are [sent][send]; methods the page's renderer or a background task answers are [called][call].
 */
internal class DevToolsSession(browser: CefBrowser) {
    private val client: CefDevToolsClient = browser.devToolsClient

    /** Sends [method] with [parameters] without waiting for Chromium's answer. */
    fun send(method: String, parameters: JsonObject = JsonObject(emptyMap())) {
        client.executeDevToolsMethod(method, parameters.toString())
    }

    /** Calls [method] with [parameters]; its result, or null where Chromium refused it or didn't answer. */
    suspend fun call(method: String, parameters: JsonObject = JsonObject(emptyMap())): JsonObject? {
        val response = try {
            client.executeDevToolsMethod(method, parameters.toString()).await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            logcat(LogPriority.WARN, e) { "Chromium didn't answer $method" }
            return null
        }
        return runCatching { Json.parseToJsonElement(response).jsonObject }.getOrNull()
    }

    fun close() = client.close()
}
