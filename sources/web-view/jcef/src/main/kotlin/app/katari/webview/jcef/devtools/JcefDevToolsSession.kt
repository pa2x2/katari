package app.katari.webview.jcef.devtools

import app.katari.webview.engine.devtools.DevToolsSession
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
 * A [DevToolsSession] with a JCEF browser, which must have been created.
 *
 * JCEF loses the answer to a method Chromium answers before the method's sending returns, as it does for methods the
 * browser process answers on the spot, and waiting for one waits forever.
 */
internal class JcefDevToolsSession(browser: CefBrowser) : DevToolsSession {
    private val client: CefDevToolsClient = browser.devToolsClient

    override fun send(method: String, parameters: JsonObject) {
        client.executeDevToolsMethod(method, parameters.toString())
    }

    override suspend fun call(method: String, parameters: JsonObject): JsonObject? {
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

    override fun onEvent(listener: (method: String, parameters: JsonObject) -> Unit) {
        client.addEventListener { method, parameters ->
            runCatching { Json.parseToJsonElement(parameters).jsonObject }.getOrNull()?.let { listener(method, it) }
        }
    }

    override fun close() = client.close()
}
