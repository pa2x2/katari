package eu.kanade.tachiyomi.ui.video.player.engine.exoplayer

import androidx.media3.common.C
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.Tracks
import androidx.media3.common.text.CueGroup
import app.katari.video.engine.spi.player.SubtitleCue
import app.katari.video.engine.spi.player.SurfaceViewOutput
import app.katari.video.engine.spi.player.TextureViewOutput
import app.katari.video.engine.spi.player.VideoOutput
import app.katari.video.engine.spi.player.VideoPlayer
import app.katari.video.engine.spi.player.VideoPlayerCapabilities
import app.katari.video.engine.spi.player.VideoPlayerError
import app.katari.video.engine.spi.player.VideoPlayerEvent
import app.katari.video.engine.spi.player.VideoPlayerListener
import app.katari.video.engine.spi.player.VideoPlayerState
import app.katari.video.engine.spi.player.VideoSize
import app.katari.video.engine.spi.tracks.AudioTrackSelection
import app.katari.video.engine.spi.tracks.TextTrackSelection
import app.katari.video.engine.spi.tracks.VideoTrackSelection
import app.katari.video.engine.spi.tracks.VideoTracks
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * A Media3 [Player], ExoPlayer's, as a [VideoPlayer]. The state is taken from the player after each of its events and
 * each command, and on a ticker for the position, which Media3 reports to no listener. A player that is a
 * [VolumeBoost] can boost its volume, and one whose text renderers read a [subtitleDelay] can move its subtitles.
 */
