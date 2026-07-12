package eu.kanade.tachiyomi.ui.video.player.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.outlined.Pause
import androidx.compose.material.icons.outlined.SkipNext
import androidx.compose.material.icons.outlined.SkipPrevious
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.i18n.stringResource

private val youtubeLikeControlButtonSize = 60.dp

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
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(30.dp, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        VideoPlayerTransportButton(
            icon = {
                Icon(
                    imageVector = Icons.Outlined.SkipPrevious,
                    modifier = Modifier.size(24.dp),
                    contentDescription = stringResource(MR.strings.action_previous_chapter),
                    tint = Color.White,
                )
            },
            enabled = hasPreviousEpisode,
            onClick = onPreviousEpisode,
        )
        VideoPlayerTransportButton(
            icon = {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(30.dp),
                        color = Color.White,
                        strokeWidth = 2.dp,
                    )
                } else {
                    Icon(
                        imageVector = if (isPlaying) Icons.Outlined.Pause else Icons.Filled.PlayArrow,
                        modifier = Modifier.size(30.dp),
                        contentDescription = if (isPlaying) stringResource(MR.strings.action_pause) else "Play",
                        tint = Color.White,
                    )
                }
            },
            enabled = !isLoading,
            onClick = onTogglePlayback,
        )
        VideoPlayerTransportButton(
            icon = {
                Icon(
                    imageVector = Icons.Outlined.SkipNext,
                    modifier = Modifier.size(24.dp),
                    contentDescription = stringResource(MR.strings.action_next_chapter),
                    tint = Color.White,
                )
            },
            enabled = hasNextEpisode,
            onClick = onNextEpisode,
        )
    }
}

@Composable
internal fun VideoPlayerTransportButton(
    icon: @Composable () -> Unit,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    Box(
        modifier = modifier
            .size(youtubeLikeControlButtonSize)
            .clip(CircleShape)
            .border(
                width = 1.dp,
                color = if (enabled) Color.White.copy(alpha = 0.16f) else Color.White.copy(alpha = 0.08f),
                shape = CircleShape,
            )
            .background(
                if (enabled) Color.Black.copy(alpha = 0.24f) else Color.Black.copy(alpha = 0.1f),
            ),
        contentAlignment = Alignment.Center,
    ) {
        IconButton(
            modifier = Modifier.size(youtubeLikeControlButtonSize),
            enabled = enabled,
            onClick = onClick,
        ) {
            icon()
        }
    }
}
