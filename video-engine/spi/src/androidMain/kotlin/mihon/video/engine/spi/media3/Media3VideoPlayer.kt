package mihon.video.engine.spi.media3

import androidx.media3.common.C
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.Tracks
import androidx.media3.common.text.CueGroup
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
import mihon.video.engine.spi.chapters.FileChapter
import mihon.video.engine.spi.player.SubtitleCue
import mihon.video.engine.spi.player.VideoPlayer
import mihon.video.engine.spi.player.VideoPlayerCapabilities
import mihon.video.engine.spi.player.VideoPlayerError
import mihon.video.engine.spi.player.VideoPlayerEvent
import mihon.video.engine.spi.player.VideoPlayerListener
import mihon.video.engine.spi.player.VideoPlayerState
import mihon.video.engine.spi.player.VideoSize
import mihon.video.engine.spi.tracks.AudioTrackSelection
import mihon.video.engine.spi.tracks.TextTrackSelection
import mihon.video.engine.spi.tracks.VideoTrackSelection
import mihon.video.engine.spi.tracks.VideoTracks

/**
 * A Media3 [Player] as a [VideoPlayer], for both Android engines: ExoPlayer, and mpv's Media3 player. The state is
 * taken from the player after each of its events and each command, and on a ticker for the position, which Media3
 * reports to no listener. What the player adds beyond Media3 ([SelfDrawnSubtitles], [SelfPlacedPicture],
 * [FileChapters], [VolumeBoost]) becomes the contract's capabilities.
 *
 * The Media3 player stays reachable through [media3Player] for what Media3 still draws and controls on Android.
 */
class Media3VideoPlayer(internal val player: Player) : VideoPlayer {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val listeners = mutableListOf<VideoPlayerListener>()
    private val mutableState = MutableStateFlow(VideoPlayerState())
    private var released = false

    // Media3 hands out the same objects until they change, so each is translated once.
    private var tracks: Pair<Tracks, VideoTracks>? = null
    private var cues: Pair<CueGroup, List<SubtitleCue>>? = null
    private var error: Pair<PlaybackException, VideoPlayerError>? = null
    private var fileChapters = emptyList<FileChapter>()

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
            fileChapters = player is FileChapters,
            volumeBoost = (player as? VolumeBoost)?.canBoostVolume == true,
            drawsSubtitles = player is SelfDrawnSubtitles,
            placesPicture = player is SelfPlacedPicture,
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
        (player as? FileChapters)?.let { chapters ->
            scope.launch {
                chapters.fileChapters.collect {
                    fileChapters = it
                    refresh()
                }
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

    override fun placePicture(scale: Double, offsetX: Double, offsetY: Double) {
        (player as? SelfPlacedPicture)?.placePicture(scale, offsetX, offsetY)
    }

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
            drawsSelectedSubtitle = (player as? SelfDrawnSubtitles)?.drawsSelectedSubtitle == true,
            fileChapters = fileChapters,
            error = player.playerError?.let(::errorOf),
        )
    }

    private fun tracksOf(media3: Tracks): VideoTracks = tracks
        ?.takeIf { it.first === media3 }?.second
        ?: media3.toVideoTracks().also { tracks = media3 to it }

    private fun cuesOf(media3: CueGroup): List<SubtitleCue> = cues
        ?.takeIf { it.first === media3 }?.second
        ?: media3.cues.mapNotNull { cue -> cue.text?.let { SubtitleCue(it.toString()) } }.also { cues = media3 to it }

    private fun errorOf(media3: PlaybackException): VideoPlayerError = error
        ?.takeIf { it.first === media3 }?.second
        ?: media3.toVideoPlayerError().also { error = media3 to it }

    private companion object {
        // As often as the timeline needs while the position moves; while it stands, only the buffered part grows.
        const val MOVING_POSITION_INTERVAL_MS = 250L
        const val STILL_POSITION_INTERVAL_MS = 750L
    }
}

/**
 * The Media3 player under a player of an Android engine, for the parts Media3 still draws and controls on Android: the
 * player view and its subtitles, the seek preview's texture, the feed's view and the media session.
 */
val VideoPlayer.media3Player: Player
    get() = (this as Media3VideoPlayer).player
