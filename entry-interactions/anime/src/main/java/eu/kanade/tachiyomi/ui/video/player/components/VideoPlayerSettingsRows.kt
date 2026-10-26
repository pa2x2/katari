package eu.kanade.tachiyomi.ui.video.player.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import dev.icerock.moko.resources.StringResource
import eu.kanade.tachiyomi.source.entry.VideoPlaybackOption
import eu.kanade.tachiyomi.ui.video.player.VideoPlayerSubtitleOption
import eu.kanade.tachiyomi.ui.video.player.engine.MpvTrackPick
import eu.kanade.tachiyomi.ui.video.player.tracks.VideoPlayerAudioTrackOption
import mihon.presentation.kit.button.Button
import mihon.presentation.kit.button.ButtonSize
import mihon.presentation.kit.button.ButtonStyle
import mihon.presentation.kit.chip.FilterChip
import mihon.presentation.kit.content.Text
import mihon.presentation.kit.list.ChipsRow
import mihon.presentation.kit.list.SettingsRow
import mihon.presentation.kit.progress.LoadingIndicator
import mihon.presentation.kit.theme.ContentTone
import mihon.presentation.kit.theme.KatariTheme
import mihon.presentation.kit.theme.TextRole
import tachiyomi.i18n.*
import tachiyomi.presentation.core.i18n.stringResource

@Composable
internal fun PlaybackOptionRow(
    options: List<VideoPlaybackOption>,
    titleRes: StringResource,
    selectedKey: String?,
    enabled: Boolean = true,
    tags: Map<String, String> = emptyMap(),
    onSelect: (String?) -> Unit,
) {
    ChipsRow(title = stringResource(titleRes)) {
        options.forEach { option ->
            FilterChip(
                label = option.label,
                selected = option.key == selectedKey,
                onClick = { onSelect(option.key) },
                enabled = enabled,
                tag = tags[option.key],
            )
        }
    }
}

@Composable
internal fun LoadingPlaybackOptionRow(
    titleRes: StringResource,
) {
    ChipsRow(title = stringResource(titleRes)) {
        Row(
            modifier = Modifier.padding(vertical = KatariTheme.spacing.xs),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(KatariTheme.spacing.m),
        ) {
            LoadingIndicator(modifier = Modifier.size(KatariTheme.sizes.iconSmall))
            Text(text = stringResource(MR.strings.loading), role = TextRole.Supporting)
        }
    }
}

/** A drafted dub's or server's options that the source couldn't list, with a way to ask again. */
@Composable
internal fun FailedPlaybackOptionRow(
    titleRes: StringResource,
    onRetry: () -> Unit,
) {
    ChipsRow(title = stringResource(titleRes)) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(KatariTheme.spacing.s),
        ) {
            Text(
                text = stringResource(MR.strings.anime_playback_options_failed),
                role = TextRole.Supporting,
                tone = ContentTone.Error,
            )
            Button(label = stringResource(MR.strings.action_retry), onClick = onRetry, style = ButtonStyle.Text)
        }
    }
}

@Composable
internal fun SubtitlePlaybackOptionRow(
    options: List<VideoPlayerSubtitleOption>,
    selectedKey: String?,
    trackPick: MpvTrackPick,
    onSelect: (String?) -> Unit,
    onOpenSubtitleSettings: (() -> Unit)?,
) {
    Column {
        ChipsRow(
            title = stringResource(MR.strings.anime_playback_subtitles),
            supportingText = mpvOnlyTracksLine(trackPick, options.any { it.needsMpv }),
        ) {
            options.forEach { option ->
                FilterChip(
                    label = option.label,
                    selected = option.key == selectedKey,
                    onClick = { onSelect(option.key) },
                    enabled = !option.needsMpv || trackPick == MpvTrackPick.Switches,
                    tag = mpvOnlyTag(option.needsMpv),
                )
            }
        }
        onOpenSubtitleSettings?.let { openSubtitleSettings ->
            SettingsRow(
                title = stringResource(MR.strings.anime_playback_subtitle_settings),
                trailing = {
                    Button(
                        label = stringResource(MR.strings.action_edit),
                        onClick = openSubtitleSettings,
                        style = ButtonStyle.Text,
                        size = ButtonSize.Small,
                    )
                },
            )
        }
    }
}

@Composable
internal fun AudioTrackRow(
    tracks: List<VideoPlayerAudioTrackOption>,
    currentKey: String?,
    trackPick: MpvTrackPick,
    onSelect: (String) -> Unit,
) {
    // A session pick the stream doesn't have, or that only mpv plays, leaves the stream's own choice playing.
    val selectedKey = currentKey?.takeIf { key -> tracks.any { it.key == key && !it.needsMpv } }
        ?: tracks.firstOrNull { it.selected }?.key
    ChipsRow(
        title = stringResource(MR.strings.anime_playback_audio),
        supportingText = mpvOnlyTracksLine(trackPick, tracks.any { it.needsMpv }),
    ) {
        tracks.forEach { track ->
            FilterChip(
                label = track.label ?: stringResource(MR.strings.anime_playback_audio_track, track.number),
                selected = track.key == selectedKey,
                onClick = { onSelect(track.key) },
                enabled = !track.needsMpv || trackPick == MpvTrackPick.Switches,
                tag = mpvOnlyTag(track.needsMpv),
            )
        }
    }
}

@Composable
private fun mpvOnlyTag(needsMpv: Boolean): String? =
    if (needsMpv) stringResource(MR.strings.anime_player_engine_mpv) else null

/** Why the tracks marked mpv can't be picked; null while picking one moves to mpv, or when there are none. */
@Composable
private fun mpvOnlyTracksLine(trackPick: MpvTrackPick, hasMpvOnlyTracks: Boolean): String? = when {
    !hasMpvOnlyTracks -> null
    trackPick == MpvTrackPick.Switches -> null
    trackPick == MpvTrackPick.NeedsAddon -> stringResource(MR.strings.anime_player_engine_tracks_need_addon)
    else -> stringResource(MR.strings.anime_player_engine_tracks_only_mpv)
}

/** Shown under the source rows while a dub, stream or source quality change waits to reload the stream. */
@Composable
internal fun ApplySourceRow(
    onCancel: () -> Unit,
    onApply: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = KatariTheme.spacing.rowInset, vertical = KatariTheme.spacing.xs),
        horizontalArrangement = Arrangement.spacedBy(KatariTheme.spacing.s, Alignment.End),
    ) {
        Button(
            label = stringResource(MR.strings.action_cancel),
            onClick = onCancel,
            style = ButtonStyle.Text,
        )
        Button(
            label = stringResource(MR.strings.action_apply),
            onClick = onApply,
        )
    }
}
