package eu.kanade.presentation.library.update.report.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import mihon.icons.materialsymbols.MaterialSymbols
import mihon.icons.materialsymbols.rounded.PlayArrow
import mihon.presentation.kit.chip.AssistChip
import mihon.presentation.kit.chip.FilterChip
import mihon.presentation.kit.theme.KatariTheme
import tachiyomi.i18n.*
import tachiyomi.presentation.core.i18n.stringResource

/** The actions of a report row, set under its text. */
@Composable
internal fun ActionChips(content: @Composable () -> Unit) {
    FlowRow(
        modifier = Modifier.padding(top = KatariTheme.spacing.xs),
        horizontalArrangement = Arrangement.spacedBy(KatariTheme.spacing.s),
    ) {
        content()
    }
}

@Composable
internal fun ResumeChip(onClick: () -> Unit) {
    AssistChip(
        label = stringResource(MR.strings.library_update_report_resume),
        onClick = onClick,
        icon = MaterialSymbols.Rounded.PlayArrow,
    )
}

@Composable
internal fun PauseChip(paused: Boolean, label: String, onPausedChange: (Boolean) -> Unit) {
    FilterChip(
        label = label,
        selected = paused,
        onClick = { onPausedChange(!paused) },
    )
}
