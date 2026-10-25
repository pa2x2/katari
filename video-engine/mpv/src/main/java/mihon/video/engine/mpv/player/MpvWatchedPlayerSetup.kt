package mihon.video.engine.mpv.player

import `is`.xyz.mpv.MPVLib
import `is`.xyz.mpv.MPVLib.MpvFormat
import java.io.File

/**
 * Sets up a new instance for the watched player, before it initialises.
 *
 * Everything reaches mpv through the proxy, so it never opens HTTPS itself; `tls-verify` makes an accidental direct
 * request fail rather than go unchecked. `keep-open` holds the last frame at the end, where the player can still seek,
 * and `idle` keeps the instance after a file ends: the player destroys it. It has no output until it gets a surface,
 * and no subtitles drawn until [MpvPlayer] picks one mpv draws itself. `config` stays on for the fonts in
 * [configDirectory] ([mpvConfigDirectory]).
 */
internal fun setWatchedPlayerOptions(configDirectory: File) {
    OPTIONS.forEach { (name, value) -> MPVLib.setOptionString(name, value) }
    MPVLib.setOptionString("config-dir", configDirectory.path)
}

/** The properties [MpvPlayer] follows; an instance forgets them when it is destroyed. */
internal fun observeWatchedPlayerProperties() {
    OBSERVED_PROPERTIES.forEach { (name, format) -> MPVLib.observeProperty(name, format) }
}

private val OPTIONS = listOf(
    "config" to "yes",
    "vo" to "null",
    "gpu-context" to "android",
    "opengl-es" to "yes",
    // Frames go from the decoder to the GPU without a copy. Android logs a stream of dmabuf `getattr` SELinux denials
    // for it that don't affect playback; `mediacodec-copy` has none, at the cost of a copy per frame.
    "hwdec" to "mediacodec,mediacodec-copy",
    "hwdec-codecs" to "h264,hevc,mpeg4,mpeg2video,vp8,vp9,av1",
    "ao" to "audiotrack,opensles",
    "idle" to "yes",
    "keep-open" to "yes",
    "force-window" to "no",
    "pause" to "yes",
    "tls-verify" to "yes",
    "input-default-bindings" to "no",
    "input-vo-keyboard" to "no",
    "osd-level" to "0",
    "ytdl" to "no",
    "load-scripts" to "no",
    "sub-auto" to "no",
    "audio-file-auto" to "no",
    "cover-art-auto" to "no",
    "sub-visibility" to "no",
    // mpv would otherwise list only the playing HLS variant's tracks, as an edition, which hides the other qualities.
    "flatten-editions" to "yes",
    // A stream reads ahead 50 s, as ExoPlayer does: a quality Auto chooses plays once what was buffered has, so a
    // deeper buffer would delay it by minutes. The size caps what high bitrates hold.
    "cache-secs" to "50",
    "demuxer-max-bytes" to "64MiB",
    "demuxer-max-back-bytes" to "16MiB",
)

private val OBSERVED_PROPERTIES = listOf(
    "time-pos" to MpvFormat.MPV_FORMAT_DOUBLE,
    "duration" to MpvFormat.MPV_FORMAT_DOUBLE,
    "demuxer-cache-time" to MpvFormat.MPV_FORMAT_DOUBLE,
    "paused-for-cache" to MpvFormat.MPV_FORMAT_FLAG,
    "seeking" to MpvFormat.MPV_FORMAT_FLAG,
    "seekable" to MpvFormat.MPV_FORMAT_FLAG,
    "eof-reached" to MpvFormat.MPV_FORMAT_FLAG,
    "dwidth" to MpvFormat.MPV_FORMAT_INT64,
    "dheight" to MpvFormat.MPV_FORMAT_INT64,
    "track-list" to MpvFormat.MPV_FORMAT_STRING,
    "sub-text" to MpvFormat.MPV_FORMAT_STRING,
)
