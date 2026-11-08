package eu.kanade.tachiyomi.ui.video.player.background

import android.content.Context
import app.katari.video.engine.spi.player.VideoPlayer
import eu.kanade.tachiyomi.ui.video.player.mediacontrols.VideoMetadata
import eu.kanade.tachiyomi.ui.video.player.mediacontrols.VideoPlayerMediaControls

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
    private var canPlayNext = false

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
    ) {
        val session = mediaSession
            ?: VideoPlayerMediaSession(context) { onNextEpisode() }
                .also { mediaSession = it }
        session.setPlayer(player, metadata, seekStepSeconds, nextEpisodeButton = canPlayNext)
        syncHosting()
    }

    override fun setEpisodeNavigation(canPlayPrevious: Boolean, canPlayNext: Boolean) {
        this.canPlayNext = canPlayNext
        mediaSession?.setNextEpisodeButton(canPlayNext)
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
