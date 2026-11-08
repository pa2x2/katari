package eu.kanade.tachiyomi.ui.video.player.components

import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import eu.kanade.tachiyomi.ui.video.player.gesture.VideoPlayerShortcuts
import eu.kanade.tachiyomi.ui.video.player.picture.icon
import eu.kanade.tachiyomi.ui.video.player.picture.label
import mihon.icons.materialsymbols.MaterialSymbols
import mihon.icons.materialsymbols.automirroredroundedfilled.ViewList
import mihon.icons.materialsymbols.rounded.Fullscreen
import mihon.icons.materialsymbols.rounded.FullscreenExit
import mihon.icons.materialsymbols.rounded.MobileRotate
import mihon.icons.materialsymbols.rounded.Speed
import mihon.icons.materialsymbols.rounded.Subtitles
import mihon.icons.materialsymbols.roundedfilled.SkipNext
import mihon.icons.materialsymbols.roundedfilled.SkipPrevious
import mihon.presentation.kit.button.IconButton
import mihon.presentation.kit.button.IconButtonSize
import mihon.presentation.kit.navigation.MediaToolbar
import mihon.presentation.kit.navigation.MediaToolbarButton
import mihon.presentation.kit.navigation.mediaToolbarSlot
import mihon.presentation.kit.theme.ContentTone
import tachiyomi.domain.entry.model.PictureFraming
import tachiyomi.i18n.*
import tachiyomi.presentation.core.i18n.stringResource

/**
 * The row under the timeline: episode skipping and the settings people change mid-episode on the left, the episode
 * list on the right. A missing previous or next episode drops its button, and Subtitles shows only when the stream has
 * subtitles to pick from. Rotate is null where the window doesn't turn or ignores the app's orientation, which drops it
 * too. The picture button switches between Fit and Fill, and takes a pinch's zoom back to Fit. [volume] follows it
 * where the controls set the volume, and the full-screen button closes the row where the window has a full screen
 * ([fullScreen] says whether it is in it): both desktop's, which Android's row never shows.
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
    onRotate: (() -> Unit)?,
    onTogglePicture: () -> Unit,
    onOpenEpisodes: () -> Unit,
    volume: (@Composable () -> Unit)?,
    fullScreen: Boolean?,
    onToggleFullScreen: () -> Unit,
    modifier: Modifier = Modifier,
) {
    MediaToolbar(modifier = modifier) {
        if (hasPreviousEpisode) {
            IconButton(
                icon = MaterialSymbols.RoundedFilled.SkipPrevious,
                contentDescription = stringResource(MR.strings.anime_playback_previous_episode),
                onClick = onPreviousEpisode,
                modifier = Modifier.videoPlayerControl(),
                size = IconButtonSize.Large,
                tone = ContentTone.OnMedia,
                shortcut = VideoPlayerShortcuts.PreviousEpisode,
            )
        }
        if (hasNextEpisode) {
            IconButton(
                icon = MaterialSymbols.RoundedFilled.SkipNext,
                contentDescription = stringResource(MR.strings.anime_playback_next_episode),
                onClick = onNextEpisode,
                modifier = Modifier.videoPlayerControl(),
                size = IconButtonSize.Large,
                tone = ContentTone.OnMedia,
                shortcut = VideoPlayerShortcuts.NextEpisode,
            )
        }
        MediaToolbarButton(
            icon = MaterialSymbols.Rounded.Speed,
            label = playbackSpeedLabel,
            onClick = onOpenSpeed,
            iconDescription = stringResource(MR.strings.anime_playback_speed),
            modifier = Modifier.mediaToolbarSlot(labelDropOrder = SPEED_LABEL_DROP_ORDER).videoPlayerControl(),
        )
        if (showSubtitles) {
            IconButton(
                icon = MaterialSymbols.Rounded.Subtitles,
                contentDescription = stringResource(MR.strings.anime_playback_subtitles),
                onClick = onOpenSubtitles,
                modifier = Modifier.videoPlayerControl(),
                size = IconButtonSize.Large,
                tone = ContentTone.OnMedia,
            )
        }
        if (onRotate != null) {
            IconButton(
                icon = MaterialSymbols.Rounded.MobileRotate,
                contentDescription = stringResource(MR.strings.anime_playback_rotate),
                onClick = onRotate,
                modifier = Modifier.videoPlayerControl(),
                size = IconButtonSize.Large,
                tone = ContentTone.OnMedia,
            )
        }
        MediaToolbarButton(
            icon = picture.icon,
            label = picture.label(),
            onClick = onTogglePicture,
            iconDescription = stringResource(MR.strings.anime_playback_picture),
            modifier = Modifier.mediaToolbarSlot(labelDropOrder = PICTURE_LABEL_DROP_ORDER).videoPlayerControl(),
        )
        volume?.invoke()
        MediaToolbarButton(
            icon = MaterialSymbols.AutoMirroredRoundedFilled.ViewList,
            label = stringResource(MR.strings.episodes),
            onClick = onOpenEpisodes,
            iconDescription = stringResource(MR.strings.action_view_episodes),
            modifier = Modifier
                .mediaToolbarSlot(trailing = true, labelDropOrder = EPISODES_LABEL_DROP_ORDER)
                .videoPlayerControl(),
        )
        if (fullScreen != null) {
            Box(modifier = Modifier.mediaToolbarSlot(trailing = true)) {
                IconButton(
                    icon = if (fullScreen) {
                        MaterialSymbols.Rounded.FullscreenExit
                    } else {
                        MaterialSymbols.Rounded.Fullscreen
                    },
                    contentDescription = stringResource(
                        if (fullScreen) {
                            MR.strings.anime_playback_exit_full_screen
                        } else {
                            MR.strings.anime_playback_enter_full_screen
                        },
                    ),
                    onClick = onToggleFullScreen,
                    modifier = Modifier.videoPlayerControl(),
                    size = IconButtonSize.Large,
                    tone = ContentTone.OnMedia,
                    shortcut = VideoPlayerShortcuts.FullScreen,
                )
            }
        }
    }
}

// When the row is short of room, the episode list's label goes first, then the picture's, then the speed's.
private const val EPISODES_LABEL_DROP_ORDER = 0
private const val PICTURE_LABEL_DROP_ORDER = 1
private const val SPEED_LABEL_DROP_ORDER = 2
