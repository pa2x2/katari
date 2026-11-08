package eu.kanade.tachiyomi.ui.video.player.mediacontrols

import mihon.video.engine.spi.player.VideoPlayer

/**
 * The controls the system shows for the playing episode outside the player: play/pause, back and forward by the seek
 * step, the next and previous episodes, and what plays. Android's are its media session and notification; desktop's
 * are MPRIS. They last the whole visit, and each episode's player takes them over.
 */
internal interface VideoPlayerMediaControls {
    /** Where the next-episode button goes; the screen points it at its current handler. */
    var onNextEpisode: () -> Unit

    /** Where the previous-episode button goes, where the controls have one. */
    var onPreviousEpisode: () -> Unit

    fun attach(
        player: VideoPlayer,
        metadata: VideoMetadata,
        seekStepSeconds: Int,
    )

    /** Which episode buttons work now: none while another episode opens, as on the player's own controls. */
    fun setEpisodeNavigation(canPlayPrevious: Boolean, canPlayNext: Boolean)
}
