package mihon.video.engine.mpv.player

import eu.kanade.tachiyomi.source.entry.VideoStream
import eu.kanade.tachiyomi.source.entry.VideoStreamType

/*
 * A stream's file options go with its loadfile command, so mpv drops them when that file ends. loadfile reads them as
 * one comma-separated list; each value is length-prefixed (%n%, in bytes) so commas, equals signs and quotes inside it
 * stay part of it.
 */

/**
 * The options [stream]'s file loads with: how Katari has mpv open it, then the source's own, which win.
 *
 * An HLS or DASH stream goes to FFmpeg's demuxers alone. Otherwise a manifest they reject, such as an error page or a
 * playlist the host garbled, reaches mpv's own playlist reader, which takes its lines for a playlist of URLs and plays
 * whichever of them works. mpv starts on the highest variant within [bitrateCap], in bits per second.
 */
internal fun streamFileOptions(stream: VideoStream, bitrateCap: Long): Map<String, String> {
    val opening = when (stream.type) {
        VideoStreamType.HLS, VideoStreamType.DASH -> mapOf("demuxer" to "lavf")
        VideoStreamType.PROGRESSIVE, VideoStreamType.UNKNOWN -> emptyMap()
    }
    return opening + ("hls-bitrate" to bitrateCap.toString()) + stream.mpvOptions
}

/** [options] as loadfile's options argument, leaving out names mpv couldn't parse; null when none remain. */
internal fun loadfileOptions(options: Map<String, String>): String? =
    options.entries
        .filter { (name, _) -> OPTION_NAME.matches(name) }
        .joinToString(separator = ",") { (name, value) -> "$name=%${value.encodeToByteArray().size}%$value" }
        .ifEmpty { null }

private val OPTION_NAME = Regex("[a-z0-9][a-z0-9-]*")
