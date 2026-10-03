package eu.kanade.presentation.library.update.report.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ExpandLess
import androidx.compose.material.icons.outlined.ExpandMore
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import eu.kanade.presentation.library.update.report.LibraryUpdateReportActions
import eu.kanade.presentation.library.update.sourcePauseStateText
import mihon.feature.library.update.report.LibraryUpdateReport
import tachiyomi.domain.library.update.model.EntryUpdateOutcome
import tachiyomi.domain.library.update.model.SourceUpdatePause
import tachiyomi.i18n.*
import tachiyomi.presentation.core.components.material.padding
import tachiyomi.presentation.core.i18n.pluralStringResource
import tachiyomi.presentation.core.i18n.stringResource

/**
 * One row for a source the run left out because it was paused, expanding to its entries.
 *
 * While the source stays paused it can be resumed, or checked once to see whether it is back; the row sums up how that
 * check went. Once resumed, the next update checks the source, and until then it can be paused again.
 *
 * @param name null when the source isn't installed and its name was never recorded.
 * @param pause the source's pause in effect now, or null when it has been resumed.
 */
@Composable
internal fun PausedSourceGroup(
    source: LibraryUpdateReport.PausedSource,
    name: String?,
    pause: SourceUpdatePause?,
    actions: LibraryUpdateReportActions,
) {
    var expanded by rememberSaveable { mutableStateOf(false) }
    Column {
        ReportRow(
            entry = null,
            title = pluralStringResource(
                MR.plurals.library_update_report_source_entries,
                source.items.size,
                name ?: stringResource(MR.strings.library_update_report_unknown_source),
                source.items.size,
            ),
            subtitle = listOfNotNull(
                if (pause != null) {
                    sourcePauseStateText(pause.until)
                } else {
                    stringResource(MR.strings.library_update_report_resumed)
                },
                checkSummary(source.checked),
            )
                .joinToString(" · "),
            onClick = { expanded = !expanded },
            trailing = {
                Icon(
                    imageVector = if (expanded) Icons.Outlined.ExpandLess else Icons.Outlined.ExpandMore,
                    contentDescription = null,
                )
            },
        )
        if (expanded) {
            source.items.forEach { item ->
                ReportRow(
                    entry = item.entry,
                    title = item.entry.title,
                    subtitle = checkResult(item),
                    modifier = Modifier.padding(start = MaterialTheme.padding.medium),
                    isError = item.status.outcome == EntryUpdateOutcome.FAILED,
                    onClick = { actions.onClickEntry(item.entry) },
                )
            }
        }
        ActionChips(start = SOURCE_ACTIONS_START) {
            if (pause != null) {
                ResumeChip(onClick = { actions.onResumeSource(source.sourceId) })
            } else {
                SourcePauseChip(
                    pause = null,
                    onPause = { actions.onPauseSource(source.sourceId, it) },
                    onResume = {},
                )
            }
            AssistChip(
                onClick = { actions.onRetry(source.items.map { it.entry }) },
                label = { Text(stringResource(MR.strings.library_update_report_check_now)) },
            )
        }
    }
}

/** "Checked: 12 fine, 2 failed", or null before the source has been checked. */
@Composable
private fun checkSummary(checked: List<LibraryUpdateReport.Item>): String? {
    if (checked.isEmpty()) return null
    val failed = checked.count { it.status.outcome == EntryUpdateOutcome.FAILED }
    val fine = checked.size - failed
    val counts = listOfNotNull(
        pluralStringResource(MR.plurals.library_update_report_fine, fine, fine).takeIf { fine > 0 },
        pluralStringResource(MR.plurals.library_updates_failed, failed, failed).takeIf { failed > 0 },
    )
    return stringResource(MR.strings.library_update_report_checked, counts.joinToString(", "))
}

/** What checking the entry while its source was paused found, or null when it hasn't been checked. */
@Composable
private fun checkResult(item: LibraryUpdateReport.Item): String? {
    return when (item.status.outcome) {
        EntryUpdateOutcome.NEW_CHAPTERS -> pluralStringResource(
            MR.plurals.library_updates_new_chapters,
            item.status.newChapters,
            item.status.newChapters,
        )
        EntryUpdateOutcome.NO_CHANGES -> stringResource(MR.strings.library_update_report_no_changes)
        EntryUpdateOutcome.FAILED -> item.status.error ?: stringResource(MR.strings.library_update_report_unknown_error)
        EntryUpdateOutcome.SKIPPED, EntryUpdateOutcome.NOT_CHECKED -> null
    }
}
