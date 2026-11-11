package eu.kanade.presentation.library.update.report

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import app.katari.icons.materialsymbols.MaterialSymbols
import app.katari.icons.materialsymbols.rounded.CheckCircle
import app.katari.icons.materialsymbols.rounded.Error
import app.katari.icons.materialsymbols.rounded.FiberNew
import app.katari.icons.materialsymbols.rounded.PauseCircle
import app.katari.icons.materialsymbols.rounded.SkipNext
import app.katari.presentation.kit.content.Text
import app.katari.presentation.kit.layout.RowList
import app.katari.presentation.kit.list.RowGroupHeader
import app.katari.presentation.kit.list.RowGroupItem
import app.katari.presentation.kit.list.RowGroupNote
import app.katari.presentation.kit.list.RowGroupPosition
import app.katari.presentation.kit.list.SettingsRow
import app.katari.presentation.kit.list.rowGroupItems
import app.katari.presentation.kit.progress.LinearProgressIndicator
import app.katari.presentation.kit.theme.ContentTone
import app.katari.presentation.kit.theme.TextRole
import dev.icerock.moko.resources.StringResource
import eu.kanade.presentation.library.update.libraryUpdateRunSummaryText
import eu.kanade.presentation.library.update.report.components.ReportRunNote
import eu.kanade.tachiyomi.ui.library.update.report.LibraryUpdateReportViewModel.OtherDeviceReport
import tachiyomi.domain.library.update.model.LibraryUpdateRunSummary
import tachiyomi.i18n.*
import tachiyomi.presentation.core.i18n.stringResource

/**
 * The report of an update another device made. Each entry's result stays on that device, so it shows the update's
 * counts, and leads on to this device's own latest report with [onOpenHere].
 */
@Composable
fun OtherDeviceUpdateReportContent(
    report: OtherDeviceReport,
    contentPadding: PaddingValues,
    onOpenHere: () -> Unit,
) {
    val summary = report.update.summary
    RowList(contentPadding = contentPadding) {
        if (summary.isRunning) {
            item(key = "running") {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            }
        }
        item(key = "header") {
            ReportRunNote(summary.run, summary.checked)
        }
        item(key = "device") {
            val deviceName = report.update.deviceName
            RowGroupNote(
                text = if (deviceName != null) {
                    stringResource(MR.strings.library_update_report_from_device, deviceName)
                } else {
                    stringResource(MR.strings.library_update_report_from_other_device)
                },
            )
        }
        outcomeItems(summary)
        report.here?.let { here ->
            item(key = "header-here") {
                RowGroupHeader(text = stringResource(MR.strings.library_update_report_this_device))
            }
            item(key = "here") {
                RowGroupItem(position = RowGroupPosition.Alone) {
                    SettingsRow(
                        title = stringResource(MR.strings.library_update_report_this_device_update),
                        subtitle = libraryUpdateRunSummaryText(here),
                        onClick = onOpenHere,
                    )
                }
            }
        }
    }
}

private fun LazyListScope.outcomeItems(summary: LibraryUpdateRunSummary) {
    val outcomes = listOf(
        Outcome(MaterialSymbols.Rounded.Error, MR.strings.library_update_report_failed, summary.failed, isError = true),
        Outcome(MaterialSymbols.Rounded.FiberNew, MR.strings.library_update_report_new, summary.newChapters),
        Outcome(MaterialSymbols.Rounded.CheckCircle, MR.strings.library_update_report_checked_entries, summary.checked),
        Outcome(MaterialSymbols.Rounded.SkipNext, MR.strings.library_update_report_skipped, summary.skipped),
        Outcome(
            MaterialSymbols.Rounded.PauseCircle,
            MR.strings.library_update_report_not_checked,
            summary.neverChecked + summary.switchedOff,
        ),
    )
        .filter { it.count > 0 }
    rowGroupItems(outcomes) { outcome ->
        SettingsRow(
            title = stringResource(outcome.title),
            icon = outcome.icon,
            iconAlert = outcome.isError,
            trailing = {
                Text(text = outcome.count.toString(), role = TextRole.Label, tone = ContentTone.Secondary)
            },
        )
    }
}

private class Outcome(
    val icon: ImageVector,
    val title: StringResource,
    val count: Int,
    val isError: Boolean = false,
)
