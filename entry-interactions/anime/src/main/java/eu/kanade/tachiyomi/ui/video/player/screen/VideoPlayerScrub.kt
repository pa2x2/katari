package eu.kanade.tachiyomi.ui.video.player.screen

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.media3.common.Player
import eu.kanade.tachiyomi.ui.video.player.pip.VideoPlayerPictureInPicture
import eu.kanade.tachiyomi.ui.video.player.playback.coerceToPlaybackDuration

/**
 * Moving the playback position by dragging, along the timeline or by swiping sideways on the video. While it lasts
 * the timeline shows the target and the seek preview follows it; finishing seeks there, and cancelling leaves playback
 * where it was.
 */
@Stable
internal class VideoPlayerScrub(
    private val ui: VideoPlayerWatchUiState,
    private val seekPreview: VideoPlayerSeekPreviewSession,
    private val pictureInPicture: VideoPlayerPictureInPicture,
    private val player: Player,
) {
    /** How far a sideways swipe has moved the target from where it started; null outside a swipe. */
    var swipeDeltaMs by mutableStateOf<Long?>(null)
        private set
    private var swipeStartMs = 0L

    fun start() {
        ui.registerControllerInteraction(true)
        ui.isScrubbing = true
        ui.scrubPositionMs = ui.playbackSnapshot.positionMs
        seekPreview.onScrubStarted(
            positionMs = ui.playbackSnapshot.positionMs,
            durationMs = ui.playbackSnapshot.durationMs,
        )
    }

    fun moveTo(positionMs: Long) {
        val targetPositionMs = positionMs.coerceToPlaybackDuration(ui.playbackSnapshot.durationMs)
        ui.scrubPositionMs = targetPositionMs
        seekPreview.onScrubPositionChange(targetPositionMs, ui.playbackSnapshot.durationMs)
    }

    fun finish() {
        val targetPositionMs = ui.scrubPositionMs.coerceToPlaybackDuration(ui.playbackSnapshot.durationMs)
        endScrubbing()
        ui.scrubPositionMs = targetPositionMs
        ui.playbackSnapshot = ui.playbackSnapshot.copy(positionMs = targetPositionMs)
        pictureInPicture.latestSnapshot = ui.playbackSnapshot
        player.seekTo(targetPositionMs)
        ui.registerControllerInteraction(true)
    }

    fun cancel() {
        endScrubbing()
        ui.scrubPositionMs = ui.playbackSnapshot.positionMs
    }

    /** Starts a sideways swipe from the playing position; false while the episode's length is unknown. */
    fun startSwipe(): Boolean {
        if (ui.playbackSnapshot.durationMs <= 0L) return false
        start()
        swipeStartMs = ui.scrubPositionMs
        swipeDeltaMs = 0L
        return true
    }

    fun swipeBy(deltaMs: Long) {
        moveTo(swipeStartMs + deltaMs)
        swipeDeltaMs = ui.scrubPositionMs - swipeStartMs
    }

    private fun endScrubbing() {
        ui.isScrubbing = false
        seekPreview.onScrubFinished()
        swipeDeltaMs = null
    }
}

@Composable
internal fun rememberVideoPlayerScrub(
    ui: VideoPlayerWatchUiState,
    seekPreview: VideoPlayerSeekPreviewSession,
    pictureInPicture: VideoPlayerPictureInPicture,
    player: Player,
): VideoPlayerScrub = remember(ui, seekPreview, pictureInPicture, player) {
    VideoPlayerScrub(ui, seekPreview, pictureInPicture, player)
}
