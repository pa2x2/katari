package eu.kanade.tachiyomi.ui.video.player.background

import android.content.Context
import androidx.media3.common.Player

/**
 * The player's media session and, while background playback is on, its hosting in [VideoPlaybackService]. The session
 * is created with the first episode's player and kept for the whole visit; each later episode's player takes it over
 * through [attach].
 */
internal class VideoPlayerBackgroundPlayback(
    private val context: Context,
) {
    var enabled = false
        private set
    private var mediaSession: VideoPlayerMediaSession? = null

    // The session outlives each episode's composition, so its next-episode button calls whichever handler is current.
    var onNextEpisode: () -> Unit = {}

    fun setEnabled(enabled: Boolean) {
        this.enabled = enabled
        syncHosting()
    }

    fun attach(player: Player, seekStepSeconds: Int, hasNextEpisode: Boolean) {
        val session = mediaSession
            ?: VideoPlayerMediaSession(context, player) { onNextEpisode() }
                .also { mediaSession = it }
        session.setPlayer(player, seekStepSeconds, hasNextEpisode = hasNextEpisode)
        syncHosting()
    }

    fun release() {
        VideoPlaybackService.stopHosting(context)
        mediaSession?.release()
        mediaSession = null
    }

    private fun syncHosting() {
        val session = mediaSession ?: return
        if (enabled) {
            VideoPlaybackService.host(context, session.session)
        } else {
            VideoPlaybackService.stopHosting(context)
        }
    }
}
