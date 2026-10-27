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
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.unit.Constraints
import eu.kanade.tachiyomi.ui.video.player.picture.icon
import eu.kanade.tachiyomi.ui.video.player.picture.label
import mihon.icons.materialsymbols.MaterialSymbols
import mihon.icons.materialsymbols.automirroredroundedfilled.ViewList
import mihon.icons.materialsymbols.rounded.MobileRotate
import mihon.icons.materialsymbols.rounded.Speed
import mihon.icons.materialsymbols.rounded.Subtitles
import mihon.icons.materialsymbols.roundedfilled.SkipNext
import mihon.icons.materialsymbols.roundedfilled.SkipPrevious
import mihon.presentation.kit.button.ContentButton
import mihon.presentation.kit.button.IconButton
import mihon.presentation.kit.button.IconButtonSize
import mihon.presentation.kit.content.Icon
import mihon.presentation.kit.content.Text
import mihon.presentation.kit.theme.ContentTone
import mihon.presentation.kit.theme.KatariTheme
import mihon.presentation.kit.theme.TextRole
import tachiyomi.domain.entry.model.PictureFraming
import tachiyomi.i18n.*
import tachiyomi.presentation.core.i18n.stringResource

/**
 * The row under the timeline: episode skipping and the settings people change mid-episode on the left, the episode
 * list on the right. A missing previous or next episode drops its button, and Subtitles shows only when the stream has
 * subtitles to pick from. The picture button switches between Fit and Fill, and takes a pinch's zoom back to Fit.
 */
@Composable
internal fun VideoPlayerTimelineToolbar(
    hasPreviousEpisode: Boolean,
    hasNextEpisode: Boolean,
    playbackSpeedLabel: String,
    picture: PictureFraming,
    showSubtitles: Boolean,
    onPreviousEpisode: () -> Unit,
    onNextEpisode: () -> Unit,
    onOpenSpeed: () -> Unit,
    onOpenSubtitles: () -> Unit,
    onRotate: () -> Unit,
    onTogglePicture: () -> Unit,
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
        ToolbarLabelButton(
            icon = picture.icon,
            label = picture.label(),
            onClick = onTogglePicture,
            iconDescription = stringResource(MR.strings.anime_playback_picture),
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

/**
 * An icon with a label after it. A label that doesn't fit whole is left out, so the button the row squeezes, the last
 * one, keeps its icon and its description rather than an ellipsis.
 */
@Composable
private fun ToolbarLabelButton(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
    iconDescription: String,
) {
    val spacing = KatariTheme.spacing
    ContentButton(onClick = onClick, modifier = Modifier.heightIn(min = KatariTheme.sizes.touchTargetLarge)) {
        Layout(
            modifier = Modifier.padding(horizontal = spacing.m),
            content = {
                Icon(imageVector = icon, contentDescription = iconDescription, tone = ContentTone.OnMedia)
                Text(text = label, role = TextRole.Label, tone = ContentTone.OnMedia, maxLines = 1)
            },
        ) { measurables, constraints ->
            val gap = spacing.s.roundToPx()
            val iconPlaceable = measurables[0].measure(constraints.copy(minWidth = 0, minHeight = 0))
            val labelWidth = measurables[1].maxIntrinsicWidth(constraints.maxHeight)
            val labelPlaceable = measurables[1]
                .takeIf { iconPlaceable.width + gap + labelWidth <= constraints.maxWidth }
                ?.measure(Constraints(maxWidth = labelWidth, maxHeight = constraints.maxHeight))
            val contentWidth = iconPlaceable.width + (labelPlaceable?.let { gap + it.width } ?: 0)
            val width = contentWidth.coerceIn(constraints.minWidth, constraints.maxWidth)
            val height = maxOf(iconPlaceable.height, labelPlaceable?.height ?: 0)
                .coerceIn(constraints.minHeight, constraints.maxHeight)
            layout(width, height) {
                val start = (width - contentWidth) / 2
                iconPlaceable.placeRelative(start, (height - iconPlaceable.height) / 2)
                labelPlaceable?.placeRelative(start + iconPlaceable.width + gap, (height - labelPlaceable.height) / 2)
            }
        }
    }
}
