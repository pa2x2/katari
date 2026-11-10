package app.katari.webpages.page

import app.katari.webpages.host.HostPage
import app.katari.webpages.host.HostPageEvents
import app.katari.webpages.host.HostPageSettings
import app.katari.webpages.host.HostPages
import app.katari.webpages.host.HostTouch
import eu.kanade.tachiyomi.source.entry.webpage.WebPage
import eu.kanade.tachiyomi.source.entry.webpage.WebPageDirection
import eu.kanade.tachiyomi.source.entry.webpage.WebPageException
import eu.kanade.tachiyomi.source.entry.webpage.WebPageKey
import eu.kanade.tachiyomi.source.entry.webpage.WebPageResponse
import eu.kanade.tachiyomi.source.entry.webpage.WebPageSpec
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Job
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.async
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.selects.select
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.floatOrNull
import kotlinx.serialization.json.jsonPrimitive
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

/**
 * A source's [WebPage] on a platform's hidden page: what the page offers by selector is found and measured by scripts,
 * then done with the platform's own input, and waits ask the page again and again until it shows what they wait for.
 * Every call ends at once when the page is [lost][isLost].
 */
internal class DrivenWebPage private constructor(
    private val host: HostPage,
    private val headers: Map<String, String>,
    private val responses: SharedFlow<WebPageResponse>,
    private val lost: Job,
) : WebPage {
    val isLost: Boolean
        get() = lost.isCompleted

    suspend fun setIdle(idle: Boolean) = host.setIdle(idle)

    suspend fun close() = host.close()

    override suspend fun url(): String = text(run("location.href")) ?: throw WebPageException(UNREADABLE)

    override suspend fun load(url: String) {
        whileOpen {
            withTimeoutOrNull(LOAD_TIMEOUT) { host.load(url, headers) }
                ?: throw WebPageException("$url didn't load in $LOAD_TIMEOUT")
        }
    }

    override suspend fun evaluate(script: String): String {
        val value = run(guarded(script))
        if (value.startsWith(THROWN_START)) {
            val thrown = (runCatching { Json.parseToJsonElement(value) }.getOrNull() as? JsonObject)?.get(THROWN)
            (thrown as? JsonPrimitive)?.contentOrNull?.let { throw WebPageException("The script threw $it") }
        }
        if (value == NO_VALUE) {
            text(run(misreadingOf(script)))?.let { throw WebPageException("The script can't be run: $it") }
        }
        return value
    }

    override suspend fun document(): Document {
        val page = runCatching { Json.parseToJsonElement(run(DOCUMENT)) }.getOrNull() as? JsonArray
        val url = page?.getOrNull(0)?.jsonPrimitive?.contentOrNull
        val html = page?.getOrNull(1)?.jsonPrimitive?.contentOrNull
        if (url == null || html == null) throw WebPageException(UNREADABLE)
        return Jsoup.parse(html, url)
    }

    override suspend fun click(selector: String) {
        var found = run(middleOf(selector))
        // What still moves, as the slide a swipe brought in does, is tapped where it comes to rest
        withTimeoutOrNull(SETTLE_TIMEOUT) {
            while (true) {
                delay(SETTLE_CHECK)
                val now = run(middleOf(selector))
                if (now == found) break
                found = now
            }
        }
        val (x, y) = numbers(found, POINT, selector, "tapped")
        finger {
            host.touch(HostTouch.DOWN, x, y)
            delay(FINGER_RESTS)
            host.touch(HostTouch.UP, x, y)
        }
    }

    override suspend fun swipe(selector: String, direction: WebPageDirection) {
        val (fromX, fromY, toX, toY) = numbers(run(swipeAcross(selector, direction)), PATH, selector, "swiped")
        finger {
            host.touch(HostTouch.DOWN, fromX, fromY)
            delay(FINGER_RESTS)
            for (step in 1..SWIPE_STEPS) {
                val done = step.toFloat() / SWIPE_STEPS
                host.touch(HostTouch.MOVE, fromX + (toX - fromX) * done, fromY + (toY - fromY) * done)
                delay(SWIPE_STEP)
            }
            // Lifted at speed the finger would fling the page on, and the browser takes the next tap for the fling's
            // stop and gives the page no click
            delay(FINGER_STOPS)
            host.touch(HostTouch.UP, toX, toY)
        }
    }

    override suspend fun type(selector: String, text: String) {
        when (run(focus(selector))) {
            "true" -> whileOpen { host.insertText(text) }
            "false" -> throw WebPageException("$selector doesn't take the focus, so nothing can be typed into it")
            else -> throw unmatched(selector)
        }
        // The text reaches the page apart from the scripts asked of it, so one run next could find the field as it was
        if (!shows(TEXT_TIMEOUT) { run(tookText(text)) == "true" }) {
            throw WebPageException("$selector took no text")
        }
    }

    override suspend fun press(key: WebPageKey) = whileOpen { host.press(key) }

    override suspend fun scrollTo(selector: String) {
        if (run(scrollIntoView(selector)) != "true") throw unmatched(selector)
    }

    override suspend fun scrollToEnd() {
        run(SCROLL_TO_END)
    }

    override suspend fun awaitSelector(selector: String, timeout: Duration) {
        val present = "document.querySelector(${quoted(selector)}) !== null"
        await(timeout, "for $selector") { run(present) == "true" }
    }

    override suspend fun awaitUrl(timeout: Duration, matches: (url: String) -> Boolean) {
        await(timeout, "for the page's address to change") { matches(url()) }
    }

    override suspend fun responseTo(
        matches: (response: WebPageResponse) -> Boolean,
        timeout: Duration,
        action: suspend () -> Unit,
    ): WebPageResponse = whileOpen {
        // Subscribed before the action runs, as the flow keeps nothing for a late subscriber
        val response = async(start = CoroutineStart.UNDISPATCHED) { responses.first(matches) }
        action()
        withTimeoutOrNull(timeout) { response.await() }
            ?: run {
                response.cancel()
                throw WebPageException("The page received no response the source waited $timeout for")
            }
    }

    private suspend fun run(script: String): String = whileOpen { host.evaluate(script) }

    private suspend fun finger(moves: suspend () -> Unit) {
        run(WATCH_LIFT)
        // A call cancelled between the press and the lift would leave a finger on the page for the next call to find
        whileOpen { withContext(NonCancellable) { moves() } }
        // The finger reaches the page apart from the scripts asked of it, so one run next could come before the lift
        // and find nothing moved yet. A lift that loads another document is never seen, nor is one waited for long.
        shows(LIFT_TIMEOUT) { run(LIFT_SEEN) == "true" }
    }

    /** The [count] numbers a script found for [selector], or the failure of the reason it put after them. */
    private fun numbers(found: String, count: Int, selector: String, deed: String): List<Float> {
        val all = (runCatching { Json.parseToJsonElement(found) }.getOrNull() as? JsonArray)
            ?.filterIsInstance<JsonPrimitive>()
            ?: throw unmatched(selector)
        when (all.lastOrNull()?.takeIf { it.isString }?.content) {
            OUT_OF_VIEW -> throw WebPageException("$selector can't be $deed: scrolling doesn't bring it into view")
            COVERED -> throw WebPageException("$selector can't be $deed: something else on the page lies over it")
        }
        return all.mapNotNull { it.floatOrNull }.takeIf { it.size == count } ?: throw unmatched(selector)
    }

    private suspend fun await(timeout: Duration, what: String, isThere: suspend () -> Boolean) {
        if (!shows(timeout, isThere)) {
            throw WebPageException("The page didn't get there in $timeout: the source waited $what")
        }
    }

    // The page's loss ends the pauses between two askings too
    private suspend fun shows(timeout: Duration, isThere: suspend () -> Boolean): Boolean = whileOpen {
        withTimeoutOrNull(timeout) {
            while (!isThere()) delay(ASK_AGAIN)
        }
    } != null

    private suspend fun <T> whileOpen(block: suspend CoroutineScope.() -> T): T = coroutineScope {
        val work = async(block = block)
        select {
            work.onAwait { it }
            lost.onJoin {
                work.cancel()
                throw WebPageException("The page was closed before the source finished with it")
            }
        }
    }

    private fun text(json: String): String? =
        runCatching { Json.parseToJsonElement(json).jsonPrimitive }.getOrNull()?.takeIf { it.isString }?.content

    private fun unmatched(selector: String) = WebPageException("Nothing on the page matches $selector")

    companion object {
        private val LOAD_TIMEOUT = 30.seconds
        private val TEXT_TIMEOUT = 5.seconds
        private val LIFT_TIMEOUT = 2.seconds
        private val SETTLE_TIMEOUT = 2.seconds

        // Longer than two of a page's frames, so that an element on its way reads differently each time
        private val SETTLE_CHECK = 80.milliseconds
        private val FINGER_RESTS = 60.milliseconds
        private val SWIPE_STEP = 16.milliseconds
        private val FINGER_STOPS = 150.milliseconds
        private const val SWIPE_STEPS = 12
        private const val POINT = 2
        private const val PATH = 4
        private const val NO_VALUE = "null"
        private const val THROWN = "thrown by the evaluated script"
        private val THROWN_START = "{${JsonPrimitive(THROWN)}:"
        private val ASK_AGAIN = 100.milliseconds
        private const val UNREADABLE = "The page couldn't be read"
        private const val RESPONSES_HELD = 64

        private const val DOCUMENT = "[location.href, document.documentElement.outerHTML]"
        private const val SCROLL_TO_END =
            "window.scrollTo({ top: document.documentElement.scrollHeight, behavior: 'instant' })"

        // In a block of its own the script's value is still its last statement's, and what it throws can be caught
        private fun guarded(script: String) =
            "try {\n$script\n} catch (thrown) { ({ ${quoted(THROWN)}: String(thrown) }) }"

        // Compiling doesn't run the script. A site whose policy forbids compiling from text gives no answer here.
        private fun misreadingOf(script: String) = """
            (() => {
                try {
                    new Function(${quoted(script)});
                    return null;
                } catch (refused) {
                    return refused instanceof SyntaxError ? String(refused) : null;
                }
            })()
        """.trimIndent()

        // A finger put down a tenth in from one edge of what shows of the element and moved to a tenth from the other
        private fun swipeAcross(selector: String, direction: WebPageDirection): String {
            val (fromX, fromY, toX, toY) = when (direction) {
                WebPageDirection.LEFT -> listOf(0.9, 0.5, 0.1, 0.5)
                WebPageDirection.RIGHT -> listOf(0.1, 0.5, 0.9, 0.5)
                WebPageDirection.UP -> listOf(0.5, 0.9, 0.5, 0.1)
                WebPageDirection.DOWN -> listOf(0.5, 0.1, 0.5, 0.9)
            }
            return """
                (() => {
                    const element = document.querySelector(${quoted(selector)});
                    if (!element) return null;
                    element.scrollIntoView({ behavior: 'instant', block: 'center', inline: 'center' });
                    const box = element.getBoundingClientRect();
                    const left = Math.max(box.left, 0), right = Math.min(box.right, innerWidth);
                    const top = Math.max(box.top, 0), bottom = Math.min(box.bottom, innerHeight);
                    if (right - left < 1 || bottom - top < 1) return [${quoted(OUT_OF_VIEW)}];
                    const x = part => left + (right - left) * part, y = part => top + (bottom - top) * part;
                    const from = [x($fromX), y($fromY)];
                    if (!element.contains(document.elementFromPoint(from[0], from[1]))) return [${quoted(COVERED)}];
                    return [from[0], from[1], x($toX), y($toY)];
                })()
            """.trimIndent()
        }

        private const val LIFTED = "document[Symbol.for('finger lifted')]"
        private val WATCH_LIFT = """
            (() => {
                $LIFTED = false;
                const lifted = () => {
                    $LIFTED = true;
                    document.removeEventListener('touchend', lifted, true);
                    document.removeEventListener('touchcancel', lifted, true);
                };
                document.addEventListener('touchend', lifted, true);
                document.addEventListener('touchcancel', lifted, true);
            })()
        """.trimIndent()
        private const val LIFT_SEEN = "$LIFTED !== false"

        private const val OUT_OF_VIEW = "out of view"
        private const val COVERED = "covered"

        // Scrolled at once: a site's smooth scrolling would still be moving the element when it is measured. A tap
        // lands on whatever is at its point, so one that can't land on the element is refused: scrolling doesn't
        // bring in what a transform moved away, such as a carousel's other slides, and a banner over the element
        // takes its tap. The reason follows the point, so that an element on its way in reads as still moving.
        private fun middleOf(selector: String) = """
            (() => {
                const element = document.querySelector(${quoted(selector)});
                if (!element) return null;
                element.scrollIntoView({ behavior: 'instant', block: 'center', inline: 'center' });
                const box = element.getBoundingClientRect();
                const x = box.left + box.width / 2, y = box.top + box.height / 2;
                const shown = box.width > 0 && box.height > 0;
                if (!shown || x < 0 || y < 0 || x >= innerWidth || y >= innerHeight) {
                    return [x, y, ${quoted(OUT_OF_VIEW)}];
                }
                if (!element.contains(document.elementFromPoint(x, y))) return [x, y, ${quoted(COVERED)}];
                return [x, y];
            })()
        """.trimIndent()

        private fun scrollIntoView(selector: String) = """
            (() => {
                const element = document.querySelector(${quoted(selector)});
                if (!element) return null;
                element.scrollIntoView({ behavior: 'instant', block: 'center', inline: 'center' });
                return true;
            })()
        """.trimIndent()

        // What the field holds is selected, so the text typed next takes its place, and the input that text makes is
        // watched for
        private fun focus(selector: String) = """
            (() => {
                const element = document.querySelector(${quoted(selector)});
                if (!element) return null;
                element.focus();
                if (document.activeElement !== element) return false;
                document[$TOOK_TEXT] = false;
                const arrived = () => { document[$TOOK_TEXT] = true; };
                document.addEventListener('input', arrived, { capture: true, once: true });
                if (typeof element.select === 'function') {
                    element.select();
                } else {
                    document.getSelection().selectAllChildren(element);
                }
                return true;
            })()
        """.trimIndent()

        // No text typed into a field that holds none makes no input
        private fun tookText(text: String) = if (text.isEmpty()) {
            """
            (() => {
                const field = document.activeElement;
                return document[$TOOK_TEXT] === true || (field.value ?? field.textContent) === '';
            })()
            """.trimIndent()
        } else {
            "document[$TOOK_TEXT] === true"
        }

        private const val TOOK_TEXT = "Symbol.for('typed text arrived')"

        private fun quoted(text: String) = JsonPrimitive(text).toString()

        /**
         * A page of [pages] for [spec] that has loaded nothing yet.
         *
         * @throws WebPageException where the platform can open none.
         */
        suspend fun open(pages: HostPages, spec: WebPageSpec): DrivenWebPage {
            val capture = ResponseCapture()
            // A response nobody waits for is dropped, and a burst a slow source hasn't read loses its oldest
            val responses = MutableSharedFlow<WebPageResponse>(
                extraBufferCapacity = RESPONSES_HELD,
                onBufferOverflow = BufferOverflow.DROP_OLDEST,
            )
            val lost = Job()
            val host = pages.open(
                HostPageSettings(
                    width = spec.size.width,
                    height = spec.size.height,
                    userAgent = spec.userAgent,
                    startScript = capture.script,
                    channel = capture.channel,
                ),
                object : HostPageEvents {
                    override fun onMessage(message: String) {
                        capture.read(message)?.let(responses::tryEmit)
                    }

                    override fun onGone() {
                        lost.complete()
                    }
                },
            )
            return DrivenWebPage(host, spec.headers, responses, lost)
        }
    }
}
