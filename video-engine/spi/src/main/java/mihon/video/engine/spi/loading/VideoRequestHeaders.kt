package mihon.video.engine.spi.loading

import eu.kanade.tachiyomi.source.entry.VideoStream

/**
 * The headers every request of one playback carries, whichever engine makes it: the stream's own headers and user
 * agent on every request, and an external subtitle's or audio track's own headers on top of them for that track.
 */
class VideoRequestHeaders(
    stream: VideoStream,
    defaultUserAgent: () -> String,
) {
    /** The stream's own headers, which may name the user agent as well. */
    val stream: Map<String, String> = stream.resolvedRequest.headers

    val userAgent: String = this.stream.entries
        .firstOrNull { it.key.equals(USER_AGENT, ignoreCase = true) }
        ?.value
        ?: defaultUserAgent()

    private val trackHeadersByUrl =
        stream.subtitles.associate { it.request.url to it.request.headers } +
            stream.audioTracks.associate { it.request.url to it.request.headers }

    /** The headers [url] adds on top of [stream] when it is one of the external subtitles or audio tracks. */
    fun trackExtras(url: String): Map<String, String> = trackHeadersByUrl[url].orEmpty()

    companion object {
        const val USER_AGENT = "User-Agent"
    }
}
