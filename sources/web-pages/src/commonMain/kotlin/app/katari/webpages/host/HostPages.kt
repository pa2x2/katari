package app.katari.webpages.host

import app.katari.core.common.platform.PlatformFacility
import eu.kanade.tachiyomi.source.entry.webpage.WebPageException
import eu.kanade.tachiyomi.source.entry.webpage.WebPageKey

/** A platform's web view as the keeper needs it: pages nobody sees, each laid out at a size of its own. */
@PlatformFacility
interface HostPages {
    /**
     * A page that has loaded nothing yet.
     *
     * @throws WebPageException telling the user why, where the platform can open none now.
     */
    suspend fun open(settings: HostPageSettings, events: HostPageEvents): HostPage
}

/**
 * @property width in CSS pixels, as [height].
 * @property userAgent null for the one the app's requests send.
 * @property startScript run at the start of each of the page's documents, before their own scripts.
 * @property channel the name of an object on the documents' `window` whose `postMessage(text)` hands the text to
 * [HostPageEvents.onMessage].
 */
class HostPageSettings(
    val width: Int,
    val height: Int,
    val userAgent: String?,
    val startScript: String,
    val channel: String,
)

/** What a page tells, on any thread, until it is closed. */
interface HostPageEvents {
    fun onMessage(message: String)

    /** The page's renderer or its web view ended; the page does nothing more. */
    fun onGone()
}

/** One page. Calls are made one at a time. */
interface HostPage {
    /**
     * Loads [url] in the main frame, sending [headers] with its request, and returns once it finished.
     *
     * @throws WebPageException where the load failed.
     */
    suspend fun load(url: String, headers: Map<String, String>)

    /** The value of [script] in the main frame as JSON, `null` where it has none or threw. */
    suspend fun evaluate(script: String): String

    /**
     * One step of a finger at the page's CSS pixel [x], [y], which the page takes for a person's. The caller lifts
     * every finger it put down, and paces the steps.
     */
    suspend fun touch(step: HostTouch, x: Float, y: Float)

    /**
     * Enters [text] into the focused field as a keyboard does.
     *
     * @throws WebPageException where nothing that takes text has the focus.
     */
    suspend fun insertText(text: String)

    suspend fun press(key: WebPageKey)

    /** While [idle], the page stops what only a page someone looks at needs, where the platform can stop it. */
    suspend fun setIdle(idle: Boolean)

    suspend fun close()
}

enum class HostTouch { DOWN, MOVE, UP }