internal class Media3VideoPlayer(
    private val player: Player,
    private val subtitleDelay: SubtitleDelay?,
) : VideoPlayer {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val listeners = mutableListOf<VideoPlayerListener>()
    private val mutableState = MutableStateFlow(VideoPlayerState())
    private var released = false

    // Media3 hands out the same objects until they change, so each is translated once.
    private var tracks: Pair<Tracks, VideoTracks>? = null
    private var cues: Pair<CueGroup, List<SubtitleCue>>? = null
    private var error: Pair<PlaybackException, VideoPlayerError>? = null
    private var output: VideoOutput? = null

    private val media3Listener = object : Player.Listener {
        override fun onPositionDiscontinuity(
            oldPosition: Player.PositionInfo,
            newPosition: Player.PositionInfo,
            reason: Int,
        ) {
            refresh()
            dispatch(
                VideoPlayerEvent.PositionJumped(
                    positionMs = newPosition.positionMs,
                    bySeek = reason == Player.DISCONTINUITY_REASON_SEEK,
                ),
            )
        }

        override fun onRenderedFirstFrame() {
            refresh()
            dispatch(VideoPlayerEvent.FirstFrameRendered)
        }

        override fun onPlayerError(error: PlaybackException) {
            refresh()
            dispatch(VideoPlayerEvent.Failed(errorOf(error)))
        }

        override fun onEvents(player: Player, events: Player.Events) {
            refresh()
        }
    }

    override val state: StateFlow<VideoPlayerState> = mutableState.asStateFlow()

    override val currentPositionMs: Long
        get() = player.currentPosition

    override val capabilities: VideoPlayerCapabilities by lazy {
        VideoPlayerCapabilities(
            volumeBoost = (player as? VolumeBoost)?.canBoostVolume == true,
        )
    }

    init {
        player.addListener(media3Listener)
        refresh()
        scope.launch {
            while (isActive) {
                val playing = mutableState.value.run { isPlaying || playback == VideoPlayerState.Playback.Buffering }
                delay(if (playing) MOVING_POSITION_INTERVAL_MS else STILL_POSITION_INTERVAL_MS)
                refresh()
            }
        }
    }

    override fun addListener(listener: VideoPlayerListener) {
        listeners += listener
    }

    override fun removeListener(listener: VideoPlayerListener) {
        listeners -= listener
    }

    override fun prepare() = command { prepare() }

    override fun play() = command { play() }

    override fun pause() = command { pause() }

    override fun setPlayWhenReady(playWhenReady: Boolean) = command { this.playWhenReady = playWhenReady }

    override fun seekTo(positionMs: Long) = command { seekTo(positionMs) }

    override fun seekBack() = command { seekBack() }

    override fun seekForward() = command { seekForward() }

    override fun stop() = command { stop() }

    override fun release() {
        player.release()
        player.removeListener(media3Listener)
        released = true
        scope.cancel()
    }

    override fun setSpeed(speed: Float) = command { setPlaybackSpeed(speed) }

    override fun setVolume(volume: Float) = command { this.volume = volume }

    override fun setVolumeBoost(percent: Int) {
        (player as? VolumeBoost)?.setVolumeBoost(percent)
    }

    override fun setRepeatOne(repeatOne: Boolean) = command {
        repeatMode = if (repeatOne) Player.REPEAT_MODE_ONE else Player.REPEAT_MODE_OFF
    }

    override fun selectVideo(selection: VideoTrackSelection) = command {
        trackSelectionParameters = trackSelectionParameters.with(selection, currentTracks)
    }

    override fun selectAudio(selection: AudioTrackSelection) = command {
        trackSelectionParameters = trackSelectionParameters.with(selection, currentTracks)
    }

    override fun selectText(selection: TextTrackSelection) = command {
        trackSelectionParameters = trackSelectionParameters.with(selection, currentTracks)
    }

    override fun setSubtitleDelay(delayMs: Long) {
        subtitleDelay?.delayUs = delayMs * 1000L
    }

    override fun setVideoOutput(output: VideoOutput?) {
        if (released || output === this.output) return
        require(output == null || output is SurfaceViewOutput || output is TextureViewOutput) {
            "An Android player can't draw into a ${output?.let { it::class.simpleName }}"
        }
        when (val previous = this.output) {
            is SurfaceViewOutput -> player.clearVideoSurfaceView(previous.view)
            is TextureViewOutput -> player.clearVideoTextureView(previous.view)
        }
        this.output = output
        when (output) {
            is SurfaceViewOutput -> player.setVideoSurfaceView(output.view)
            is TextureViewOutput -> player.setVideoTextureView(output.view)
            else -> Unit
        }
    }

    override fun placePicture(scale: Double, offsetX: Double, offsetY: Double) {
        // ExoPlayer's picture is placed by transforming its output's view.
    }

    // ExoPlayer has no frame stepping, which its capabilities say.
    override fun stepFrame(forward: Boolean) = Unit

    private inline fun command(block: Player.() -> Unit) {
        player.block()
        refresh()
    }

    private fun dispatch(event: VideoPlayerEvent) {
        listeners.toList().forEach { it.onEvent(event) }
    }

    private fun refresh() {
        if (released) return
        mutableState.value = VideoPlayerState(
            positionMs = player.currentPosition,
            bufferedPositionMs = player.bufferedPosition,
            durationMs = player.duration.takeUnless { it == C.TIME_UNSET },
            playback = when (player.playbackState) {
                Player.STATE_BUFFERING -> VideoPlayerState.Playback.Buffering
                Player.STATE_READY -> VideoPlayerState.Playback.Ready
                Player.STATE_ENDED -> VideoPlayerState.Playback.Ended
                else -> VideoPlayerState.Playback.Idle
            },
            playWhenReady = player.playWhenReady,
            isPlaying = player.isPlaying,
            speed = player.playbackParameters.speed,
            volume = player.volume,
            videoSize = player.videoSize
                .takeIf { it.width > 0 && it.height > 0 }
                ?.let { VideoSize(it.width, it.height, it.pixelWidthHeightRatio) },
            tracks = tracksOf(player.currentTracks),
            cues = cuesOf(player.currentCues),
            error = player.playerError?.let(::errorOf),
        )
    }

    private fun tracksOf(media3: Tracks): VideoTracks = tracks
        ?.takeIf { it.first === media3 }?.second
        ?: media3.toVideoTracks().also { tracks = media3 to it }

    private fun cuesOf(media3: CueGroup): List<SubtitleCue> = cues
        ?.takeIf { it.first === media3 }?.second
        ?: media3.cues.toSubtitleCues().also { cues = media3 to it }

    private fun errorOf(media3: PlaybackException): VideoPlayerError = error
        ?.takeIf { it.first === media3 }?.second
        ?: media3.toVideoPlayerError().also { error = media3 to it }

    private companion object {
        // As often as the timeline needs while the position moves; while it stands, only the buffered part grows.
        const val MOVING_POSITION_INTERVAL_MS = 250L
        const val STILL_POSITION_INTERVAL_MS = 750L
    }
}
