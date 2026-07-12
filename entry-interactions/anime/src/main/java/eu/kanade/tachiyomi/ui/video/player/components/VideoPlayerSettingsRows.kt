package eu.kanade.tachiyomi.ui.video.player.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import dev.icerock.moko.resources.StringResource
import eu.kanade.tachiyomi.source.entry.VideoPlaybackOption
import tachiyomi.presentation.core.components.SettingsChipRow

@Composable
internal fun PlaybackOptionRow(
    options: List<VideoPlaybackOption>,
    titleRes: StringResource,
    selectedKey: String?,
    enabled: Boolean = true,
    onSelect: (String?) -> Unit,
) {
    SettingsChipRow(titleRes) {
        options.forEach { option ->
            FilterChip(
                enabled = enabled,
                selected = option.key == selectedKey,
                onClick = { onSelect(option.key) },
                label = { Text(option.label) },
            )
        }
    }
}

@Composable
internal fun LoadingPlaybackOptionRow(
    titleRes: StringResource,
) {
    SettingsChipRow(titleRes) {
        Row(
            modifier = Modifier.padding(vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
            Text(text = "Loading...")
        }
    }
}
