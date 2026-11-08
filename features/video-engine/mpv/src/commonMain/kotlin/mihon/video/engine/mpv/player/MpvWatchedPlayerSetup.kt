package mihon.video.engine.mpv.player

import mihon.video.engine.mpv.client.MpvClient
import mihon.video.engine.mpv.client.MpvFormat

/**
 * Sets up a new instance for the watched player, before it initialises, on top of the options its platform sets for
 * drawing, decoding and sound.
 *
 * Everything reaches mpv through the proxy, so it never opens HTTPS itself; `tls-verify` makes an accidental direct
 * request fail rather than go unchecked. `keep-open` holds the last frame at the end, where the player can still seek,
 * and `idle` keeps the instance after a file ends: the player destroys it. It has no output until it gets one, and no
 * subtitles drawn until [MpvPlayer] picks one mpv draws itself.
 */
internal fun setWatchedPlayerOptions(client: MpvClient) {
    OPTIONS.forEach { (name, value) -> client.setOption(name, value) }
}

/** The properties [MpvPlayer] follows; an instance forgets them when it is destroyed. */
internal fun observeWatchedPlayerProperties(client: MpvClient) {
    OBSERVED_PROPERTIES.forEach { (name, format) -> client.observe(name, format) }
}

private val OPTIONS = listOf(
    "vo" to "null",
    "idle" to "yes",
    "keep-open" to "yes",
    "force-window" to "no",
    "pause" to "yes",
    "tls-verify" to "yes",
    "input-default-bindings" to "no",
    "input-vo-keyboard" to "no",
    "osd-level" to "0",
    "sub-auto" to "no",
    "audio-file-auto" to "no",
    "cover-art-auto" to "no",
    "sub-visibility" to "no",
    // Room for the volume boost; mpv caps the volume at 130 otherwise.
    "volume-max" to "150",
    // mpv would otherwise list only the playing HLS variant's tracks, as an edition, which hides the other qualities.
    "flatten-editions" to "yes",
    // A stream reads ahead 50 s, as ExoPlayer does: a quality Auto chooses plays once what was buffered has, so a
    // deeper buffer would delay it by minutes. Each platform sizes the buffer, which caps what high bitrates hold.
    "cache-secs" to "50",
)

private val OBSERVED_PROPERTIES = listOf(
    "time-pos" to MpvFormat.Double,
    "duration" to MpvFormat.Double,
    "demuxer-cache-time" to MpvFormat.Double,
    "paused-for-cache" to MpvFormat.Flag,
    "seeking" to MpvFormat.Flag,
    "eof-reached" to MpvFormat.Flag,
    "dwidth" to MpvFormat.Int64,
    "dheight" to MpvFormat.Int64,
    "track-list" to MpvFormat.String,
    "chapter-list" to MpvFormat.String,
    "sub-text" to MpvFormat.String,
)
