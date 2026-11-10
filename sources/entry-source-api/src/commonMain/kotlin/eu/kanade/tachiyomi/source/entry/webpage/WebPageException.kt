package eu.kanade.tachiyomi.source.entry.webpage

import java.io.IOException

/**
 * A page couldn't be made, was lost, or didn't get to what a call waited for. Katari shows [message] as the source's
 * error.
 */
class WebPageException(message: String, cause: Throwable? = null) : IOException(message, cause)
