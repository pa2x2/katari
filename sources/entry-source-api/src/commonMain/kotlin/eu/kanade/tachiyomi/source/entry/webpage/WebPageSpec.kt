package eu.kanade.tachiyomi.source.entry.webpage

/**
 * @property url the address a new page loads before the source prepares it.
 * @property size the window the site lays itself out in, the same on a phone and on a desktop, so the source meets one
 * layout of the site wherever it runs.
 * @property userAgent sent by the page and shown to its scripts; null sends the one Katari's own requests send.
 * @property headers sent with the request for [url] and with every [WebPage.load], not with what the page's scripts
 * request.
 */
class WebPageSpec(
    val url: String,
    val size: WebPageSize = WebPageSize.PHONE,
    val userAgent: String? = null,
    val headers: Map<String, String> = emptyMap(),
)

/** A page's window in CSS pixels. */
class WebPageSize(val width: Int, val height: Int) {
    companion object {
        val PHONE = WebPageSize(width = 412, height = 915)
        val DESKTOP = WebPageSize(width = 1280, height = 800)
    }
}
