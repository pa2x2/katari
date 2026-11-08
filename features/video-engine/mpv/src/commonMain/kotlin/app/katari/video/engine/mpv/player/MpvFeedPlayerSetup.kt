package app.katari.video.engine.mpv.player

import app.katari.video.engine.mpv.client.MpvClient

/**
 * Sets up a new instance for a clip in the immersive feed, over the watched player's options. The feed keeps the clips
 * beside the one shown open too, each with a buffer of its own, so a clip reads ahead only enough to play on smoothly
 * rather than the watched player's deep buffer.
 */
internal fun setFeedOptions(client: MpvClient) {
    OPTIONS.forEach { (name, value) -> client.setOption(name, value) }
}

private val OPTIONS = listOf(
    "cache-secs" to "20",
    "demuxer-max-bytes" to "32MiB",
    "demuxer-max-back-bytes" to "8MiB",
)
