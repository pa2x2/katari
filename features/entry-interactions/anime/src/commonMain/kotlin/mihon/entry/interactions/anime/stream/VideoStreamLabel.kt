package mihon.entry.interactions.anime.stream

import eu.kanade.tachiyomi.source.entry.VideoStream

/** What a stream is called in the player and the download sheet: its label, else its hints, such as "1080p · 4.2 Mb/s". */
internal fun streamDisplayLabel(stream: VideoStream): String {
    return stream.label
        .ifBlank {
            listOfNotNull(stream.height?.let { "${it}p" }, stream.bitrate?.let(::bitrateLabel)).joinToString(" · ")
        }
        .ifBlank { stream.request?.url.orEmpty() }
        .ifBlank { stream.key }
}

private fun bitrateLabel(bitsPerSecond: Long): String {
    return if (bitsPerSecond >= 1_000_000) {
        "%.1f Mb/s".format(bitsPerSecond / 1_000_000.0)
    } else {
        "${bitsPerSecond / 1_000} kb/s"
    }
}
