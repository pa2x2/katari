package mihon.video.engine.mpv.session

import android.content.Context
import mihon.video.engine.mpv.client.MpvClient

/**
 * How the add-on's mpv draws, decodes and plays sound on Android, set on each new instance before its player's own
 * options. `config` stays on for the fonts in [mpvConfigDirectory]. youtube-dl and scripts, which Katari never uses,
 * stay off.
 */
internal fun setAndroidOptions(client: MpvClient, context: Context) {
    OPTIONS.forEach { (name, value) -> client.setOption(name, value) }
    client.setOption("config-dir", mpvConfigDirectory(context).path)
}

private val OPTIONS = listOf(
    "config" to "yes",
    "gpu-context" to "android",
    "opengl-es" to "yes",
    // Frames go from the decoder to the GPU without a copy. Android logs a stream of dmabuf `getattr` SELinux denials
    // for it that don't affect playback; `mediacodec-copy` has none, at the cost of a copy per frame.
    "hwdec" to "mediacodec,mediacodec-copy",
    "hwdec-codecs" to "h264,hevc,mpeg4,mpeg2video,vp8,vp9,av1",
    "ao" to "audiotrack,opensles",
    "ytdl" to "no",
    "load-scripts" to "no",
)
