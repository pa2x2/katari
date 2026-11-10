package app.katari.webpages.host

import app.katari.core.common.web.ExtensionPageEvents
import app.katari.core.common.web.ExtensionPageKey
import app.katari.core.common.web.ExtensionPageRequest
import app.katari.core.common.web.ExtensionPageResponse
import app.katari.core.common.web.ExtensionPageSettings
import app.katari.core.common.web.ExtensionPageTouch
import app.katari.core.common.web.ExtensionPages
import eu.kanade.tachiyomi.source.entry.webpage.WebPageException
import eu.kanade.tachiyomi.source.entry.webpage.WebPageKey
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonPrimitive
import kotlin.coroutines.resume

/**
 * One page on a hidden Chromium page. The page's posts come through a bridge named after the channel, whose
 * `postMessage` they call.
 */
internal class DesktopHostPage(
    pages: ExtensionPages,
    settings: HostPageSettings,
    private val events: HostPageEvents,
) : HostPage {
    @Volatile
    private var load: CompletableDeferred<Unit>? = null

    private val page = pages.open(
        ExtensionPageSettings(
            userAgent = settings.userAgent ?: pages.defaultUserAgent,
            javaScriptEnabled = true,
            blocksImages = false,
        ),
        Events(settings.channel),
    )

    init {
        page.resize(settings.width, settings.height)
        // Before the start script, which the bridge must be there for
        page.addBridge(settings.channel, setOf(POST))
        page.addStartScript(settings.startScript)
    }

    override suspend fun load(url: String, headers: Map<String, String>) {
        val finished = CompletableDeferred<Unit>()
        load = finished
        page.load(url, headers)
        finished.await()
    }

    override suspend fun evaluate(script: String): String = suspendCancellableCoroutine { continuation ->
        page.evaluate(script) { value -> continuation.resume(value) }
    }

    override suspend fun touch(step: HostTouch, x: Float, y: Float) = page.touch(
        ExtensionPageTouch(
            pointerId = 0,
            phase = when (step) {
                HostTouch.DOWN -> ExtensionPageTouch.Phase.PRESSED
                HostTouch.MOVE -> ExtensionPageTouch.Phase.MOVED
                HostTouch.UP -> ExtensionPageTouch.Phase.RELEASED
            },
            tool = ExtensionPageTouch.Tool.FINGER,
            x = x,
            y = y,
            radiusX = 0f,
            radiusY = 0f,
            rotation = 0f,
            pressure = 0f,
        ),
    )

    override suspend fun insertText(text: String) = page.insertText(text)

    override suspend fun press(key: WebPageKey) = page.press(
        when (key) {
            WebPageKey.ENTER -> ExtensionPageKey.ENTER
            WebPageKey.TAB -> ExtensionPageKey.TAB
            WebPageKey.ESCAPE -> ExtensionPageKey.ESCAPE
            WebPageKey.BACKSPACE -> ExtensionPageKey.BACKSPACE
            WebPageKey.ARROW_UP -> ExtensionPageKey.ARROW_UP
            WebPageKey.ARROW_DOWN -> ExtensionPageKey.ARROW_DOWN
            WebPageKey.ARROW_LEFT -> ExtensionPageKey.ARROW_LEFT
            WebPageKey.ARROW_RIGHT -> ExtensionPageKey.ARROW_RIGHT
        },
    )

    // Chromium has no pause for a page nobody sees that it comes back from whole, so an idle page runs on
    override suspend fun setIdle(idle: Boolean) = Unit

    // Closing waits for the page's cookies to reach the app's store
    override suspend fun close() = withContext(Dispatchers.IO) { page.close() }

    private inner class Events(private val channel: String) : ExtensionPageEvents {
        override fun onPageStarted(url: String) = Unit

        override fun onPageFinished(url: String) {
            load?.complete(Unit)
        }

        override fun onTitleChanged(title: String) = Unit

        override fun onLoadFailed(url: String, isForMainFrame: Boolean, errorCode: Int, description: String) {
            if (isForMainFrame) load?.completeExceptionally(WebPageException("$url couldn't be loaded: $description"))
        }

        override fun interceptRequest(request: ExtensionPageRequest): ExtensionPageResponse? = null

        override fun onConsoleMessage(
            level: ExtensionPageEvents.ConsoleLevel,
            message: String,
            sourceId: String,
            lineNumber: Int,
        ) = Unit

        override fun onBridgeCall(name: String, method: String, arguments: String) {
            if (name != channel || method != POST) return
            runCatching { Json.parseToJsonElement(arguments).jsonArray.firstOrNull()?.jsonPrimitive?.contentOrNull }
                .getOrNull()
                ?.let(events::onMessage)
        }

        override fun onPageGone(crashed: Boolean) {
            load?.completeExceptionally(WebPageException("The page was closed"))
            events.onGone()
        }
    }

    private companion object {
        const val POST = "postMessage"
    }
}
