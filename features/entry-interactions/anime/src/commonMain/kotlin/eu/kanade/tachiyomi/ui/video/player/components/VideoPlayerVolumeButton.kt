package eu.kanade.tachiyomi.ui.video.player.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import app.katari.presentation.kit.control.MediaVolumeControl
import app.katari.video.engine.spi.player.VideoPlayer
import eu.kanade.tachiyomi.ui.video.player.gesture.VideoPlayerShortcuts
import eu.kanade.tachiyomi.ui.video.player.volume.VideoPlayerVolumeControl

/** The controls' volume button and slider for [player]; each use is an [onInteraction], which keeps the controls up. */
@Composable
internal fun VideoPlayerVolumeButton(
    control: VideoPlayerVolumeControl,
    player: VideoPlayer?,
    onInteraction: () -> Unit,
) {
    MediaVolumeControl(
        level = control.percent,
        maxLevel = control.maxPercent,
        fullLevel = VideoPlayer.NO_VOLUME_BOOST_PERCENT,
        muted = control.muted,
        onLevelChange = { percent ->
            player?.let { control.setPercent(it, percent) }
            onInteraction()
        },
        onToggleMute = {
            player?.let { control.setMuted(it, !control.muted) }
            onInteraction()
        },
        modifier = Modifier.videoPlayerControl(),
        muteShortcut = VideoPlayerShortcuts.Mute,
    )
}
