package eu.kanade.tachiyomi.ui.video.player.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import app.katari.icons.materialsymbols.MaterialSymbols
import app.katari.icons.materialsymbols.rounded.Forward10
import app.katari.icons.materialsymbols.rounded.Forward30
import app.katari.icons.materialsymbols.rounded.Forward5
import app.katari.icons.materialsymbols.rounded.Replay10
import app.katari.icons.materialsymbols.rounded.Replay30
import app.katari.icons.materialsymbols.rounded.Replay5
import app.katari.icons.materialsymbols.roundedfilled.Pause
import app.katari.icons.materialsymbols.roundedfilled.PlayArrow
import app.katari.presentation.kit.content.Icon
import app.katari.presentation.kit.progress.LoadingIndicator
import app.katari.presentation.kit.theme.ContentTone
import app.katari.presentation.kit.theme.KatariTheme
import eu.kanade.tachiyomi.ui.video.player.gesture.VideoPlayerShortcuts
import tachiyomi.i18n.*
import tachiyomi.presentation.core.i18n.stringResource

@Composable
internal fun VideoPlayerCenterControls(
    isPlaying: Boolean,
    isLoading: Boolean,
    seekStepSeconds: Int,
    onSeekBackward: () -> Unit,
    onTogglePlayback: () -> Unit,
    onSeekForward: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val sizes = KatariTheme.sizes
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(KatariTheme.spacing.xxl, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        val seekBackward = stringResource(MR.strings.anime_playback_seek_backward, seekStepSeconds)
        VideoPlayerTransportButton(
            onClick = onSeekBackward,
            enabled = true,
            contentSize = sizes.icon,
            label = seekBackward,
            shortcut = VideoPlayerShortcuts.seekBackward(seekStepSeconds),
        ) { tone ->
            Icon(
                imageVector = seekBackwardIcon(seekStepSeconds),
                contentDescription = seekBackward,
                modifier = Modifier.fillMaxSize(),
                tone = tone,
            )
        }
        val playback = stringResource(if (isPlaying) MR.strings.action_pause else MR.strings.action_play)
        VideoPlayerTransportButton(
            onClick = onTogglePlayback,
            enabled = !isLoading,
            contentSize = sizes.iconLarge,
            label = playback,
            shortcut = VideoPlayerShortcuts.PlayPause,
        ) { tone ->
            if (isLoading) {
                LoadingIndicator(modifier = Modifier.fillMaxSize(), tone = ContentTone.OnMedia)
            } else {
                Icon(
                    imageVector = if (isPlaying) {
                        MaterialSymbols.RoundedFilled.Pause
                    } else {
                        MaterialSymbols.RoundedFilled.PlayArrow
                    },
                    contentDescription = playback,
                    modifier = Modifier.fillMaxSize(),
                    tone = tone,
                )
            }
        }
        val seekForward = stringResource(MR.strings.anime_playback_seek_forward, seekStepSeconds)
        VideoPlayerTransportButton(
            onClick = onSeekForward,
            enabled = true,
            contentSize = sizes.icon,
            label = seekForward,
            shortcut = VideoPlayerShortcuts.seekForward(seekStepSeconds),
        ) { tone ->
            Icon(
                imageVector = seekForwardIcon(seekStepSeconds),
                contentDescription = seekForward,
                modifier = Modifier.fillMaxSize(),
                tone = tone,
            )
        }
    }
}

// Material Symbols draws the step inside the arrow, so each offered step has its own icon.
internal fun seekBackwardIcon(stepSeconds: Int): ImageVector = when (stepSeconds) {
    5 -> MaterialSymbols.Rounded.Replay5
    30 -> MaterialSymbols.Rounded.Replay30
    else -> MaterialSymbols.Rounded.Replay10
}

internal fun seekForwardIcon(stepSeconds: Int): ImageVector = when (stepSeconds) {
    5 -> MaterialSymbols.Rounded.Forward5
    30 -> MaterialSymbols.Rounded.Forward30
    else -> MaterialSymbols.Rounded.Forward10
}
