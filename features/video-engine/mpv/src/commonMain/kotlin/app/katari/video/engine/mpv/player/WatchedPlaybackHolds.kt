package app.katari.video.engine.mpv.player

import app.katari.video.engine.spi.player.VideoPlayerState

/**
 * What the watched player holds outside itself, such as Android's audio focus, its pause when the audio output goes
 * away, and its wake and Wi-Fi locks while it plays. Every call comes on the UI thread, and so do the listener's.
 */
internal interface WatchedPlaybackHolds {
    /** False while something else may play over the player for a moment, which it waits out still meaning to play. */
    val allowsPlaying: Boolean

    /** Below 1 while another app's sound ducks this one. */
    val volumeMultiplier: Float

    /** The player's own play/pause was set, which may take the holds back from whoever has them now. */
    fun setPlayWhenReady(playWhenReady: Boolean, playback: VideoPlayerState.Playback)

    /** Follows any other change of the player's state. */
    fun update(playWhenReady: Boolean, playback: VideoPlayerState.Playback)

    fun release()

    interface Listener {
        /** Something outside the player paused it for good, such as another app taking audio focus. */
        fun onPauseRequested()

        /** [allowsPlaying] or [volumeMultiplier] changed. */
        fun onHoldsChanged()
    }
}
