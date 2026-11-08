package mihon.video.engine.mpv.player

import logcat.LogPriority
import mihon.video.engine.mpv.client.MpvLogLevel
import mihon.video.engine.spi.VideoPlayerRole
import tachiyomi.core.common.util.system.logcat

/**
 * What the app's log keeps of a player on mpv, so a playback problem a viewer reports can be traced afterwards: mpv's
 * warnings and errors, such as a stream whose timestamps jump, and the seeks asked of it, which the proxy's dropped
 * connections follow. The seek preview's seeks aren't kept: it seeks on every move of the timeline.
 */
internal class MpvAppLog(private val role: VideoPlayerRole) {
    val player: String = when (role) {
        is VideoPlayerRole.Watched -> "watched"
        VideoPlayerRole.SeekPreview -> "seek preview"
        VideoPlayerRole.Feed -> "feed"
    }

    fun add(prefix: String, level: MpvLogLevel, text: String) {
        val priority = when (level) {
            MpvLogLevel.Fatal, MpvLogLevel.Error -> LogPriority.ERROR
            MpvLogLevel.Warn -> LogPriority.WARN
            else -> return
        }
        logcat(priority) { "mpv ($player): ${mpvLogLine(prefix, text)}" }
    }

    fun seekAsked(seconds: String) {
        if (role == VideoPlayerRole.SeekPreview) return
        logcat(LogPriority.INFO) { "mpv ($player): seek to $seconds s" }
    }
}
