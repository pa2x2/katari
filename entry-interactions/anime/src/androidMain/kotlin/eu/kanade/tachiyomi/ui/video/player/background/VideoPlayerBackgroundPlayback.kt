package eu.kanade.tachiyomi.ui.video.player.background

import android.content.Context
import eu.kanade.tachiyomi.ui.video.player.mediacontrols.VideoPlayerMediaControls
import mihon.video.engine.spi.media3.media3Player
import mihon.video.engine.spi.player.VideoPlayer

/**
 * The player's media session and, while background playback is on, its hosting in [VideoPlaybackService]. The session
 * is created with the first episode's player and kept for the whole visit; each later episode's player takes it over
 * through [attach]. The session controls the Media3 player under each one.
 */
internal class VideoPlayerBackgroundPlayback(
    private val context: Context,
) : VideoPlayerMediaControls {
    var enabled = false
        private set
    private var mediaSession: VideoPlayerMediaSession? = null

    // The session outlives each episode's composition, so its next-episode button calls whichever handler is current.
    override var onNextEpisode: () -> Unit = {}

    fun setEnabled(enabled: Boolean) {
        this.enabled = enabled
        syncHosting()
    }

    override fun attach(player: VideoPlayer, seekStepSeconds: Int, hasNextEpisode: Boolean) {
        val media3Player = player.media3Player
        val session = mediaSession
            ?: VideoPlayerMediaSession(context, media3Player) { onNextEpisode() }
                .also { mediaSession = it }
        session.setPlayer(media3Player, seekStepSeconds, hasNextEpisode = hasNextEpisode)
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
