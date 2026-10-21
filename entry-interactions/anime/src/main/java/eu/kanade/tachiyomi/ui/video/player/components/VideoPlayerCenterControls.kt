package eu.kanade.tachiyomi.ui.video.player.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import mihon.icons.materialsymbols.MaterialSymbols
import mihon.icons.materialsymbols.rounded.Forward10
import mihon.icons.materialsymbols.rounded.Forward30
import mihon.icons.materialsymbols.rounded.Forward5
import mihon.icons.materialsymbols.rounded.Replay10
import mihon.icons.materialsymbols.rounded.Replay30
import mihon.icons.materialsymbols.rounded.Replay5
import mihon.icons.materialsymbols.roundedfilled.Pause
import mihon.icons.materialsymbols.roundedfilled.PlayArrow
import mihon.presentation.kit.content.Icon
import mihon.presentation.kit.progress.LoadingIndicator
import mihon.presentation.kit.theme.ContentTone
import mihon.presentation.kit.theme.KatariTheme
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
        VideoPlayerTransportButton(
            onClick = onSeekBackward,
            enabled = true,
            contentSize = sizes.icon,
        ) { tone ->
            Icon(
                imageVector = seekBackwardIcon(seekStepSeconds),
                contentDescription = stringResource(MR.strings.anime_playback_seek_backward, seekStepSeconds),
                modifier = Modifier.fillMaxSize(),
                tone = tone,
            )
        }
        VideoPlayerTransportButton(
            onClick = onTogglePlayback,
            enabled = !isLoading,
            contentSize = sizes.iconLarge,
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
                    contentDescription = stringResource(
                        if (isPlaying) MR.strings.action_pause else MR.strings.action_play,
                    ),
                    modifier = Modifier.fillMaxSize(),
                    tone = tone,
                )
            }
        }
        VideoPlayerTransportButton(
            onClick = onSeekForward,
            enabled = true,
            contentSize = sizes.icon,
        ) { tone ->
            Icon(
                imageVector = seekForwardIcon(seekStepSeconds),
                contentDescription = stringResource(MR.strings.anime_playback_seek_forward, seekStepSeconds),
                modifier = Modifier.fillMaxSize(),
                tone = tone,
            )
        }
    }
}

// Material Symbols draws the step inside the arrow, so each offered step has its own icon.
private fun seekBackwardIcon(stepSeconds: Int): ImageVector = when (stepSeconds) {
    5 -> MaterialSymbols.Rounded.Replay5
    30 -> MaterialSymbols.Rounded.Replay30
    else -> MaterialSymbols.Rounded.Replay10
}

private fun seekForwardIcon(stepSeconds: Int): ImageVector = when (stepSeconds) {
    5 -> MaterialSymbols.Rounded.Forward5
    30 -> MaterialSymbols.Rounded.Forward30
    else -> MaterialSymbols.Rounded.Forward10
}
