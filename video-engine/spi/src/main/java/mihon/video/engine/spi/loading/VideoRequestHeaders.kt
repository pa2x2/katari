package mihon.video.engine.spi.loading

import eu.kanade.tachiyomi.source.entry.VideoStream
import eu.kanade.tachiyomi.source.entry.VideoSubtitle

/**
 * The headers every request of one playback carries, whichever engine makes it: the stream's own headers and user
 * agent on every request, and an external subtitle's own headers on top of them for that subtitle.
 */
class VideoRequestHeaders(
    stream: VideoStream,
    subtitles: List<VideoSubtitle>,
    defaultUserAgent: () -> String,
) {
    /** The stream's own headers, which may name the user agent as well. */
    val stream: Map<String, String> = stream.request.headers

    val userAgent: String = stream.request.headers.entries
        .firstOrNull { it.key.equals(USER_AGENT, ignoreCase = true) }
        ?.value
        ?: defaultUserAgent()

    private val subtitleHeadersByUrl = subtitles.associate { it.request.url to it.request.headers }

    /** The headers [url] adds on top of [stream] when it is one of the external subtitles. */
    fun subtitleExtras(url: String): Map<String, String> = subtitleHeadersByUrl[url].orEmpty()

    companion object {
        const val USER_AGENT = "User-Agent"
    }
}
