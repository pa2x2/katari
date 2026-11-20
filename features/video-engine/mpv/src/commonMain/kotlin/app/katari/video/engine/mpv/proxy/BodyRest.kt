package app.katari.video.engine.mpv.proxy

import okhttp3.Request
import okhttp3.Response

/*
 * The rest of a body the host stopped sending partway, asked for by its byte range so the proxy can go on relaying it
 * on the same response to mpv: FFmpeg skips a segment that reaches it cut short, which plays a separate audio track out
 * of step with the video.
 */

/**
 * The request for the rest of this response's body once [copied] bytes of it were relayed, or null when the rest can't
 * be asked for: without the body's length neither what remains nor whether an answer continues it is known, and an
 * encoded body's ranges count encoded bytes, which the host needn't encode the same way again. `If-Range` makes a host
 * whose file changed since answer with the whole new file, which [continues] refuses.
 */
internal fun Response.restRequest(copied: Long): Request? {
    val length = body.contentLength()
    if (length < 0 || isEncoded) return null
    val start = if (code == PARTIAL_CONTENT) contentRange()?.first ?: return null else 0L
    val validator = header("ETag")?.takeUnless { it.startsWith("W/") } ?: header("Last-Modified")
    return request.newBuilder()
        .header("Range", "bytes=${start + copied}-${start + length - 1}")
        .apply { validator?.let { header("If-Range", it) } }
        .build()
}

/** Whether this response is exactly the range [rest] asked for, so its body continues the one that was cut short. */
internal fun Response.continues(rest: Request): Boolean {
    if (code != PARTIAL_CONTENT) return false
    val asked = rest.header("Range")?.removePrefix("bytes=")?.split('-')?.map { it.toLongOrNull() ?: return false }
    val answered = contentRange() ?: return false
    val length = body.contentLength()
    return asked == listOf(answered.first, answered.last) &&
        (length < 0 || length == answered.last - answered.first + 1)
}

/** The bytes a 206 response's `Content-Range` says it holds. */
private fun Response.contentRange(): LongRange? {
    val range = header("Content-Range")?.trim()?.removePrefix("bytes ")?.substringBefore('/') ?: return null
    val first = range.substringBefore('-').toLongOrNull() ?: return null
    val last = range.substringAfter('-').toLongOrNull() ?: return null
    return first..last
}

private const val PARTIAL_CONTENT = 206
