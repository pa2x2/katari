package mihon.webview.jcef.devtools

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.boolean
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import logcat.LogPriority
import tachiyomi.core.common.util.system.logcat
import java.util.concurrent.atomic.AtomicInteger

/**
 * One user agent for everything a page runs: its frames, the frames of other sites that run in processes of their own,
 * such as a challenge's, and their workers. JCEF sets a user agent only for the whole of Chromium, and DevTools sets one
 * per target, which leaves a frame of another site with Chromium's own; a challenge solved there earns a clearance that
 * the app's requests, sent as the page, never get through with, and a site that sees two user agents may ask a person
 * to prove they are one.
 *
 * Each new target is held at its start ([waitForDebuggerOnStart][AUTO_ATTACH]) until it has the user agent, and is
 * watched for targets of its own in turn. JCEF's DevTools calls name no session, so the calls to a target go wrapped in
 * `Target.sendMessageToTarget` for each target on the way to it, and its events come back wrapped the same way.
 */
internal class PageUserAgent(private val session: DevToolsSession, userAgent: String) {
    private val messageIds = AtomicInteger()

    @Volatile
    private var userAgent = userAgent

    /** Sets the user agent on the page and on every target it starts from now on. */
    suspend fun apply() {
        session.onEvent { method, parameters -> onEvent(path = emptyList(), method, parameters) }
        session.call("Emulation.setUserAgentOverride", userAgentParameters())
            ?: logcat(LogPriority.WARN) { "The page's user agent wasn't set" }
        session.call("Target.setAutoAttach", AUTO_ATTACH)
            ?: logcat(LogPriority.WARN) { "The page's frames won't get its user agent" }
    }

    /** Sets [userAgent] on the page and on the targets it starts from now on; those it started keep the one before. */
    fun update(userAgent: String) {
        if (userAgent == this.userAgent) return
        this.userAgent = userAgent
        session.send("Emulation.setUserAgentOverride", userAgentParameters())
    }

    // A target's path is the sessions on the way to it from the page
    private fun onEvent(path: List<String>, method: String, parameters: JsonObject) {
        when (method) {
            "Target.attachedToTarget" -> {
                val sessionId = parameters.string("sessionId") ?: return
                val type = parameters["targetInfo"]?.jsonObject?.string("type")
                val target = path + sessionId
                // Workers have no Emulation domain
                val override = if (type == "page" || type == "iframe") "Emulation" else "Network"
                sendTo(target, "$override.setUserAgentOverride", userAgentParameters())
                sendTo(target, "Target.setAutoAttach", AUTO_ATTACH)
                if (parameters["waitingForDebugger"]?.jsonPrimitive?.boolean == true) {
                    sendTo(target, "Runtime.runIfWaitingForDebugger", JsonObject(emptyMap()))
                }
            }
            "Target.receivedMessageFromTarget" -> {
                val sessionId = parameters.string("sessionId") ?: return
                val message = parameters.string("message")
                    ?.let { runCatching { Json.parseToJsonElement(it).jsonObject }.getOrNull() }
                    ?: return
                // Answers to calls carry no method
                val event = message.string("method") ?: return
                val eventParameters = message["params"]?.jsonObject ?: JsonObject(emptyMap())
                onEvent(path + sessionId, event, eventParameters)
            }
        }
    }

    private fun sendTo(path: List<String>, method: String, parameters: JsonObject) {
        var message = buildJsonObject {
            put("id", messageIds.incrementAndGet())
            put("method", method)
            put("params", parameters)
        }
        for (sessionId in path.drop(1).asReversed()) {
            message = buildJsonObject {
                put("id", messageIds.incrementAndGet())
                put("method", SEND_TO_TARGET)
                put("params", targetMessage(sessionId, message))
            }
        }
        session.send(SEND_TO_TARGET, targetMessage(path.first(), message))
    }

    private fun targetMessage(sessionId: String, message: JsonObject) = buildJsonObject {
        put("sessionId", sessionId)
        put("message", message.toString())
    }

    private fun userAgentParameters() = buildJsonObject { put("userAgent", userAgent) }

    private fun JsonObject.string(name: String) = get(name)?.jsonPrimitive?.contentOrNull

    private companion object {
        const val SEND_TO_TARGET = "Target.sendMessageToTarget"

        val AUTO_ATTACH = buildJsonObject {
            put("autoAttach", true)
            put("waitForDebuggerOnStart", true)
            put("flatten", false)
        }
    }
}
