package eu.kanade.tachiyomi.source.aniyomi.playback

import eu.kanade.tachiyomi.animesource.AnimeSource
import eu.kanade.tachiyomi.animesource.model.HttpServer
import eu.kanade.tachiyomi.animesource.online.AnimeHttpSource
import eu.kanade.tachiyomi.source.entry.VideoRequest
import eu.kanade.tachiyomi.source.entry.VideoStream

/**
 * The loopback server an extension runs for streams its own code produces. It writes [HttpServer.PLACEHOLDER_URL] into
 * those links; the server starts the first time one is played and runs until the source is unloaded, so downloads of
 * such streams work too.
 */
internal class AniyomiLoopbackServer(private val source: AnimeSource) : AutoCloseable {

    private var server: HttpServer? = null
    private var closed = false

    /** The running server's address, or null when the extension has none or it couldn't start. */
    @Synchronized
    fun url(): String? {
        if (closed) return null
        server?.let { return it.url }
        val created = (source as? AnimeHttpSource)?.createHttpServer() ?: return null
        created.start()
        if (!created.isRunning()) return null
        server = created
        return created.url
    }

    @Synchronized
    override fun close() {
        closed = true
        server?.stop()
        server = null
    }
}

internal val VideoStream.usesLoopbackServer: Boolean
    get() = requestUrls().any(::isPlaceholder)

/** [this] with the placeholder in its video, audio and subtitle links pointing at [serverUrl]. */
internal fun VideoStream.withLoopbackServer(serverUrl: String): VideoStream = copy(
    request = request?.withServer(serverUrl),
    subtitles = subtitles.map { it.copy(request = it.request.withServer(serverUrl)) },
    audioTracks = audioTracks.map { it.copy(request = it.request.withServer(serverUrl)) },
)

private fun VideoStream.requestUrls(): List<String> =
    listOfNotNull(request?.url) + subtitles.map { it.request.url } + audioTracks.map { it.request.url }

private fun VideoRequest.withServer(serverUrl: String): VideoRequest =
    if (isPlaceholder(url)) copy(url = serverUrl + url.removePrefix(HttpServer.PLACEHOLDER_URL)) else this

// Port 1 exactly: http://localhost:1234 is some other server
private fun isPlaceholder(url: String): Boolean =
    url.startsWith(HttpServer.PLACEHOLDER_URL) &&
        url.getOrNull(HttpServer.PLACEHOLDER_URL.length).let { it == null || it == '/' || it == '?' || it == '#' }
