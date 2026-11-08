package app.katari.video.engine.mpv.player

import android.content.Context
import android.os.Looper
import androidx.annotation.OptIn
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.Player
import androidx.media3.common.audio.AudioBecomingNoisyManager
import androidx.media3.common.audio.AudioFocusManager
import androidx.media3.common.util.Clock
import androidx.media3.common.util.UnstableApi
import androidx.media3.common.util.WakeLockManager
import androidx.media3.common.util.WifiLockManager
import app.katari.video.engine.spi.player.VideoPlayerState

/**
 * What `ExoPlayer.Builder` sets up for the watched player and mpv's has to do itself: audio focus, a pause when the
 * audio output goes away, and wake and Wi-Fi locks while it plays. Media3's own managers do the work, so both engines
 * behave alike. Every call comes on the player's thread, [backgroundLooper] takes the slow parts.
 */
@OptIn(markerClass = [UnstableApi::class])
internal class AndroidWatchedPlaybackHolds(
    context: Context,
    playerLooper: Looper,
    backgroundLooper: Looper,
    private val listener: WatchedPlaybackHolds.Listener,
) : WatchedPlaybackHolds {

    private val audioFocus = AudioFocusManager(
        context,
        playerLooper,
        object : AudioFocusManager.PlayerControl {
            override fun setVolumeMultiplier(volumeMultiplier: Float) {
                listener.onHoldsChanged()
            }

            override fun executePlayerCommand(playerCommand: Int) {
                focusCommand = playerCommand
                if (playerCommand == AudioFocusManager.PLAYER_COMMAND_DO_NOT_PLAY) {
                    listener.onPauseRequested()
                } else {
                    listener.onHoldsChanged()
                }
            }
        },
    ).apply { setAudioAttributes(AUDIO_ATTRIBUTES) }

    private val becomingNoisy = AudioBecomingNoisyManager(
        context,
        playerLooper,
        backgroundLooper,
        { listener.onPauseRequested() },
        Clock.DEFAULT,
    )
    private val wakeLock = WakeLockManager(context, backgroundLooper, Clock.DEFAULT).apply { setEnabled(true) }
    private val wifiLock = WifiLockManager(context, backgroundLooper, Clock.DEFAULT).apply { setEnabled(true) }

    /** One of [AudioFocusManager]'s player commands: whether audio focus lets the player play now. */
    private var focusCommand: Int = AudioFocusManager.PLAYER_COMMAND_PLAY_WHEN_READY

    override val allowsPlaying: Boolean
        get() = focusCommand == AudioFocusManager.PLAYER_COMMAND_PLAY_WHEN_READY

    override val volumeMultiplier: Float get() = audioFocus.volumeMultiplier

    /** Whether focus was last updated for a state other than idle, the one difference [AudioFocusManager] reads. */
    private var focusFollowsPlayback = false

    /**
     * Focus is asked for even when the player already meant to play, as ExoPlayer does, so pressing play during a
     * transient loss takes focus back.
     */
    override fun setPlayWhenReady(playWhenReady: Boolean, playback: VideoPlayerState.Playback) {
        updateFocus(playWhenReady, playback.media3State)
        update(playWhenReady, playback)
    }

    /**
     * Focus is updated only when the player enters or leaves idle: asking again while it means to play would take
     * focus back from the app that holds it for a moment, and inside a focus callback the answer would replace the
     * command that callback just gave.
     */
    override fun update(playWhenReady: Boolean, playback: VideoPlayerState.Playback) {
        val playbackState = playback.media3State
        if ((playbackState != Player.STATE_IDLE) != focusFollowsPlayback) updateFocus(playWhenReady, playbackState)
        val active = playWhenReady && (playbackState == Player.STATE_READY || playbackState == Player.STATE_BUFFERING)
        becomingNoisy.setEnabled(playWhenReady)
        wakeLock.setStayAwake(active)
        wifiLock.setStayAwake(active)
    }

    private fun updateFocus(playWhenReady: Boolean, @Player.State playbackState: Int) {
        focusFollowsPlayback = playbackState != Player.STATE_IDLE
        focusCommand = audioFocus.updateAudioFocus(playWhenReady, playbackState)
    }

    override fun release() {
        audioFocus.release()
        becomingNoisy.setEnabled(false)
        wakeLock.setEnabled(false)
        wifiLock.setEnabled(false)
    }

    private companion object {
        val AUDIO_ATTRIBUTES: AudioAttributes = AudioAttributes.Builder()
            .setUsage(C.USAGE_MEDIA)
            .setContentType(C.AUDIO_CONTENT_TYPE_MOVIE)
            .build()

        val VideoPlayerState.Playback.media3State: Int
            get() = when (this) {
                VideoPlayerState.Playback.Idle -> Player.STATE_IDLE
                VideoPlayerState.Playback.Buffering -> Player.STATE_BUFFERING
                VideoPlayerState.Playback.Ready -> Player.STATE_READY
                VideoPlayerState.Playback.Ended -> Player.STATE_ENDED
            }
    }
}
