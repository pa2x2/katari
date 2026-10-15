package eu.kanade.presentation.library.update.report.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import mihon.icons.materialsymbols.MaterialSymbols
import mihon.icons.materialsymbols.rounded.PlayArrow
import mihon.presentation.kit.chip.AssistChip
import mihon.presentation.kit.chip.FilterChip
import mihon.presentation.kit.content.LibraryMetrics
import mihon.presentation.kit.theme.KatariSpacing
import mihon.presentation.kit.theme.KatariTheme
import tachiyomi.i18n.*
import tachiyomi.presentation.core.i18n.stringResource

// Lines the chips up with the row's title: past the cover for entries, past the padding for sources.
internal val ENTRY_ACTIONS_START: Dp =
    KatariSpacing.contentInset + (LibraryMetrics.listRowHeight - KatariSpacing.s * 2) + KatariSpacing.l
internal val SOURCE_ACTIONS_START: Dp = KatariSpacing.contentInset + KatariSpacing.l

@Composable
internal fun ActionChips(start: Dp = ENTRY_ACTIONS_START, content: @Composable () -> Unit) {
    Row(
        modifier = Modifier.padding(
            start = start,
            end = KatariTheme.spacing.contentInset,
            bottom = KatariTheme.spacing.xs,
        ),
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
