package app.katari.webview.jcef.page.challenge

import app.katari.core.common.web.ChallengePageEvents
import app.katari.webview.engine.devtools.DevToolsSession
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import logcat.LogPriority
import okhttp3.HttpUrl
import org.cef.CefClient
import org.cef.CefSettings
import org.cef.browser.CefBrowser
import org.cef.handler.CefDisplayHandlerAdapter
import tachiyomi.core.common.util.system.logcat
import java.security.SecureRandom
import java.util.Base64

/**
 * Hears the events the challenge on a page posts, and passes them on to [events]. A script in a world of its own, at the
 * start of every document of [url]'s origin in the main frame, hears the challenge's messages to the page and logs
 * their events to the console behind a secret of this page's, which Chromium hands the browser's client. The page's own
 * scripts reach neither the world nor the secret, as on Android's WebView.
 *
 * Not a DevTools binding: Chromium gives a world bindings only while the session's Runtime domain is enabled, which
 * challenges can tell. The world itself is made only while the Page domain is.
 */
internal class ChallengeListener(url: HttpUrl, private val events: ChallengePageEvents) {
    private val origin = if (url.port == HttpUrl.defaultPort(url.scheme)) {
        "${url.scheme}://${url.host}"
    } else {
        "${url.scheme}://${url.host}:${url.port}"
    }

    private val secret = ByteArray(SECRET_BYTES)
        .also(SecureRandom()::nextBytes)
        .let { Base64.getUrlEncoder().withoutPadding().encodeToString(it) + ":" }

    /** Hears the console of the browsers [client] makes; before the browser is created. */
    fun attachTo(client: CefClient) {
        client.addDisplayHandler(console)
    }

    /** Puts the listener in place for the documents to come, through the page's DevTools [session]. */
    suspend fun listen(session: DevToolsSession) {
        session.call("Page.enable")
            ?: logcat(LogPriority.WARN) { "The challenge page's listener won't run" }
        session.call(
            "Page.addScriptToEvaluateOnNewDocument",
            buildJsonObject {
                put("source", listenerScript())
                put("worldName", WORLD)
            },
        ) ?: logcat(LogPriority.WARN) { "The challenge page's listener wasn't added" }
    }

    private val console = object : CefDisplayHandlerAdapter() {
        override fun onConsoleMessage(
            browser: CefBrowser,
            level: CefSettings.LogSeverity,
            message: String,
            source: String?,
            line: Int,
        ): Boolean {
            if (!message.startsWith(secret)) return false
            val event = message.removePrefix(secret)
            logcat(LogPriority.DEBUG) { "A challenge page's challenge posted $event" }
            events.onChallengeEvent(event)
            return true
        }
    }

    private fun listenerScript() = """
        if (window.top === window && location.origin === ${JsonPrimitive(origin)}) {
            addEventListener("message", ({data}) => {
                if (data?.source === "cloudflare-challenge") {
                    console.debug(${JsonPrimitive(secret)} + data.event);
                }
            });
        }
    """.trimIndent()

    private companion object {
        const val WORLD = "katari-challenge"
        const val SECRET_BYTES = 16
    }
}
