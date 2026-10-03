package eu.kanade.presentation.library.update.report.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import tachiyomi.i18n.*
import tachiyomi.presentation.core.components.material.padding
import tachiyomi.presentation.core.i18n.stringResource

// Lines the chips up with the row's title: past the cover for entries, past the padding for sources.
internal val ENTRY_ACTIONS_START = 76.dp
internal val SOURCE_ACTIONS_START = 32.dp

@Composable
internal fun ActionChips(start: Dp = ENTRY_ACTIONS_START, content: @Composable () -> Unit) {
    Row(
        modifier = Modifier.padding(start = start, end = MaterialTheme.padding.medium, bottom = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        content()
    }
}

@Composable
internal fun ResumeChip(onClick: () -> Unit) {
    AssistChip(
        onClick = onClick,
        label = { Text(stringResource(MR.strings.library_update_report_resume)) },
        leadingIcon = {
            Icon(
                imageVector = Icons.Outlined.PlayArrow,
                contentDescription = null,
                modifier = Modifier.size(AssistChipDefaults.IconSize),
            )
        },
    )
}

@Composable
internal fun PauseChip(paused: Boolean, label: String, onPausedChange: (Boolean) -> Unit) {
    FilterChip(
        selected = paused,
        onClick = { onPausedChange(!paused) },
        label = { Text(label) },
        leadingIcon = if (paused) {
            { Icon(imageVector = Icons.Outlined.Check, contentDescription = null) }
        } else {
            null
        },
    )
}
