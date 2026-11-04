package mihon.video.engine.mpv.player

import android.content.Context
import android.os.Looper
import androidx.annotation.OptIn
import androidx.media3.common.AudioAttributes
import androidx.media3.common.Player
import androidx.media3.common.audio.AudioBecomingNoisyManager
import androidx.media3.common.audio.AudioFocusManager
import androidx.media3.common.util.Clock
import androidx.media3.common.util.UnstableApi
import androidx.media3.common.util.WakeLockManager
import androidx.media3.common.util.WifiLockManager

/**
 * What `ExoPlayer.Builder` sets up for the watched player and a `SimpleBasePlayer` has to do itself: audio focus, a
 * pause when the audio output goes away, and wake and Wi-Fi locks while it plays. Media3's own managers do the work,
 * so both engines behave alike. Every call comes on the player's thread, [backgroundLooper] takes the slow parts.
 */
@OptIn(markerClass = [UnstableApi::class])
internal class WatchedPlaybackHolds(
    context: Context,
    playerLooper: Looper,
    backgroundLooper: Looper,
    attributes: AudioAttributes,
    private val listener: Listener,
) {
    interface Listener {
        /** Something outside the player paused it for good: a permanent focus loss or the audio output going away. */
        fun onPauseRequested(@Player.PlayWhenReadyChangeReason reason: Int)

        /** [focusCommand] or [volumeMultiplier] changed. */
        fun onFocusChanged()
    }

    private val audioFocus = AudioFocusManager(
        context,
        playerLooper,
        object : AudioFocusManager.PlayerControl {
            override fun setVolumeMultiplier(volumeMultiplier: Float) {
                listener.onFocusChanged()
            }

            override fun executePlayerCommand(playerCommand: Int) {
                focusCommand = playerCommand
                if (playerCommand == AudioFocusManager.PLAYER_COMMAND_DO_NOT_PLAY) {
                    listener.onPauseRequested(Player.PLAY_WHEN_READY_CHANGE_REASON_AUDIO_FOCUS_LOSS)
                } else {
                    listener.onFocusChanged()
                }
            }
        },
    ).apply { setAudioAttributes(attributes) }

    private val becomingNoisy = AudioBecomingNoisyManager(
        context,
        playerLooper,
        backgroundLooper,
        { listener.onPauseRequested(Player.PLAY_WHEN_READY_CHANGE_REASON_AUDIO_BECOMING_NOISY) },
        Clock.DEFAULT,
    )
    private val wakeLock = WakeLockManager(context, backgroundLooper, Clock.DEFAULT).apply { setEnabled(true) }
    private val wifiLock = WifiLockManager(context, backgroundLooper, Clock.DEFAULT).apply { setEnabled(true) }

    /** One of [AudioFocusManager]'s player commands: whether audio focus lets the player play now. */
    var focusCommand: Int = AudioFocusManager.PLAYER_COMMAND_PLAY_WHEN_READY
        private set

    /** Below 1 while another app's sound ducks this one. */
    val volumeMultiplier: Float get() = audioFocus.volumeMultiplier

    /** Whether focus was last updated for a state other than idle, the one difference [AudioFocusManager] reads. */
    private var focusFollowsPlayback = false

    /**
     * The player's own play/pause was set. Focus is asked for even when the player already meant to play, as
     * ExoPlayer does, so pressing play during a transient loss takes focus back.
     */
    fun setPlayWhenReady(playWhenReady: Boolean, @Player.State playbackState: Int) {
        updateFocus(playWhenReady, playbackState)
        update(playWhenReady, playbackState)
    }

    /**
     * Follows any change of the player's state. Focus is updated only when the player enters or leaves idle: asking
     * again while it means to play would take focus back from the app that holds it for a moment, and inside a focus
     * callback the answer would replace the command that callback just gave.
     */
    fun update(playWhenReady: Boolean, @Player.State playbackState: Int) {
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

    fun release() {
        audioFocus.release()
        becomingNoisy.setEnabled(false)
        wakeLock.setEnabled(false)
        wifiLock.setEnabled(false)
    }
}
