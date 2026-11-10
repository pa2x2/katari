package eu.kanade.tachiyomi.source.entry.webpage

import org.jsoup.nodes.Document
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

/**
 * A page of the source's site with its scripts running. Selectors are CSS selectors as `document.querySelector` takes
 * them, and the first match counts.
 *
 * A site's app changes route without loading a page, so nothing here waits for "loaded" except [load]: wait for what
 * the route shows ([awaitSelector]), for where it leads ([awaitUrl]) or for what it requests ([responseTo]).
 *
 * Every call throws [WebPageException] once the page is gone.
 */
interface WebPage {

    /** The page's address now, which the site's app may have changed without a load. */
    suspend fun url(): String

    /** Loads [url] as a new document, with the page's [headers][WebPageSpec.headers], and waits for it to finish. */
    suspend fun load(url: String)

    /**
     * Runs [script] in the page as its own scripts run and returns its value as JSON: a string in quotes, `null`
     * where the script has no value JSON can hold or threw. A promise isn't waited for.
     */
    suspend fun evaluate(script: String): String

    /** The page as it stands now, with what its scripts rendered. */
    suspend fun document(): Document

    /**
     * Scrolls the element into view and taps its middle as a finger would, which the page can't tell from a person's
     * tap; a click made by script, it can. Returns once the tap is made, before the page answered it: wait for what
     * the tap brings with [awaitSelector], [awaitUrl] or [responseTo].
     *
     * Fails where the tap couldn't land on the element: scrolling doesn't bring it into view, as with a carousel's
     * other slides, or something else lies over its middle, such as a banner.
     */
    suspend fun click(selector: String)

    /**
     * Puts [text] into the field as a keyboard does, in place of what it held. The page sees the input as a person's,
     * but the whole text at once and no key for each letter.
     */
    suspend fun type(selector: String, text: String)

    /** Presses and releases [key] on whatever has the focus, such as the field last [typed][type] into. */
    suspend fun press(key: WebPageKey)

    suspend fun scrollTo(selector: String)

    /** Scrolls the document to its end, where a site that loads more as one reads requests the next part. */
    suspend fun scrollToEnd()

    suspend fun awaitSelector(selector: String, timeout: Duration = 30.seconds)

    suspend fun awaitUrl(timeout: Duration = 30.seconds, matches: (url: String) -> Boolean)

    /**
     * Runs [action] and returns the first response [matches] accepts among those the page's scripts receive from then
     * on. The watch starts before [action], so a response that comes at once isn't missed, as it could be by acting
     * first and waiting after.
     *
     * Only what scripts of the page's own document request with `fetch` or `XMLHttpRequest` is seen: not its frames'
     * requests, and not what the document loads itself, such as images and style sheets.
     */
    suspend fun responseTo(
        matches: (response: WebPageResponse) -> Boolean,
        timeout: Duration = 30.seconds,
        action: suspend () -> Unit,
    ): WebPageResponse
}

enum class WebPageKey { ENTER, TAB, ESCAPE, BACKSPACE, ARROW_UP, ARROW_DOWN, ARROW_LEFT, ARROW_RIGHT }
