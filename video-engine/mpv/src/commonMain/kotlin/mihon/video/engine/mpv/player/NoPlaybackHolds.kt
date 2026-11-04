package mihon.video.engine.mpv.player

import mihon.video.engine.spi.player.VideoPlayerState

/** Holds nothing outside the player, for one that never plays sound, or a platform that has nothing to hold. */
internal object NoPlaybackHolds : WatchedPlaybackHolds {
    override val allowsPlaying: Boolean = true

    override val volumeMultiplier: Float = 1f

    override fun setPlayWhenReady(playWhenReady: Boolean, playback: VideoPlayerState.Playback) = Unit

    override fun update(playWhenReady: Boolean, playback: VideoPlayerState.Playback) = Unit

    override fun release() = Unit
}
