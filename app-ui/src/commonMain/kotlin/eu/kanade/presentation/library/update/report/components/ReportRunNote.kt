package eu.kanade.presentation.library.update.report.components

import androidx.compose.runtime.Composable
import app.katari.presentation.kit.list.RowGroupNote
import eu.kanade.presentation.more.stats.components.StatisticsDurationPrecision
import eu.kanade.presentation.more.stats.components.rememberStatisticsDurationFormatter
import eu.kanade.presentation.util.relativeTimeSpanString
import tachiyomi.domain.library.update.model.LibraryUpdateRun
import tachiyomi.domain.library.update.model.LibraryUpdateTrigger
import tachiyomi.i18n.*
import tachiyomi.presentation.core.i18n.stringResource

/**
 * "2 days ago · took 3 min · Automatic · 30 of 412 checked", heading a report; "Running now · …" while the update
 * runs. [checked] counts the entries the update checked out of all it considered.
 */
@Composable
internal fun ReportRunNote(run: LibraryUpdateRun, checked: Int) {
    val formatDuration = rememberStatisticsDurationFormatter(StatisticsDurationPrecision.SECONDS)
    val finishedAt = run.finishedAt
    RowGroupNote(
        text = listOfNotNull(
            if (finishedAt != null) {
                relativeTimeSpanString(finishedAt)
            } else {
                stringResource(MR.strings.library_updates_running)
            },
            finishedAt?.let {
                stringResource(MR.strings.library_update_report_took, formatDuration(it - run.startedAt))
            },
            stringResource(run.trigger.labelRes),
            stringResource(MR.strings.library_update_report_checked_of, checked, run.librarySize),
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
