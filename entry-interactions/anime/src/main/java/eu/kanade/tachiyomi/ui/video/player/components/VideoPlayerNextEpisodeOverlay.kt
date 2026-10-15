package eu.kanade.tachiyomi.ui.video.player.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import mihon.icons.materialsymbols.MaterialSymbols
import mihon.icons.materialsymbols.rounded.SkipNext
import mihon.presentation.kit.content.Icon
import mihon.presentation.kit.content.Text
import mihon.presentation.kit.progress.LinearProgressIndicator
import mihon.presentation.kit.surface.Surface
import mihon.presentation.kit.surface.SurfaceContainer
import mihon.presentation.kit.theme.KatariTheme
import mihon.presentation.kit.theme.TextRole
import tachiyomi.i18n.*
import tachiyomi.presentation.core.i18n.stringResource

/** The button that skips to the next episode, filling up as the countdown to playing it on its own runs. */
@Composable
internal fun VideoPlayerNextEpisodeOverlay(
    visible: Boolean,
    progress: Float,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val spacing = KatariTheme.spacing
    Box(modifier = modifier) {
        VideoPlayerChromeContainer(
            modifier = Modifier
                .fillMaxSize()
                .videoPlayerSafeContentPadding(includeTop = true, includeBottom = true),
            matchParentHeight = true,
        ) {
            AnimatedVisibility(
                visible = visible,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(end = spacing.xs, bottom = spacing.xxl),
                enter = fadeIn(animationSpec = tween(140)),
                exit = fadeOut(animationSpec = tween(120)),
            ) {
                VideoPlayerNextEpisodeCard(progress = progress, onClick = onClick)
            }
        }
    }
}

@Composable
private fun VideoPlayerNextEpisodeCard(
    progress: Float,
    onClick: () -> Unit,
) {
    val spacing = KatariTheme.spacing
    Surface(
        onClick = onClick,
        container = SurfaceContainer.MediaScrim,
        shape = KatariTheme.shapes.large,
    ) {
        Column(modifier = Modifier.width(IntrinsicSize.Max)) {
            Row(
                modifier = Modifier.padding(horizontal = spacing.l, vertical = spacing.m),
                horizontalArrangement = Arrangement.spacedBy(spacing.m),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    imageVector = MaterialSymbols.Rounded.SkipNext,
                    contentDescription = null,
                    modifier = Modifier.size(KatariTheme.sizes.iconSmall),
                )
                Text(
                    text = stringResource(MR.strings.anime_playback_next_episode),
                    role = TextRole.Label,
                    maxLines = 1,
                )
            }
            LinearProgressIndicator(
                progress = { progress.coerceIn(0f, 1f) },
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}
