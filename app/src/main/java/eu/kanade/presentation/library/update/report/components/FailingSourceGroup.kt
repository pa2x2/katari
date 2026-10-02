package eu.kanade.presentation.library.update.report.components

import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import eu.kanade.presentation.library.update.report.LibraryUpdateReportActions
import mihon.feature.library.update.report.LibraryUpdateReport
import tachiyomi.i18n.*
import tachiyomi.presentation.core.i18n.pluralStringResource
import tachiyomi.presentation.core.i18n.stringResource

/**
 * One row for a source whose checks mostly failed the same way, since pausing the source is the fix.
 *
 * @param name null when the source isn't installed and its name was never recorded.
 */
@Composable
internal fun FailingSourceGroup(
    source: LibraryUpdateReport.FailingSource,
    name: String?,
    isPaused: Boolean,
    actions: LibraryUpdateReportActions,
) {
    Column {
        ReportRow(
            entry = null,
            title = pluralStringResource(
                MR.plurals.library_update_report_source_entries,
                source.items.size,
                name ?: stringResource(MR.strings.library_update_report_unknown_source),
                source.items.size,
            ),
            subtitle = source.error ?: stringResource(MR.strings.library_update_report_unknown_error),
            isError = true,
        )
        ActionChips(start = SOURCE_ACTIONS_START) {
            PauseChip(
                paused = isPaused,
                label = stringResource(MR.strings.library_update_report_pause_source),
                onPausedChange = { actions.onSetSourcePaused(source.sourceId, it) },
            )
        }
    }
}
