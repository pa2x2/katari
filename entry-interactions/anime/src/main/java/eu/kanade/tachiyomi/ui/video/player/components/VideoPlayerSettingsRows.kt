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
import eu.kanade.tachiyomi.ui.video.player.VideoPlayerAudioTrackOption
import mihon.presentation.kit.button.Button
import mihon.presentation.kit.button.ButtonStyle
import mihon.presentation.kit.chip.FilterChip
import mihon.presentation.kit.content.Text
import mihon.presentation.kit.list.ChipsRow
import mihon.presentation.kit.list.SettingsRow
import mihon.presentation.kit.progress.LoadingIndicator
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
    onSelect: (String?) -> Unit,
) {
    ChipsRow(title = stringResource(titleRes)) {
        options.forEach { option ->
            FilterChip(
                label = option.label,
                selected = option.key == selectedKey,
                onClick = { onSelect(option.key) },
                enabled = enabled,
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

@Composable
internal fun SubtitlePlaybackOptionRow(
    options: List<VideoPlaybackOption>,
    selectedKey: String?,
    onSelect: (String?) -> Unit,
    onOpenSubtitleSettings: (() -> Unit)?,
) {
    Column {
        ChipsRow(title = stringResource(MR.strings.anime_playback_subtitles)) {
            options.forEach { option ->
                FilterChip(
                    label = option.label,
                    selected = option.key == selectedKey,
                    onClick = { onSelect(option.key) },
                )
            }
        }
        onOpenSubtitleSettings?.let { openSubtitleSettings ->
            SettingsRow(
                title = stringResource(MR.strings.anime_playback_subtitle_settings),
                onClick = openSubtitleSettings,
            )
        }
    }
}

@Composable
internal fun AudioTrackRow(
    tracks: List<VideoPlayerAudioTrackOption>,
    currentKey: String?,
    onSelect: (String) -> Unit,
) {
    // A session pick the stream doesn't have leaves the stream's own choice playing.
    val selectedKey = currentKey?.takeIf { key -> tracks.any { it.key == key } }
        ?: tracks.firstOrNull { it.selected }?.key
    ChipsRow(title = stringResource(MR.strings.anime_playback_audio)) {
        tracks.forEach { track ->
            FilterChip(
                label = track.label ?: stringResource(MR.strings.anime_playback_audio_track, track.number),
                selected = track.key == selectedKey,
                onClick = { onSelect(track.key) },
            )
        }
    }
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
            label = stringResource(MR.strings.anime_playback_apply_source),
            onClick = onApply,
        )
    }
}
