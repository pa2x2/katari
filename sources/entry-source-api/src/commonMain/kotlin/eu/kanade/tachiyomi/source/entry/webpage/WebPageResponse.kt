package eu.kanade.tachiyomi.source.entry.webpage

/**
 * A response one of the page's scripts received.
 *
 * @property url the request's address in full.
 * @property requestBody what the script sent where that was text, else null.
 * @property body the response where it is text, such as JSON, HTML or a playlist; empty for anything else.
 */
class WebPageResponse(
    val url: String,
    val method: String,
    val status: Int,
    val requestBody: String?,
    val body: String,
)
