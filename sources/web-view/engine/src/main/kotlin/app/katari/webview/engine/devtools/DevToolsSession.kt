package app.katari.webview.engine.devtools

import kotlinx.serialization.json.JsonObject

/**
 * A DevTools protocol session with a page, for what Chromium offers through the protocol alone. What a call sets, such
 * as a user agent, holds while the session is open. Chromium runs a session's methods in the order they were sent.
 *
 * Methods the browser process answers on the spot, such as Page.resetNavigationHistory, are [sent][send]: a session may
 * lose such an answer, and waiting for one would then wait forever. Methods the page's renderer or a background task
 * answers are [called][call].
 */
interface DevToolsSession {
    /** Sends [method] with [parameters] without waiting for Chromium's answer. */
    fun send(method: String, parameters: JsonObject = JsonObject(emptyMap()))

    /** Calls [method] with [parameters]; its result, or null where Chromium refused it or didn't answer. */
    suspend fun call(method: String, parameters: JsonObject = JsonObject(emptyMap())): JsonObject?

    /**
     * Calls [listener] with the method and parameters of each event Chromium sends the session, on the session's own
     * threads, until it closes. Chromium sends the events of the domains the session enabled, and those of targets it
     * attached.
     */
    fun onEvent(listener: (method: String, parameters: JsonObject) -> Unit)

    fun close()
}
