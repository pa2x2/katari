package eu.kanade.tachiyomi.ui.video.player.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import mihon.icons.materialsymbols.MaterialSymbols
import mihon.icons.materialsymbols.rounded.SkipNext
import mihon.icons.materialsymbols.rounded.SkipPrevious
import mihon.icons.materialsymbols.roundedfilled.Pause
import mihon.icons.materialsymbols.roundedfilled.PlayArrow
import mihon.presentation.kit.content.Icon
import mihon.presentation.kit.progress.LoadingIndicator
import mihon.presentation.kit.surface.Surface
import mihon.presentation.kit.surface.SurfaceContainer
import mihon.presentation.kit.theme.ContentTone
import mihon.presentation.kit.theme.KatariTheme
import tachiyomi.i18n.*
import tachiyomi.presentation.core.i18n.stringResource

@Composable
internal fun VideoPlayerCenterControls(
    isPlaying: Boolean,
    isLoading: Boolean,
    hasPreviousEpisode: Boolean,
    hasNextEpisode: Boolean,
    onPreviousEpisode: () -> Unit,
    onSeekBackward: () -> Unit,
    onTogglePlayback: () -> Unit,
    onSeekForward: () -> Unit,
    onNextEpisode: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val sizes = KatariTheme.sizes
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(KatariTheme.spacing.xxl, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        VideoPlayerTransportButton(
            onClick = onPreviousEpisode,
            enabled = hasPreviousEpisode,
            contentSize = sizes.icon,
        ) { tone ->
            Icon(
                imageVector = MaterialSymbols.Rounded.SkipPrevious,
                contentDescription = stringResource(MR.strings.action_previous_chapter),
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
                LoadingIndicator(modifier = Modifier.fillMaxSize(), color = KatariTheme.colors.onMedia)
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
            onClick = onNextEpisode,
            enabled = hasNextEpisode,
            contentSize = sizes.icon,
        ) { tone ->
            Icon(
                imageVector = MaterialSymbols.Rounded.SkipNext,
                contentDescription = stringResource(MR.strings.action_next_chapter),
                modifier = Modifier.fillMaxSize(),
                tone = tone,
            )
        }
    }
}

/** A round button on the media shade; [content] fills [contentSize] in the tone for its enabled state. */
@Composable
private fun VideoPlayerTransportButton(
    onClick: () -> Unit,
    enabled: Boolean,
    contentSize: Dp,
    content: @Composable (tone: ContentTone) -> Unit,
) {
    Surface(
        onClick = onClick,
        container = SurfaceContainer.MediaScrim,
        shape = KatariTheme.shapes.full,
        enabled = enabled,
    ) {
        Box(
            modifier = Modifier
                .padding(KatariTheme.spacing.l)
                .size(contentSize),
            contentAlignment = Alignment.Center,
        ) {
            content(if (enabled) ContentTone.OnMedia else ContentTone.OnMediaVariant)
        }
    }
}
