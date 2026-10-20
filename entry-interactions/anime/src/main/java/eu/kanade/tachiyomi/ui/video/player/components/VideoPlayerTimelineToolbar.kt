package eu.kanade.tachiyomi.ui.video.player.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextOverflow
import mihon.icons.materialsymbols.MaterialSymbols
import mihon.icons.materialsymbols.automirroredroundedfilled.ViewList
import mihon.icons.materialsymbols.rounded.MobileRotate
import mihon.icons.materialsymbols.rounded.Speed
import mihon.icons.materialsymbols.rounded.Subtitles
import mihon.icons.materialsymbols.roundedfilled.SkipNext
import mihon.icons.materialsymbols.roundedfilled.SkipPrevious
import mihon.presentation.kit.button.IconButton
import mihon.presentation.kit.button.IconButtonSize
import mihon.presentation.kit.content.Icon
import mihon.presentation.kit.content.Text
import mihon.presentation.kit.surface.Surface
import mihon.presentation.kit.surface.SurfaceContainer
import mihon.presentation.kit.theme.ContentTone
import mihon.presentation.kit.theme.KatariTheme
import mihon.presentation.kit.theme.TextRole
import tachiyomi.i18n.*
import tachiyomi.presentation.core.i18n.stringResource

/**
 * The row under the timeline: episode skipping and the settings people change mid-episode on the left, the episode
 * list on the right. A missing previous or next episode drops its button, and Subtitles shows only when the stream has
 * subtitles to pick from.
 */
@Composable
internal fun VideoPlayerTimelineToolbar(
    hasPreviousEpisode: Boolean,
    hasNextEpisode: Boolean,
    playbackSpeedLabel: String,
    showSubtitles: Boolean,
    onPreviousEpisode: () -> Unit,
    onNextEpisode: () -> Unit,
    onOpenSpeed: () -> Unit,
    onOpenSubtitles: () -> Unit,
    onRotate: () -> Unit,
    onOpenEpisodes: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(KatariTheme.spacing.xs),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (hasPreviousEpisode) {
            IconButton(
                icon = MaterialSymbols.RoundedFilled.SkipPrevious,
                contentDescription = stringResource(MR.strings.anime_playback_previous_episode),
                onClick = onPreviousEpisode,
                size = IconButtonSize.Large,
                tone = ContentTone.OnMedia,
            )
        }
        if (hasNextEpisode) {
            IconButton(
                icon = MaterialSymbols.RoundedFilled.SkipNext,
                contentDescription = stringResource(MR.strings.anime_playback_next_episode),
                onClick = onNextEpisode,
                size = IconButtonSize.Large,
                tone = ContentTone.OnMedia,
            )
        }
        ToolbarLabelButton(
            icon = MaterialSymbols.Rounded.Speed,
            label = playbackSpeedLabel,
            onClick = onOpenSpeed,
            iconDescription = stringResource(MR.strings.anime_playback_speed),
        )
        if (showSubtitles) {
            IconButton(
                icon = MaterialSymbols.Rounded.Subtitles,
                contentDescription = stringResource(MR.strings.anime_playback_subtitles),
                onClick = onOpenSubtitles,
                size = IconButtonSize.Large,
                tone = ContentTone.OnMedia,
            )
        }
        IconButton(
            icon = MaterialSymbols.Rounded.MobileRotate,
            contentDescription = stringResource(MR.strings.anime_playback_rotate),
            onClick = onRotate,
            size = IconButtonSize.Large,
            tone = ContentTone.OnMedia,
        )
        Spacer(modifier = Modifier.weight(1f))
        ToolbarLabelButton(
            icon = MaterialSymbols.AutoMirroredRoundedFilled.ViewList,
            label = stringResource(MR.strings.episodes),
            onClick = onOpenEpisodes,
            iconDescription = stringResource(MR.strings.action_view_episodes),
        )
    }
}

@Composable
private fun ToolbarLabelButton(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
    iconDescription: String,
) {
    val spacing = KatariTheme.spacing
    Surface(
        onClick = onClick,
        modifier = Modifier.heightIn(min = KatariTheme.sizes.touchTargetLarge),
        container = SurfaceContainer.None,
        shape = KatariTheme.shapes.large,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = spacing.m),
            horizontalArrangement = Arrangement.spacedBy(spacing.s),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(imageVector = icon, contentDescription = iconDescription, tone = ContentTone.OnMedia)
            Text(
                text = label,
                role = TextRole.Label,
                tone = ContentTone.OnMedia,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}
