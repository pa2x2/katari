package eu.kanade.tachiyomi.ui.video.player.background

import android.content.Context
import eu.kanade.tachiyomi.ui.video.player.mediacontrols.VideoMetadata
import eu.kanade.tachiyomi.ui.video.player.mediacontrols.VideoPlayerMediaControls
import mihon.video.engine.spi.player.VideoPlayer

/**
 * The player's media session and, while background playback is on, its hosting in [VideoPlaybackService]. The session
 * is created with the first episode's player and kept for the whole visit; each later episode's player takes it over
 * through [attach].
 */
internal class VideoPlayerBackgroundPlayback(
    private val context: Context,
) : VideoPlayerMediaControls {
    var enabled = false
        private set
    private var mediaSession: VideoPlayerMediaSession? = null

    // The session outlives each episode's composition, so its next-episode button calls whichever handler is current.
    override var onNextEpisode: () -> Unit = {}

    // The notification has no previous-episode button: its back button seeks back by the seek step
    override var onPreviousEpisode: () -> Unit = {}

    fun setEnabled(enabled: Boolean) {
        this.enabled = enabled
        syncHosting()
    }

    override fun attach(
        player: VideoPlayer,
        metadata: VideoMetadata,
        seekStepSeconds: Int,
        hasPreviousEpisode: Boolean,
        hasNextEpisode: Boolean,
    ) {
        val session = mediaSession
            ?: VideoPlayerMediaSession(context) { onNextEpisode() }
                .also { mediaSession = it }
        session.setPlayer(player, metadata, seekStepSeconds, hasNextEpisode = hasNextEpisode)
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
