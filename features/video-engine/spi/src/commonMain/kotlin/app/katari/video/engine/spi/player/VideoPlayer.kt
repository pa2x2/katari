package app.katari.video.engine.spi.player

import app.katari.video.engine.spi.tracks.AudioTrackSelection
import app.katari.video.engine.spi.tracks.TextTrackSelection
import app.katari.video.engine.spi.tracks.VideoTrackSelection
import kotlinx.coroutines.flow.StateFlow

/**
 * A player of one stream, as everything above the engines sees it: what it plays is [state], what happens once is
 * reported to its listeners, and the rest are commands. Each engine adapts its own player to this once.
 *
 * A player is confined to the UI thread: it is made, commanded, observed and released there, and its listeners and
 * [state] change there. Two watched players may exist at once, as the next episode's is made before the previous one is
 * released, so nothing about a player may be process-wide.
 */
interface VideoPlayer {
    /** Changes as the player does, and a few times a second for the position while the player plays or loads. */
    val state: StateFlow<VideoPlayerState>

    /**
     * The playback position this moment, which [state] only reports every so often. After [seekTo] it is the seek's
     * target, even before the player got there.
     */
    val currentPositionMs: Long

    val capabilities: VideoPlayerCapabilities

    fun addListener(listener: VideoPlayerListener)

    fun removeListener(listener: VideoPlayerListener)

    /** Starts loading the stream; a player that failed loads it again. */
    fun prepare()

    fun play()

    fun pause()

    /** Whether the player plays as soon as it can; unlike [play], it doesn't start loading. */
    fun setPlayWhenReady(playWhenReady: Boolean)

    fun seekTo(positionMs: Long)

    /** Seeks back by the watched role's seek step. */
    fun seekBack()

    /** Seeks forward by the watched role's seek step. */
    fun seekForward()

    /** Where [VideoPlayerCapabilities.stepsFrames], shows the next frame, or the one before, and stays paused. */
    fun stepFrame(forward: Boolean)

    /** Stops playback and lets the stream go; [prepare] loads it again. */
    fun stop()

    fun release()

    fun setSpeed(speed: Float)

    /** The player's own volume, from 0 (silent) to 1, under the device's. */
    fun setVolume(volume: Float)

    /**
     * Plays louder than full volume where [VideoPlayerCapabilities.volumeBoost], for sources mixed too quietly. The
     * boost is on top of the player's own volume and the device's, as a level from [NO_VOLUME_BOOST_PERCENT] (as
     * recorded) to [MAX_VOLUME_BOOST_PERCENT], on mpv's volume scale: its software mixer applies the cube of the
     * level, so 150 is about 10 dB louder.
     */
    fun setVolumeBoost(percent: Int)

    /** Plays the stream again from the start whenever it ends, as the feed's clips do. */
    fun setRepeatOne(repeatOne: Boolean)

    fun selectVideo(selection: VideoTrackSelection)

    fun selectAudio(selection: AudioTrackSelection)

    fun selectText(selection: TextTrackSelection)

    /**
     * Draws the picture into [output] from now on, and no longer where it drew before; null draws it nowhere, as when
     * only the sound plays on. A player draws into one output at a time, and lets go of it when released.
     */
    fun setVideoOutput(output: VideoOutput?)

    /**
     * Where [VideoPlayerCapabilities.placesPicture], shows the picture at [scale] times the size that fits the output,
     * moved from the centre by [offsetX] and [offsetY]: shares of the scaled picture's width and height.
     */
    fun placePicture(scale: Double, offsetX: Double, offsetY: Double)

    companion object {
        const val NO_VOLUME_BOOST_PERCENT = 100
        const val MAX_VOLUME_BOOST_PERCENT = 150
    }
}
