package eu.kanade.tachiyomi.ui.video.player.gesture

import androidx.compose.runtime.Composable
import app.katari.presentation.kit.shortcut.KeyboardShortcut
import app.katari.presentation.kit.shortcut.KeyboardShortcutHandler
import app.katari.presentation.kit.shortcut.LocalKeyboardShortcuts
import app.katari.presentation.kit.shortcut.ScreenShortcuts
import eu.kanade.tachiyomi.ui.video.player.picture.VideoPictureGeometry
import tachiyomi.domain.entry.model.PictureFraming

/**
 * The player's keys ([VideoPlayerShortcuts]) while the watch screen shows. They leave the keys to the settings panel,
 * the episode list and the subtitle editor while one is open, all but E, which closes the episode list it opened. The
 * zoom keys zoom about the picture's centre, and the pan keys, there while the picture is zoomed past the player,
 * bring a tenth of the player's size more into view.
 * [geometry] is the picture's shape in the player, null until the video has one.
 */
@Composable
internal fun VideoPlayerKeys(
    target: VideoPlayerInputTarget,
    seekStepSeconds: Int,
    geometry: () -> VideoPictureGeometry?,
) {
    val enabled = !target.panelOpen
    PlaybackKeys(target, seekStepSeconds, enabled)
    Key(VideoPlayerShortcuts.FullScreen, enabled && target.hasFullScreen) { target.toggleFullScreen() }
    Key(VideoPlayerShortcuts.PictureInPicture, enabled && target.hasPictureInPicture) {
        target.enterPictureInPicture()
    }
    Key(VideoPlayerShortcuts.Skip, enabled && target.skipOffered) { target.skip() }
    Key(VideoPlayerShortcuts.Episodes, enabled || target.episodesVisible) { target.toggleEpisodes() }
    Key(VideoPlayerShortcuts.ZoomIn, enabled) { target.zoomBy(geometry(), PICTURE_ZOOM_STEP) }
    Key(VideoPlayerShortcuts.ZoomOut, enabled) { target.zoomBy(geometry(), 1f / PICTURE_ZOOM_STEP) }
    Key(VideoPlayerShortcuts.ZoomFit, enabled) { target.frame(PictureFraming.Fit) }
    val pans = enabled && geometry()?.overflows(target.pictureFraming) == true
    Key(VideoPlayerShortcuts.PanLeft, pans) { target.panBy(geometry(), -1, 0) }
    Key(VideoPlayerShortcuts.PanRight, pans) { target.panBy(geometry(), 1, 0) }
    Key(VideoPlayerShortcuts.PanUp, pans) { target.panBy(geometry(), 0, -1) }
    Key(VideoPlayerShortcuts.PanDown, pans) { target.panBy(geometry(), 0, 1) }
    val registry = LocalKeyboardShortcuts.current
    Key(ScreenShortcuts.ShortcutList, enabled = true) { registry?.toggleList() }
}

/**
 * The player's keys in its picture-in-picture window, which shows the picture alone, fitted: those that play, seek,
 * change episode, volume, speed and subtitles, and I, which [onLeave]s for the main window.
 */
@Composable
internal fun PictureInPictureKeys(target: VideoPlayerInputTarget, onLeave: () -> Unit) {
    PlaybackKeys(target, target.seekStepSeconds, enabled = true)
    Key(VideoPlayerShortcuts.PictureInPicture, enabled = true, action = onLeave)
}

@Composable
private fun PlaybackKeys(target: VideoPlayerInputTarget, seekStepSeconds: Int, enabled: Boolean) {
    Key(VideoPlayerShortcuts.PlayPause, enabled) { target.togglePlayback(showControls = false) }
    Key(VideoPlayerShortcuts.seekBackward(seekStepSeconds), enabled) {
        target.seekStep(VideoPlayerSeekDirection.Backward)
    }
    Key(VideoPlayerShortcuts.seekForward(seekStepSeconds), enabled) {
        target.seekStep(VideoPlayerSeekDirection.Forward)
    }
    Key(VideoPlayerShortcuts.VolumeUp, enabled) { target.stepVolume(VideoPlayerShortcuts.VOLUME_STEP_PERCENT) }
    Key(VideoPlayerShortcuts.VolumeDown, enabled) { target.stepVolume(-VideoPlayerShortcuts.VOLUME_STEP_PERCENT) }
    Key(VideoPlayerShortcuts.Mute, enabled) { target.toggleMute() }
    Key(VideoPlayerShortcuts.Slower, enabled) { target.stepSpeed(faster = false) }
    Key(VideoPlayerShortcuts.Faster, enabled) { target.stepSpeed(faster = true) }
    Key(VideoPlayerShortcuts.PreviousFrame, enabled) { target.stepFrame(forward = false) }
    Key(VideoPlayerShortcuts.NextFrame, enabled) { target.stepFrame(forward = true) }
    Key(VideoPlayerShortcuts.PreviousEpisode, enabled && target.canPlayPreviousEpisode) { target.previousEpisode() }
    Key(VideoPlayerShortcuts.NextEpisode, enabled && target.canPlayNextEpisode) { target.nextEpisode() }
    Key(VideoPlayerShortcuts.Subtitles, enabled && target.hasSubtitles) { target.toggleSubtitles() }
}

@Composable
private fun Key(shortcut: KeyboardShortcut, enabled: Boolean, action: () -> Unit) =
    KeyboardShortcutHandler(shortcut, enabled = enabled, onTrigger = action)

private fun VideoPlayerInputTarget.stepVolume(by: Int) {
    val type = VideoPlayerSideGestureType.Volume
    applySideGestureLevel(type, sideGestureLevel(type) + by)
}

private fun VideoPlayerInputTarget.zoomBy(geometry: VideoPictureGeometry?, factor: Float) {
    geometry ?: return
    frame(geometry.zoom(pictureFraming, factor))
}

// [stepsX] and [stepsY] are toward the part of the picture to bring into view, which moves the other way
private fun VideoPlayerInputTarget.panBy(geometry: VideoPictureGeometry?, stepsX: Int, stepsY: Int) {
    geometry ?: return
    frame(
        geometry.pan(
            pictureFraming,
            deltaX = -stepsX * geometry.width * PAN_STEP_SHARE,
            deltaY = -stepsY * geometry.height * PAN_STEP_SHARE,
        ),
    )
}

// Through the preview, as a gesture goes, so a key hides the controls and shows the picture pill as gestures do
private fun VideoPlayerInputTarget.frame(framing: PictureFraming) {
    previewPictureFraming(framing)
    commitPictureFraming(framing)
}

private const val PAN_STEP_SHARE = 0.1f
