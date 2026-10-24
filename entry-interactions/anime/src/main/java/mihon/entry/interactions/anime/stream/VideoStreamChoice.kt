package mihon.entry.interactions.anime.stream

import eu.kanade.tachiyomi.source.entry.VideoStream
import eu.kanade.tachiyomi.source.entry.VideoStreamType

/**
 * The key a stream is picked, remembered and downloaded by. Streams without a key or label fall back to their hints,
 * then their URL, so two unlabelled qualities of one server stay apart.
 */
internal fun streamChoiceKey(stream: VideoStream): String {
    return stream.key
        .ifBlank { stream.label }
        .ifBlank { listOfNotNull(stream.height?.let { "${it}p" }, stream.bitrate?.toString()).joinToString("|") }
        .ifBlank { stream.request?.url.orEmpty() }
}

/**
 * The stream the player, a download and the feed take: the title's saved stream, then the one the source prefers, then
 * the one nearest [preferredHeight], then the best by transport.
 */
internal fun chooseStream(
    streams: List<VideoStream>,
    savedKey: String?,
    sourceKey: String?,
    preferredHeight: PreferredHeight?,
): VideoStream? {
    if (streams.isEmpty()) return null
    listOfNotNull(savedKey, sourceKey).forEach { key ->
        streams.firstOrNull { streamChoiceKey(it) == key }?.let { return it }
    }
    preferredHeight?.let { pickByHeight(streams, it, ::streamHeight) }?.let { return it }
    return streams.maxBy(::streamScore)
}

internal fun streamHeight(stream: VideoStream): Int? = stream.height ?: videoHeightOf(stream.label)

private fun streamScore(stream: VideoStream): Int {
    val typeScore = when (stream.type) {
        VideoStreamType.HLS -> 400
        VideoStreamType.PROGRESSIVE -> 300
        VideoStreamType.DASH -> 200
        VideoStreamType.UNKNOWN -> 100
    }
    val mimeScore = when {
        stream.mimeType?.contains("mp4", ignoreCase = true) == true -> 30
        stream.mimeType?.contains("mpegurl", ignoreCase = true) == true -> 20
        stream.mimeType?.contains("dash", ignoreCase = true) == true -> 10
        else -> 0
    }
    val headerScore = if (stream.request?.headers.isNullOrEmpty()) 0 else 5
    val labelScore = if (stream.label.isNotBlank()) 1 else 0
    return typeScore + mimeScore + headerScore + labelScore
}
