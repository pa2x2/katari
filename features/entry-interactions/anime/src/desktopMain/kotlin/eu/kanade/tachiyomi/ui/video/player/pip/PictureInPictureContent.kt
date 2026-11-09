package eu.kanade.tachiyomi.ui.video.player.pip

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.onPointerEvent
import app.katari.icons.materialsymbols.MaterialSymbols
import app.katari.icons.materialsymbols.rounded.Close
import app.katari.icons.materialsymbols.rounded.OpenInFull
import app.katari.icons.materialsymbols.roundedfilled.Pause
import app.katari.icons.materialsymbols.roundedfilled.PlayArrow
import app.katari.icons.materialsymbols.roundedfilled.SkipNext
import app.katari.icons.materialsymbols.roundedfilled.SkipPrevious
import app.katari.presentation.kit.button.IconButton
import app.katari.presentation.kit.button.IconButtonSize
import app.katari.presentation.kit.progress.LinearProgressIndicator
import app.katari.presentation.kit.shortcut.KeyboardShortcut
import app.katari.presentation.kit.surface.SurfaceContainer
import app.katari.presentation.kit.surface.background
import app.katari.presentation.kit.theme.ContentTone
import app.katari.presentation.kit.theme.KatariTheme
import eu.kanade.tachiyomi.ui.video.player.components.seekBackwardIcon
import eu.kanade.tachiyomi.ui.video.player.components.seekForwardIcon
import eu.kanade.tachiyomi.ui.video.player.gesture.PictureInPictureKeys
import eu.kanade.tachiyomi.ui.video.player.gesture.VideoPlayerSeekDirection
import eu.kanade.tachiyomi.ui.video.player.gesture.VideoPlayerShortcuts
import eu.kanade.tachiyomi.ui.video.player.surface.VideoSurface
import tachiyomi.domain.entry.model.PictureFraming
import tachiyomi.i18n.*
import tachiyomi.presentation.core.i18n.stringResource

/**
 * The picture-in-picture window's content: [scene]'s picture, fitted, under its subtitles and the small end screen,
 * and while the pointer is over the window, its controls: back to the full player ([onReturn]), close ([onClose]),
 * previous episode, back and forward by the seek step, play/pause, next episode, and a thin line for how far the
 * episode has played. The end screen takes the controls' place while it shows. The window answers the player's keys
 * for what it shows ([PictureInPictureKeys]).
 */
@OptIn(ExperimentalComposeUiApi::class)
@Composable
internal fun PictureInPictureContent(
    scene: VideoPlayerPictureInPictureScene,
    onReturn: () -> Unit,
    onClose: () -> Unit,
) {
    var pointerInside by remember { mutableStateOf(false) }
    val input = scene.input
    PictureInPictureKeys(target = input, onLeave = onReturn)
    Box(
        modifier = Modifier
            .fillMaxSize()
            .onPointerEvent(PointerEventType.Enter) { pointerInside = true }
            .onPointerEvent(PointerEventType.Move) { pointerInside = true }
            .onPointerEvent(PointerEventType.Exit) { pointerInside = false },
    ) {
        VideoSurface(
            player = scene.player,
            framing = { PictureFraming.Fit },
            aspectRatio = { input.videoAspectRatio },
            modifier = Modifier.fillMaxSize(),
        )
        scene.subtitles(Modifier.fillMaxSize())
        AnimatedVisibility(
            visible = pointerInside && !scene.endScreenVisible,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.fillMaxSize(),
        ) {
            Controls(scene, onReturn = onReturn, onClose = onClose)
        }
        scene.endScreen(Modifier.fillMaxSize())
    }
}

@Composable
private fun Controls(scene: VideoPlayerPictureInPictureScene, onReturn: () -> Unit, onClose: () -> Unit) {
    val input = scene.input
    val snapshot = scene.snapshot
    val seekStep = input.seekStepSeconds
    Box(modifier = Modifier.fillMaxSize().background(SurfaceContainer.MediaScrim)) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(KatariTheme.spacing.xs),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            ControlButton(
                icon = MaterialSymbols.Rounded.OpenInFull,
                label = stringResource(MR.strings.anime_playback_back_to_player),
                shortcut = VideoPlayerShortcuts.PictureInPicture,
                onClick = onReturn,
            )
            ControlButton(
                icon = MaterialSymbols.Rounded.Close,
                label = stringResource(MR.strings.action_close),
                onClick = onClose,
            )
        }
        Row(
            modifier = Modifier.align(Alignment.Center),
            horizontalArrangement = Arrangement.spacedBy(KatariTheme.spacing.xs),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            ControlButton(
                icon = MaterialSymbols.RoundedFilled.SkipPrevious,
                label = stringResource(MR.strings.anime_playback_previous_episode),
                shortcut = VideoPlayerShortcuts.PreviousEpisode,
                enabled = input.canPlayPreviousEpisode,
                onClick = input::previousEpisode,
            )
            ControlButton(
                icon = seekBackwardIcon(seekStep),
                label = stringResource(MR.strings.anime_playback_seek_backward, seekStep),
                shortcut = VideoPlayerShortcuts.seekBackward(seekStep),
                onClick = { input.seekStep(VideoPlayerSeekDirection.Backward) },
            )
            ControlButton(
                icon = if (snapshot.isPlaying) {
                    MaterialSymbols.RoundedFilled.Pause
                } else {
                    MaterialSymbols.RoundedFilled.PlayArrow
                },
                label = stringResource(if (snapshot.isPlaying) MR.strings.action_pause else MR.strings.action_play),
                shortcut = VideoPlayerShortcuts.PlayPause,
                size = IconButtonSize.Large,
                onClick = { input.togglePlayback(showControls = true) },
            )
            ControlButton(
                icon = seekForwardIcon(seekStep),
                label = stringResource(MR.strings.anime_playback_seek_forward, seekStep),
                shortcut = VideoPlayerShortcuts.seekForward(seekStep),
                onClick = { input.seekStep(VideoPlayerSeekDirection.Forward) },
            )
            ControlButton(
                icon = MaterialSymbols.RoundedFilled.SkipNext,
                label = stringResource(MR.strings.anime_playback_next_episode),
                shortcut = VideoPlayerShortcuts.NextEpisode,
                enabled = input.canPlayNextEpisode,
                onClick = input::nextEpisode,
            )
        }
        if (snapshot.durationMs > 0L) {
            LinearProgressIndicator(
                progress = { snapshot.positionMs.toFloat() / snapshot.durationMs },
                modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth(),
            )
        }
    }
}

@Composable
private fun ControlButton(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
    shortcut: KeyboardShortcut? = null,
    enabled: Boolean = true,
    size: IconButtonSize = IconButtonSize.Standard,
) {
    IconButton(
        icon = icon,
        contentDescription = label,
        onClick = onClick,
        size = size,
        tone = if (enabled) ContentTone.OnMedia else ContentTone.OnMediaVariant,
        enabled = enabled,
        shortcut = shortcut,
    )
}
