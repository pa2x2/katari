package eu.kanade.tachiyomi.ui.video.player.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import dev.icerock.moko.resources.StringResource
import eu.kanade.tachiyomi.source.entry.VideoPlaybackOption
import mihon.presentation.kit.chip.FilterChip
import mihon.presentation.kit.content.Text
import mihon.presentation.kit.list.ChipsRow
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
