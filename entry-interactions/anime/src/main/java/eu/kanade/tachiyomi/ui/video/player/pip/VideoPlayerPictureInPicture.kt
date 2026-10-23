package eu.kanade.tachiyomi.ui.video.player.pip

import android.app.PendingIntent
import android.app.PictureInPictureParams
import android.app.RemoteAction
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.graphics.drawable.Icon
import android.os.Build
import android.util.Rational
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.content.ContextCompat
import androidx.media3.exoplayer.ExoPlayer
import eu.kanade.tachiyomi.ui.video.player.VideoPlayerPlaybackSnapshot
import eu.kanade.tachiyomi.ui.video.player.capturePlaybackSnapshot
import tachiyomi.core.common.i18n.stringResource
import tachiyomi.i18n.*
import mihon.icons.materialsymbols.R as MaterialSymbolsR

/**
 * Picture-in-picture for the player activity: the params the system gets (the video's shape, a play/pause action and,
 * from Android 12, auto-enter while playing), the receiver for that action, and entering the mode from the controls or
 * when the user leaves. [latestSnapshot] is the playback state the params and the action were last built from.
 */
internal class VideoPlayerPictureInPicture(
    private val activity: AppCompatActivity,
    private val currentPlayer: () -> ExoPlayer?,
) {
    var isInPictureInPictureMode by mutableStateOf(false)
        private set
    var latestSnapshot = VideoPlayerPlaybackSnapshot()

    private var supportsPictureInPicture = false
    private var autoEnter by mutableStateOf(false)
    private var pendingOnPause = false
    private var suppressAutomatic = false

    // Playback snapshots arrive several times a second; the system only needs new params when these change.
    private var sentParams: ParamsKey? = null
    private var actionReceiverRegistered = false
    private val actionReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            when (intent?.action) {
                ACTION_TOGGLE_PLAYBACK -> togglePlayback()
            }
        }
    }

    val showsButton: Boolean
        get() = supportsPictureInPicture && Build.VERSION.SDK_INT >= Build.VERSION_CODES.O

    /** False while the activity is or is about to be in picture-in-picture, where the video keeps playing. */
    val pausesOnStop: Boolean
        get() = !isInPictureInPictureMode && !pendingOnPause

    fun onCreate(autoEnter: Boolean) {
        supportsPictureInPicture = activity.packageManager.hasSystemFeature(PackageManager.FEATURE_PICTURE_IN_PICTURE)
        this.autoEnter = autoEnter
        isInPictureInPictureMode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            activity.isInPictureInPictureMode
        } else {
            false
        }
        registerActionReceiver()
    }

    fun onResume(autoEnter: Boolean) {
        suppressAutomatic = false
        this.autoEnter = autoEnter
        pendingOnPause = false
        updateParams(latestSnapshot)
    }

    fun onUserLeaveHint() {
        if (suppressAutomatic) {
            pendingOnPause = false
            return
        }
        pendingOnPause = canAutoEnter()
        if (
            Build.VERSION.SDK_INT < Build.VERSION_CODES.S &&
            pendingOnPause &&
            !enterIfAvailable()
        ) {
            pendingOnPause = false
        }
    }

    fun onModeChanged(isInPictureInPictureMode: Boolean) {
        this.isInPictureInPictureMode = isInPictureInPictureMode
        pendingOnPause = false
    }

    fun onPlaybackSnapshot(snapshot: VideoPlayerPlaybackSnapshot) {
        latestSnapshot = snapshot
        updateParams(snapshot)
    }

    fun enterFromControls() {
        enterIfAvailable()
    }

    /** Keeps the system from taking the player into picture-in-picture while it opens another screen. */
    fun suppressForNavigation() {
        suppressAutomatic = true
        updateParams(latestSnapshot)
    }

    fun onNavigationFailed() {
        suppressAutomatic = false
        updateParams(latestSnapshot)
    }

    fun release() {
        if (!actionReceiverRegistered) return

        activity.unregisterReceiver(actionReceiver)
        actionReceiverRegistered = false
    }

    private fun registerActionReceiver() {
        if (actionReceiverRegistered) return

        ContextCompat.registerReceiver(
            activity,
            actionReceiver,
            IntentFilter(ACTION_TOGGLE_PLAYBACK),
            ContextCompat.RECEIVER_NOT_EXPORTED,
        )
        actionReceiverRegistered = true
    }

    private fun togglePlayback() {
        val player = currentPlayer() ?: return

        if (latestSnapshot.playbackEnded) {
            player.seekTo(0L)
        }

        if (player.isPlaying) {
            player.pause()
        } else {
            player.play()
        }

        latestSnapshot = player.capturePlaybackSnapshot()
        updateParams(latestSnapshot)
    }

    private fun enterIfAvailable(): Boolean {
        if (!canUse()) return false

        pendingOnPause = true
        val params = sendParams(paramsKey(latestSnapshot))
        return activity.enterPictureInPictureMode(params).also { entered ->
            if (!entered) {
                pendingOnPause = false
            }
        }
    }

    private fun canAutoEnter(): Boolean {
        return canUse() &&
            autoEnter &&
            latestSnapshot.isPlaying &&
            !latestSnapshot.isLoading
    }

    private fun canUse(): Boolean {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.O &&
            supportsPictureInPicture &&
            !isInPictureInPictureMode
    }

    private fun updateParams(snapshot: VideoPlayerPlaybackSnapshot) {
        if (!supportsPictureInPicture) return
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return

        val key = paramsKey(snapshot)
        if (key != sentParams) sendParams(key)
    }

    private fun paramsKey(snapshot: VideoPlayerPlaybackSnapshot): ParamsKey {
        val videoSize = currentPlayer()?.videoSize
        val width = videoSize?.width ?: 0
        val height = videoSize?.height ?: 0
        return ParamsKey(
            aspectRatio = if (width > 0 &&
                height > 0
            ) {
                Rational(width, height)
            } else {
                DEFAULT_ASPECT_RATIO
            },
            showsPause = snapshot.isPlaying && !snapshot.playbackEnded,
            autoEnter = !suppressAutomatic &&
                autoEnter &&
                snapshot.isPlaying &&
                !snapshot.isLoading,
        )
    }

    private fun sendParams(key: ParamsKey): PictureInPictureParams {
        val paramsBuilder = PictureInPictureParams.Builder()
            .setAspectRatio(key.aspectRatio)
            .setActions(buildActions(key.showsPause))
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            paramsBuilder.setAutoEnterEnabled(key.autoEnter)
        }
        return paramsBuilder.build().also {
            activity.setPictureInPictureParams(it)
            sentParams = key
        }
    }

    private fun buildActions(showsPause: Boolean): List<RemoteAction> {
        val title = if (showsPause) {
            activity.stringResource(MR.strings.action_pause)
        } else {
            activity.stringResource(MR.strings.action_play)
        }
        val iconRes = if (showsPause) {
            MaterialSymbolsR.drawable.rounded_filled_pause
        } else {
            MaterialSymbolsR.drawable.rounded_filled_play_arrow
        }
        val pendingIntent = PendingIntent.getBroadcast(
            activity,
            0,
            Intent(ACTION_TOGGLE_PLAYBACK).setPackage(activity.packageName),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        return listOf(
            RemoteAction(
                Icon.createWithResource(activity, iconRes),
                title,
                title,
                pendingIntent,
            ),
        )
    }

    private data class ParamsKey(
        val aspectRatio: Rational,
        val showsPause: Boolean,
        val autoEnter: Boolean,
    )

    private companion object {
        const val ACTION_TOGGLE_PLAYBACK = "eu.kanade.tachiyomi.ui.video.player.action.TOGGLE_PLAYBACK"
        val DEFAULT_ASPECT_RATIO = Rational(16, 9)
    }
}
