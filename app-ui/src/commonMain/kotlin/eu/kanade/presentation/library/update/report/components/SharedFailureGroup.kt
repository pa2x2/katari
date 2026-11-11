package eu.kanade.presentation.library.update.report.components

import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import app.katari.icons.materialsymbols.MaterialSymbols
import app.katari.icons.materialsymbols.rounded.KeyboardArrowDown
import app.katari.icons.materialsymbols.rounded.KeyboardArrowUp
import app.katari.library.update.report.LibraryUpdateReport
import app.katari.presentation.kit.chip.AssistChip
import app.katari.presentation.kit.content.Icon
import eu.kanade.presentation.library.update.report.LibraryUpdateReportActions
import tachiyomi.domain.entry.model.Entry
import tachiyomi.i18n.*
import tachiyomi.presentation.core.i18n.pluralStringResource

/**
 * One row for an error behind failures on several sources, such as the device being offline, expanding to the entries
 * it stands for. The sources aren't to blame, so it offers one retry of them all rather than ways to deal with a source.
 */
@Composable
internal fun SharedFailureGroup(
    failure: LibraryUpdateReport.SharedFailure,
    sourceNameOf: (Entry) -> String?,
    actions: LibraryUpdateReportActions,
) {
    var expanded by rememberSaveable { mutableStateOf(false) }
    Column {
        ReportRow(
            entry = null,
            title = pluralStringResource(
                MR.plurals.library_update_report_shared_failure,
                failure.items.size,
                failure.items.size,
                failure.sources,
            ),
            subtitle = failure.error,
            isError = true,
            onClick = { expanded = !expanded },
            actions = {
                ActionChips {
                    AssistChip(
                        label = pluralStringResource(
                            MR.plurals.library_update_report_retry_entries,
                            failure.items.size,
                            failure.items.size,
                        ),
                        onClick = { actions.onRetry(failure.items.map { it.entry }) },
                    )
                }
            },
            trailing = {
                Icon(
                    imageVector = if (expanded) {
                        MaterialSymbols.Rounded.KeyboardArrowUp
                    } else {
                        MaterialSymbols.Rounded.KeyboardArrowDown
                    },
                    contentDescription = null,
                )
            },
        )
        if (expanded) {
            failure.items.forEach { item ->
                ReportRow(
                    entry = item.entry,
                    title = item.entry.title,
                    subtitle = null,
                    sourceName = sourceNameOf(item.entry),
                    onClick = { actions.onClickEntry(item.entry) },
                )
            }
        }
    }
}
