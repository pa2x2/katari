package eu.kanade.presentation.library.update.report.components

import androidx.compose.runtime.Composable
import app.katari.presentation.kit.list.RowGroupNote
import eu.kanade.presentation.util.relativeTimeSpanString
import tachiyomi.domain.library.update.model.LibraryUpdateRun
import tachiyomi.domain.library.update.model.LibraryUpdateTrigger
import tachiyomi.i18n.*
import tachiyomi.presentation.core.i18n.pluralStringResource
import tachiyomi.presentation.core.i18n.stringResource

/** "2 hours ago · Automatic · 412 entries considered", heading a report. */
@Composable
internal fun ReportRunNote(run: LibraryUpdateRun) {
    RowGroupNote(
        text = listOf(
            relativeTimeSpanString(run.startedAt),
            stringResource(run.trigger.labelRes),
            pluralStringResource(MR.plurals.library_update_report_considered, run.librarySize, run.librarySize),
        )
            .joinToString(" · "),
    )
}

private val LibraryUpdateTrigger.labelRes
    get() = when (this) {
        LibraryUpdateTrigger.AUTOMATIC -> MR.strings.library_update_trigger_automatic
        LibraryUpdateTrigger.MANUAL -> MR.strings.library_update_trigger_manual
        LibraryUpdateTrigger.SELECTION -> MR.strings.library_update_trigger_selection
    }
