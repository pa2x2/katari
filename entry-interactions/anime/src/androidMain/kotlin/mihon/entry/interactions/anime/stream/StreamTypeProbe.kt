package mihon.entry.interactions.anime.stream

import eu.kanade.tachiyomi.network.HttpException
import eu.kanade.tachiyomi.network.await
import eu.kanade.tachiyomi.source.entry.VideoAudioTrack
import eu.kanade.tachiyomi.source.entry.VideoStream
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import logcat.LogPriority
import mihon.video.engine.spi.loading.VideoRequestHeaders
import mihon.video.engine.spi.loading.resolvedRequest
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import tachiyomi.core.common.util.system.logcat
import java.io.IOException
import java.util.concurrent.TimeUnit

/**
 * Tells what a stream the source didn't type is, once, just before it plays or downloads: one request for its first
 * bytes through the client and headers the player loads it with, read by [sniffStreamContent]. Both engines, the seek
 * preview, the feed, downloads and the download manifest then read the stream's `type`.
 *
 * A stream whose request fails or times out can't play either, so [typed] throws [StreamProbeException] and the
 * caller tries the next stream. Bytes that name no format fall back to [declaredStreamType]. A host that refuses the
 * range request is asked once more without it; if that fails as well the stream keeps its declared type, since the
 * player may still load it. External audio tracks are typed the same way but never fail their stream.
 */
internal class StreamTypeProbe(
    client: OkHttpClient,
    private val defaultUserAgent: () -> String,
) {
    private val client = client.newBuilder()
        .callTimeout(PROBE_TIMEOUT_MS, TimeUnit.MILLISECONDS)
        .build()

    // What a URL is doesn't change while it lives; replaying an episode or reopening a feed item reuses the answer.
    private val known = object : LinkedHashMap<String, SniffedContent>(16, 0.75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, SniffedContent>) = size > KNOWN_URLS
    }

    /** [stream], already resolved, with its type and the types of its external audio tracks set. */
    suspend fun typed(stream: VideoStream): VideoStream = coroutineScope {
        val headers = VideoRequestHeaders(stream, defaultUserAgent)
        val audioTracks = stream.audioTracks.map { track -> async { typedTrack(track, headers) } }
        val typed = if (isTypedBySource(stream.type, stream.mimeType)) stream else typedStream(stream, headers)
        typed.copy(audioTracks = audioTracks.awaitAll())
    }

    private suspend fun typedStream(stream: VideoStream, headers: VideoRequestHeaders): VideoStream {
        val outcome = probe(stream.resolvedRequest.url, headers.stream, headers.userAgent)
        if (outcome is Outcome.Failed) throw StreamProbeException(streamChoiceKey(stream), outcome.cause)
        val sniffed = (outcome as? Outcome.Sniffed)?.content
            ?: return stream.copy(type = declaredStreamType(stream))
        return stream.copy(type = sniffed.type, mimeType = stream.mimeType ?: sniffed.mimeType)
    }

    private suspend fun typedTrack(track: VideoAudioTrack, headers: VideoRequestHeaders): VideoAudioTrack {
        if (isTypedBySource(track.type, track.mimeType)) return track
        val url = track.request.url
        val outcome = probe(url, headers.stream + headers.trackExtras(url), headers.userAgent)
        if (outcome is Outcome.Failed) {
            logcat(LogPriority.WARN, outcome.cause) { "Couldn't tell the type of audio track '${track.key}'" }
        }
        val sniffed = (outcome as? Outcome.Sniffed)?.content
            ?: return track.copy(type = declaredStreamType(track))
        return track.copy(type = sniffed.type, mimeType = track.mimeType ?: sniffed.mimeType)
    }

    private suspend fun probe(url: String, headers: Map<String, String>, userAgent: String): Outcome {
        synchronized(known) { known[url] }?.let { return Outcome.Sniffed(it) }
        // Downloaded files and other schemes have nothing to ask; the engine reads them itself.
        val httpUrl = url.toHttpUrlOrNull() ?: return Outcome.Unrecognised
        val outcome = when (val ranged = fetch(httpUrl, headers, userAgent, ranged = true)) {
            Outcome.RangeRejected -> fetch(httpUrl, headers, userAgent, ranged = false)
                .takeIf { it is Outcome.Sniffed }
                ?: Outcome.Unrecognised
            else -> ranged
        }
        if (outcome is Outcome.Sniffed) synchronized(known) { known[url] = outcome.content }
        return outcome
    }

    private suspend fun fetch(url: HttpUrl, headers: Map<String, String>, userAgent: String, ranged: Boolean): Outcome {
        val request = Request.Builder()
            .url(url)
            .apply {
                headers.forEach { (name, value) -> header(name, value) }
                header(VideoRequestHeaders.USER_AGENT, userAgent)
                if (ranged) header("Range", "bytes=0-${PREFIX_BYTES - 1}")
            }
            .build()
        val response = try {
            client.newCall(request).await()
        } catch (e: IOException) {
            return Outcome.Failed(e)
        }
        return response.use {
            when {
                ranged && it.code == HTTP_RANGE_NOT_SATISFIABLE -> Outcome.RangeRejected
                !it.isSuccessful -> Outcome.Failed(HttpException(it.code))
                else -> try {
                    // A host that ignores the range sends the whole file: read the start only, then close.
                    val prefix = withContext(Dispatchers.IO) {
                        val source = it.body.source()
                        source.request(PREFIX_BYTES)
                        source.buffer.readByteArray(minOf(source.buffer.size, PREFIX_BYTES))
                    }
                    sniffStreamContent(prefix, it.header("Content-Type"))?.let(Outcome::Sniffed) ?: Outcome.Unrecognised
                } catch (e: IOException) {
                    Outcome.Failed(e)
                }
            }
        }
    }

    private sealed interface Outcome {
        data class Sniffed(val content: SniffedContent) : Outcome

        data object Unrecognised : Outcome

        data object RangeRejected : Outcome

        data class Failed(val cause: Throwable) : Outcome
    }

    private companion object {
        // The slowest probe measured on a first connection took 1.2 s.
        const val PROBE_TIMEOUT_MS = 5_000L
        const val PREFIX_BYTES = 4_096L
        const val KNOWN_URLS = 64
        const val HTTP_RANGE_NOT_SATISFIABLE = 416
    }
}

/** The stream couldn't be fetched to tell what it is, so it can't play either. */
internal class StreamProbeException(streamKey: String, cause: Throwable) :
    IOException("Stream '$streamKey' couldn't be reached", cause)
