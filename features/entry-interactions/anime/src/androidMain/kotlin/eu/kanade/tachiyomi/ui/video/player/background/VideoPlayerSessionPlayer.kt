package eu.kanade.tachiyomi.ui.video.player.background

import android.net.Uri
import android.os.Looper
import androidx.annotation.OptIn
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.PlaybackException
import androidx.media3.common.PlaybackParameters
import androidx.media3.common.Player
import androidx.media3.common.SimpleBasePlayer
import androidx.media3.common.VideoSize
import androidx.media3.common.util.UnstableApi
import androidx.media3.common.util.Util
import app.katari.video.engine.spi.player.VideoPlayer
import app.katari.video.engine.spi.player.VideoPlayerError
import app.katari.video.engine.spi.player.VideoPlayerState
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture
import eu.kanade.tachiyomi.ui.video.player.mediacontrols.VideoMetadata
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

/**
 * The playing episode's [VideoPlayer], whichever engine plays it, as the Media3 player Android's media session runs on:
 * the session, its notification and the system's media controls read the episode's state here and send their play,
 * pause and seek commands through it to the episode's player. One serves the whole visit; each episode's player takes
 * it over through [attach]. The players stay the screen's, which prepares and releases them.
 */
@OptIn(markerClass = [UnstableApi::class])
internal class VideoPlayerSessionPlayer : SimpleBasePlayer(Looper.getMainLooper()) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var following: Job? = null

    private var player: VideoPlayer? = null
    private var item: MediaItemData? = null
    private var seekStepMs = C.DEFAULT_SEEK_BACK_INCREMENT_MS

    // Media3 tells controllers of a new error whenever the error object changes, so each is translated once.
    private var error: Pair<VideoPlayerError, PlaybackException>? = null

    fun attach(player: VideoPlayer, metadata: VideoMetadata, seekStepMs: Long) {
        following?.cancel()
        this.player = player
        this.seekStepMs = seekStepMs
        val mediaMetadata = MediaMetadata.Builder()
            .setTitle(metadata.title)
            .setArtist(metadata.series)
            .setArtworkUri(metadata.artworkUrl?.let(Uri::parse))
            .build()
        // A new uid for each episode, so controllers see the episode change even when its metadata is the same.
        item = MediaItemData.Builder(Any())
            .setMediaItem(MediaItem.Builder().setMediaMetadata(mediaMetadata).build())
            .setMediaMetadata(mediaMetadata)
            .build()
        following = scope.launch { player.state.collect { invalidateState() } }
    }

    /** Stops following the episode's player, for a session that is being released. */
    fun detach() {
        scope.cancel()
        player = null
        item = null
    }

    override fun getState(): State {
        val player = player
        val item = item
        if (player == null || item == null) return State.Builder().build()
        val state = player.state.value
        val playbackState = when (state.playback) {
            VideoPlayerState.Playback.Idle -> Player.STATE_IDLE
            VideoPlayerState.Playback.Buffering -> Player.STATE_BUFFERING
            VideoPlayerState.Playback.Ready -> Player.STATE_READY
            VideoPlayerState.Playback.Ended -> Player.STATE_ENDED
        }
        val durationMs = state.durationMs
        return State.Builder()
            .setAvailableCommands(AVAILABLE_COMMANDS)
            .setPlayWhenReady(state.playWhenReady, Player.PLAY_WHEN_READY_CHANGE_REASON_USER_REQUEST)
            .setPlaybackState(playbackState)
            // A player that means to play and is ready, yet doesn't, is held back by a moment's loss of audio focus.
            .setPlaybackSuppressionReason(
                if (state.playWhenReady && playbackState == Player.STATE_READY && !state.isPlaying) {
                    Player.PLAYBACK_SUPPRESSION_REASON_TRANSIENT_AUDIO_FOCUS_LOSS
                } else {
                    Player.PLAYBACK_SUPPRESSION_REASON_NONE
                },
            )
            .setPlayerError(state.error?.takeIf { playbackState == Player.STATE_IDLE }?.let(::playbackException))
            .setIsLoading(playbackState == Player.STATE_BUFFERING)
            .setSeekBackIncrementMs(seekStepMs)
            .setSeekForwardIncrementMs(seekStepMs)
            .setPlaybackParameters(PlaybackParameters(state.speed))
            .setAudioAttributes(AUDIO_ATTRIBUTES)
            .setVolume(state.volume)
            .setVideoSize(
                state.videoSize?.let { VideoSize(it.width, it.height, it.pixelAspectRatio) } ?: VideoSize.UNKNOWN,
            )
            .setPlaylist(
                listOf(
                    item.buildUpon()
                        .setDurationUs(durationMs?.let(Util::msToUs) ?: C.TIME_UNSET)
                        .setIsSeekable(durationMs != null)
                        .build(),
                ),
            )
            .setCurrentMediaItemIndex(0)
            .setContentPositionMs { player.currentPositionMs }
            .setContentBufferedPositionMs { player.state.value.bufferedPositionMs }
            .build()
    }

    override fun handleSetPlayWhenReady(playWhenReady: Boolean): ListenableFuture<*> = command {
        setPlayWhenReady(playWhenReady)
    }

    override fun handlePrepare(): ListenableFuture<*> = command { prepare() }

    override fun handleStop(): ListenableFuture<*> = command { stop() }

    // The session lets go of its player without releasing it; the screen releases each episode's.
    override fun handleRelease(): ListenableFuture<*> = DONE

    override fun handleSeek(mediaItemIndex: Int, positionMs: Long, seekCommand: Int): ListenableFuture<*> = command {
        when (seekCommand) {
            Player.COMMAND_SEEK_BACK -> seekBack()
            Player.COMMAND_SEEK_FORWARD -> seekForward()
            else -> seekTo(if (positionMs == C.TIME_UNSET) 0L else positionMs)
        }
    }

    override fun handleSetPlaybackParameters(playbackParameters: PlaybackParameters): ListenableFuture<*> = command {
        setSpeed(playbackParameters.speed)
    }

    override fun handleSetVolume(volume: Float, volumeOperationType: Int): ListenableFuture<*> = command {
        setVolume(volume)
    }

    private inline fun command(block: VideoPlayer.() -> Unit): ListenableFuture<*> {
        player?.block()
        return DONE
    }

    private fun playbackException(error: VideoPlayerError): PlaybackException = this.error
        ?.takeIf { it.first === error }?.second
        ?: PlaybackException(error.message, error.cause, error.category.playbackErrorCode())
            .also { this.error = error to it }

    private companion object {
        val DONE: ListenableFuture<*> = Futures.immediateVoidFuture()

        val AUDIO_ATTRIBUTES: AudioAttributes = AudioAttributes.Builder()
            .setUsage(C.USAGE_MEDIA)
            .setContentType(C.AUDIO_CONTENT_TYPE_MOVIE)
            .build()

        val AVAILABLE_COMMANDS: Player.Commands = Player.Commands.Builder()
            .addAll(
                Player.COMMAND_PLAY_PAUSE,
                Player.COMMAND_PREPARE,
                Player.COMMAND_STOP,
                Player.COMMAND_SEEK_TO_DEFAULT_POSITION,
                Player.COMMAND_SEEK_IN_CURRENT_MEDIA_ITEM,
                Player.COMMAND_SEEK_BACK,
                Player.COMMAND_SEEK_FORWARD,
                Player.COMMAND_SET_SPEED_AND_PITCH,
                Player.COMMAND_GET_CURRENT_MEDIA_ITEM,
                Player.COMMAND_GET_TIMELINE,
                Player.COMMAND_GET_METADATA,
                Player.COMMAND_GET_AUDIO_ATTRIBUTES,
                Player.COMMAND_GET_VOLUME,
                Player.COMMAND_SET_VOLUME,
            )
            .build()

        // Controllers read only that playback failed, and roughly why.
        fun VideoPlayerError.Category.playbackErrorCode(): Int = when (this) {
            VideoPlayerError.Category.ConnectionFailed -> PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_FAILED
            VideoPlayerError.Category.ConnectionTimedOut -> PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_TIMEOUT
            VideoPlayerError.Category.Rejected -> PlaybackException.ERROR_CODE_IO_BAD_HTTP_STATUS
            VideoPlayerError.Category.LoadFailed -> PlaybackException.ERROR_CODE_IO_UNSPECIFIED
            VideoPlayerError.Category.Unplayable -> PlaybackException.ERROR_CODE_DECODING_FORMAT_UNSUPPORTED
            VideoPlayerError.Category.Other -> PlaybackException.ERROR_CODE_UNSPECIFIED
        }
    }
}
