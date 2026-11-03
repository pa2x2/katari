package mihon.webview.jcef.page.extension

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import logcat.LogPriority
import mihon.webview.jcef.devtools.DevToolsSession
import tachiyomi.core.common.util.system.logcat
import java.security.SecureRandom
import java.util.Base64

/**
 * The objects an extension's web view gives its pages' scripts (`addJavascriptInterface`), whose calls reach the
 * extension. A script added at the start of every document of the page's frames, before the page's own scripts, puts
 * an object of each name on `window`; its methods log their name and arguments to the console behind a secret of this
 * page's, which Chromium hands the browser's client ([read]). The script keeps the console's and JSON's
 * functions as they were before the page's own scripts ran.
 *
 * The page's scripts see and may call the objects, as they do on Android, where any frame may call them. Unlike
 * Android's, a call returns nothing and doesn't wait for the extension. It needs no DevTools binding, which Chromium
 * gives only while the Runtime domain is enabled, which pages can tell.
 */
internal class ExtensionBridges {
    private val secret = ByteArray(SECRET_BYTES)
        .also(SecureRandom()::nextBytes)
        .let { Base64.getUrlEncoder().withoutPadding().encodeToString(it) + ":" }

    // Only on the page's thread
    private val methods = LinkedHashMap<String, Set<String>>()
    private var script: String? = null

    /** Gives the documents that load from now on the bridge [name] with [methods] instead of what it had. */
    suspend fun add(name: String, methods: Set<String>, session: DevToolsSession) {
        this.methods[name] = methods
        update(session)
    }

    suspend fun remove(name: String, session: DevToolsSession) {
        if (methods.remove(name) != null) update(session)
    }

    /**
     * Hands [onCall] the bridge call the console [message] carries; false where it is one of the page's own console
     * messages.
     */
    fun read(message: String, onCall: (BridgeCall) -> Unit): Boolean {
        if (!message.startsWith(secret)) return false
        val call = runCatching { Json.parseToJsonElement(message.removePrefix(secret)).jsonArray }.getOrNull()
        val name = call?.getOrNull(0)?.jsonPrimitive?.contentOrNull
        val method = call?.getOrNull(1)?.jsonPrimitive?.contentOrNull
        val arguments = call?.getOrNull(2) as? JsonArray
        if (name != null && method != null && arguments != null) {
            onCall(BridgeCall(name, method, arguments.toString()))
        } else {
            logcat(LogPriority.WARN) { "An extension page's bridge call couldn't be read" }
        }
        return true
    }

    private suspend fun update(session: DevToolsSession) {
        script?.let { identifier ->
            session.call("Page.removeScriptToEvaluateOnNewDocument", buildJsonObject { put("identifier", identifier) })
        }
        script = null
        if (methods.isEmpty()) return
        val added = session.call(
            "Page.addScriptToEvaluateOnNewDocument",
            buildJsonObject { put("source", bridgeScript()) },
        )
        script = added?.get("identifier")?.jsonPrimitive?.contentOrNull
        if (script == null) logcat(LogPriority.WARN) { "An extension page's bridges weren't added" }
    }

    private fun bridgeScript(): String {
        val bridges = buildJsonObject {
            methods.forEach { (name, methods) -> put(name, JsonArray(methods.map(::JsonPrimitive))) }
        }
        return """
            (() => {
                const post = console.debug.bind(console);
                const stringify = JSON.stringify;
                const map = Array.prototype.map;
                const secret = ${JsonPrimitive(secret)};
                const bridges = $bridges;
                for (const name of Object.keys(bridges)) {
                    const bridge = {};
                    for (const method of bridges[name]) {
                        bridge[method] = function (...values) {
                            const passed = map.call(values, (value) => value === undefined ? null : value);
                            let call;
                            try {
                                call = stringify([name, method, passed]);
                            } catch (e) {
                                // Such as an object that refers to itself
                                call = stringify([name, method, map.call(passed, () => null)]);
                            }
                            post(secret + call);
                        };
                    }
                    window[name] = bridge;
                }
            })();
        """.trimIndent()
    }

    /** A call of [method] of the bridge [name] with [arguments], a JSON array. */
    data class BridgeCall(val name: String, val method: String, val arguments: String)

    private companion object {
        const val SECRET_BYTES = 16
    }
}
