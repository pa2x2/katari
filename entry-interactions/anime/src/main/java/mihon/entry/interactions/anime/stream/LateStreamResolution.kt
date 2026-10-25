package mihon.entry.interactions.anime.stream

import eu.kanade.tachiyomi.source.entry.DeferredStreamSource
import eu.kanade.tachiyomi.source.entry.SEntryChapter
import eu.kanade.tachiyomi.source.entry.UnifiedSource
import eu.kanade.tachiyomi.source.entry.VideoStream
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.withTimeoutOrNull
import logcat.LogPriority
import tachiyomi.core.common.util.system.logcat
import java.io.IOException
import kotlin.time.Duration.Companion.milliseconds

/**
 * The streams worth trying: those with a link other than a torrent, and those [source] links on demand as a
 * [DeferredStreamSource]. A stream without a link from any other source can never play.
 */
internal fun playableCandidates(source: UnifiedSource, streams: List<VideoStream>): List<VideoStream> {
    val resolvesLinks = source is DeferredStreamSource
    return streams.filter { stream ->
        val request = stream.request
        when {
            stream.isTorrent -> false
            request != null -> request.url.isNotBlank()
            resolvesLinks -> true
            else -> {
                source.logcat(LogPriority.WARN) {
                    "Source ${source.id} listed stream '${streamChoiceKey(stream)}' without a link and can't resolve it"
                }
                false
            }
        }
    }
}

/**
 * The first of [ranked] that plays, each made ready by [prepare]: its link resolved when it has none, and its type
 * probed. When that fails the next stream is tried, at most [MAX_PREPARE_ATTEMPTS] times, unless the user picked the
 * first one ([explicitPick]): they see that stream's failure rather than a different stream.
 */
internal suspend fun firstPlayableStream(
    ranked: List<VideoStream>,
    explicitPick: Boolean,
    prepare: suspend (VideoStream) -> VideoStream,
): Result<VideoStream> {
    var lastFailure: Throwable = NoSuchElementException("No stream to play")
    for ((attempt, stream) in ranked.withIndex()) {
        if (attempt == MAX_PREPARE_ATTEMPTS) break
        try {
            return Result.success(prepare(stream))
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            stream.logcat(LogPriority.WARN, e) { "Stream '${streamChoiceKey(stream)}' couldn't be made ready" }
            lastFailure = e
        }
        if (explicitPick) break
    }
    return Result.failure(lastFailure)
}

/** [stream] with the link [source] fills in for it. */
internal suspend fun resolveDeferredStream(
    source: UnifiedSource,
    chapter: SEntryChapter,
    stream: VideoStream,
    timeoutMs: Long = STREAM_RESOLVE_TIMEOUT_MS,
): VideoStream {
    val key = streamChoiceKey(stream)
    val deferredSource = source as? DeferredStreamSource
        ?: throw IllegalStateException("Source ${source.id} can't resolve stream '$key'")
    val resolved = withTimeoutOrNull(timeoutMs.milliseconds) { deferredSource.resolveStream(chapter, stream) }
        ?: throw StreamResolveTimeoutException(key)
    if (resolved.request?.url.isNullOrBlank()) {
        throw IOException(
            "Source ${source.id} resolved stream '$key' without a link",
        )
    }
    if (resolved.isTorrent) throw TorrentStreamException(key)
    // The key the stream was picked and saved by must still find it after resolution.
    return resolved.copy(key = key, subtitles = resolved.subtitles.filter { it.request.url.isNotBlank() })
}

internal class StreamResolveTimeoutException(streamKey: String) : IOException("Resolving stream '$streamKey' timed out")

private const val MAX_PREPARE_ATTEMPTS = 3
private const val STREAM_RESOLVE_TIMEOUT_MS = 15_000L
