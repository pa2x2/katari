package eu.kanade.tachiyomi.ui.video.player.gesture

import androidx.compose.runtime.Composable
import eu.kanade.tachiyomi.ui.video.player.picture.VideoPictureGeometry
import mihon.presentation.kit.shortcut.KeyboardShortcut
import mihon.presentation.kit.shortcut.KeyboardShortcutHandler
import mihon.presentation.kit.shortcut.LocalKeyboardShortcuts
import tachiyomi.domain.entry.model.PictureFraming

/**
 * The player's keys ([VideoPlayerShortcuts]) while the watch screen shows. They leave the keys to the settings panel,
 * the episode list and the subtitle editor while one is open. The zoom keys zoom about the picture's centre.
 * [geometry] is the picture's shape in the player, null until the video has one.
 */
@Composable
internal fun VideoPlayerKeys(
    target: VideoPlayerInputTarget,
    seekStepSeconds: Int,
    geometry: () -> VideoPictureGeometry?,
) {
    val enabled = !target.panelOpen
    Key(VideoPlayerShortcuts.PlayPause, enabled) { target.togglePlayback() }
    Key(VideoPlayerShortcuts.seekBackward(seekStepSeconds), enabled) {
        target.seekStep(VideoPlayerSeekDirection.Backward)
    }
    Key(VideoPlayerShortcuts.seekForward(seekStepSeconds), enabled) {
        target.seekStep(VideoPlayerSeekDirection.Forward)
    }
    Key(VideoPlayerShortcuts.VolumeUp, enabled) { target.stepVolume(VOLUME_STEP_PERCENT) }
    Key(VideoPlayerShortcuts.VolumeDown, enabled) { target.stepVolume(-VOLUME_STEP_PERCENT) }
    Key(VideoPlayerShortcuts.Mute, enabled) { target.toggleMute() }
    Key(VideoPlayerShortcuts.FullScreen, enabled && target.hasFullScreen) { target.toggleFullScreen() }
    Key(VideoPlayerShortcuts.Slower, enabled) { target.stepSpeed(faster = false) }
    Key(VideoPlayerShortcuts.Faster, enabled) { target.stepSpeed(faster = true) }
    Key(VideoPlayerShortcuts.PreviousFrame, enabled) { target.stepFrame(forward = false) }
    Key(VideoPlayerShortcuts.NextFrame, enabled) { target.stepFrame(forward = true) }
    Key(VideoPlayerShortcuts.PreviousEpisode, enabled && target.hasPreviousEpisode) { target.previousEpisode() }
    Key(VideoPlayerShortcuts.NextEpisode, enabled && target.hasNextEpisode) { target.nextEpisode() }
    Key(VideoPlayerShortcuts.Subtitles, enabled && target.hasSubtitles) { target.toggleSubtitles() }
    Key(VideoPlayerShortcuts.Skip, enabled && target.skipOffered) { target.skip() }
    Key(VideoPlayerShortcuts.Episodes, enabled) { target.openEpisodes() }
    Key(VideoPlayerShortcuts.ZoomIn, enabled) { target.zoomBy(geometry(), PICTURE_ZOOM_STEP) }
    Key(VideoPlayerShortcuts.ZoomOut, enabled) { target.zoomBy(geometry(), 1f / PICTURE_ZOOM_STEP) }
    Key(VideoPlayerShortcuts.ZoomFit, enabled) { target.commitPictureFraming(PictureFraming.Fit) }
    val registry = LocalKeyboardShortcuts.current
    Key(VideoPlayerShortcuts.ShowList, enabled = true) { registry?.showList() }
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
    val framing = geometry.zoom(pictureFraming, factor)
    previewPictureFraming(framing)
    commitPictureFraming(framing)
}
