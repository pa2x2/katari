package app.katari.video.engine.mpv.player

import app.katari.video.engine.mpv.client.MpvClient

/**
 * Sets up a new instance for the seek preview, over the watched player's options. It only ever shows the frame at one
 * moment: no sound or subtitles, seeks to the keyframe before it, which shows a frame soonest, a small buffer, and
 * decoding that skips the deblocking filter, which a small picture doesn't show.
 */
internal fun setSeekPreviewOptions(client: MpvClient) {
    OPTIONS.forEach { (name, value) -> client.setOption(name, value) }
}

private val OPTIONS = listOf(
    "aid" to "no",
    "sid" to "no",
    "hr-seek" to "no",
    "cache-secs" to "5",
    "demuxer-max-bytes" to "16MiB",
    "demuxer-max-back-bytes" to "0",
    "vd-lavc-skiploopfilter" to "all",
)
