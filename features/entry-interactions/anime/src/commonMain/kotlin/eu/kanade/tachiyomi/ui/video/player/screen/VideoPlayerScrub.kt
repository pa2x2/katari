package eu.kanade.tachiyomi.ui.video.player.screen

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.remember
import app.katari.video.engine.spi.player.VideoPlayer
import eu.kanade.tachiyomi.ui.video.player.pip.VideoPlayerPictureInPicture
import eu.kanade.tachiyomi.ui.video.player.playback.coerceToPlaybackDuration

/**
 * Moving the playback position by dragging along the timeline. While it lasts the timeline shows the target and the
 * seek preview follows it; finishing seeks there.
 */
@Stable
internal class VideoPlayerScrub(
    private val ui: VideoPlayerWatchUiState,
    private val seekPreview: VideoPlayerSeekPreviewSession,
    private val pictureInPicture: VideoPlayerPictureInPicture,
    private val player: VideoPlayer,
) {
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
        ui.isScrubbing = false
        seekPreview.onScrubFinished()
        ui.scrubPositionMs = targetPositionMs
        ui.playbackSnapshot = ui.playbackSnapshot.copy(positionMs = targetPositionMs)
        pictureInPicture.latestSnapshot = ui.playbackSnapshot
        player.seekTo(targetPositionMs)
        ui.registerControllerInteraction(true)
    }
}

@Composable
internal fun rememberVideoPlayerScrub(
    ui: VideoPlayerWatchUiState,
    seekPreview: VideoPlayerSeekPreviewSession,
    pictureInPicture: VideoPlayerPictureInPicture,
    player: VideoPlayer,
): VideoPlayerScrub = remember(ui, seekPreview, pictureInPicture, player) {
    VideoPlayerScrub(ui, seekPreview, pictureInPicture, player)
}
