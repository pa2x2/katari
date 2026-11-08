package app.katari.video.engine.mpv.session

import app.katari.video.engine.mpv.client.MpvClient

/**
 * How mpv decodes and buffers on the desktop, set on each new instance before its player's own options.
 *
 * `hwdec=auto` takes whichever hardware decoding the renderer can use: VA-API frames mapped as textures where mpv
 * renders with EGL, otherwise copied back from VA-API or Vulkan. The libmpv the app carries has no youtube-dl or
 * scripts, so their options don't exist and aren't set.
 *
 * Playback caches nothing outside mpv's own buffer, which a desktop can afford to make deep: at 4K's bitrates it still
 * holds the 50 seconds the player reads ahead, and a seek back within the last minutes plays from memory.
 */
internal fun setDesktopOptions(client: MpvClient) {
    OPTIONS.forEach { (name, value) -> client.setOption(name, value) }
}

private val OPTIONS = listOf(
    "config" to "no",
    "terminal" to "no",
    "hwdec" to "auto",
    "demuxer-max-bytes" to "384MiB",
    "demuxer-max-back-bytes" to "128MiB",
)
