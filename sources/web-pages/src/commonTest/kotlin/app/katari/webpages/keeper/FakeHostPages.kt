package app.katari.webpages.keeper

import app.katari.webpages.host.HostPage
import app.katari.webpages.host.HostPageEvents
import app.katari.webpages.host.HostPageSettings
import app.katari.webpages.host.HostPages
import eu.kanade.tachiyomi.source.entry.webpage.WebPageKey
import kotlinx.coroutines.delay
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlin.time.Duration.Companion.milliseconds

/** Pages that load nothing and remember what was done to them. */
internal class FakeHostPages : HostPages {
    val opened = mutableListOf<FakeHostPage>()

    val open: List<FakeHostPage>
        get() = opened.filterNot { it.isClosed }

    override suspend fun open(settings: HostPageSettings, events: HostPageEvents): HostPage =
        FakeHostPage(settings, events).also(opened::add)
}

internal class FakeHostPage(private val settings: HostPageSettings, private val events: HostPageEvents) : HostPage {
    var isClosed = false
        private set
    var isIdle = false
        private set

    /** What the page does when tapped, as a site's app answers a tap. */
    var onTap: () -> Unit = {}

    var isFingerDown = false
        private set

    fun lose() = events.onGone()

    /** The page's script received a response to a GET of [url], which the start script posts as a real page's does. */
    fun receive(url: String, body: String) {
        val secret = Regex("""const secret = "(\w+)"""").find(settings.startScript)!!.groupValues[1]
        val entry = JsonObject(
            mapOf(
                "url" to JsonPrimitive(url),
                "method" to JsonPrimitive("GET"),
                "status" to JsonPrimitive(200),
                "body" to JsonPrimitive(body),
            ),
        )
        events.onMessage(JsonArray(listOf(JsonPrimitive(secret), entry)).toString())
    }

    override suspend fun load(url: String, headers: Map<String, String>) = Unit

    // Where an element's middle is asked for, it is there
    override suspend fun evaluate(script: String): String = "[10,20]"

    override suspend fun tap(x: Float, y: Float) {
        isFingerDown = true
        delay(TAP_HELD)
        isFingerDown = false
        onTap()
    }

    override suspend fun insertText(text: String) = Unit

    override suspend fun press(key: WebPageKey) = Unit

    override suspend fun setIdle(idle: Boolean) {
        isIdle = idle
    }

    override suspend fun close() {
        isClosed = true
    }

    companion object {
        val TAP_HELD = 60.milliseconds
    }
}
