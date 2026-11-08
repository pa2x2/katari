package app.katari.video.engine.spi.player

/** What happens once in a player, as opposed to what it is, which is its [VideoPlayer.state]. */
sealed interface VideoPlayerEvent {
    /**
     * The position jumped instead of moving on as the player played, to [positionMs]; [bySeek] when a seek did it,
     * rather than the player itself, such as when it starts a repeated stream over.
     */
    data class PositionJumped(val positionMs: Long, val bySeek: Boolean) : VideoPlayerEvent

    /** The first frame of the stream is on screen. */
    data object FirstFrameRendered : VideoPlayerEvent

    /** Playback stopped on [error], which [VideoPlayerState.error] holds from then on. */
    data class Failed(val error: VideoPlayerError) : VideoPlayerEvent
}

/** Called on the UI thread, after the player's [VideoPlayer.state] took in what the event changed. */
fun interface VideoPlayerListener {
    fun onEvent(event: VideoPlayerEvent)
}
