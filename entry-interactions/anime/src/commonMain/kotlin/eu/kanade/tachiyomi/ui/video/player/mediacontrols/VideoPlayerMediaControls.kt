package eu.kanade.tachiyomi.ui.video.player.mediacontrols

import mihon.video.engine.spi.player.VideoPlayer

/**
 * The controls the system shows for the playing episode outside the player: play/pause, back and forward by the seek
 * step, next episode, and what plays. Android's are its media session and notification; desktop's will be MPRIS. They
 * last the whole visit, and each episode's player takes them over.
 */
internal interface VideoPlayerMediaControls {
    /** Where the next-episode button goes; the screen points it at its current handler. */
    var onNextEpisode: () -> Unit

    fun attach(player: VideoPlayer, seekStepSeconds: Int, hasNextEpisode: Boolean)
}
