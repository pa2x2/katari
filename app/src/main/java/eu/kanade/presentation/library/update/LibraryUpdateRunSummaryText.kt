package eu.kanade.presentation.library.update

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.platform.LocalContext
import eu.kanade.presentation.util.relativeTimeSpanString
import mihon.feature.library.update.report.LibraryUpdateRunSummary
import tachiyomi.core.common.i18n.pluralStringResource
import tachiyomi.i18n.*
import tachiyomi.presentation.core.i18n.stringResource

/** "2 hours ago · 12 new chapters · 3 failed · 58 skipped", leaving out the counts that are zero. */
@Composable
@ReadOnlyComposable
fun libraryUpdateRunSummaryText(summary: LibraryUpdateRunSummary): String {
    val time = if (summary.isRunning) {
        stringResource(MR.strings.library_updates_running)
    } else {
        relativeTimeSpanString(summary.run.startedAt)
    }
    return (listOf(time) + LocalContext.current.libraryUpdateOutcomeCounts(summary)).joinToString(" · ")
}

fun Context.libraryUpdateOutcomeCounts(summary: LibraryUpdateRunSummary): List<String> {
    return buildList {
        if (summary.newChapters > 0) {
            add(pluralStringResource(MR.plurals.library_updates_new_chapters, summary.newChapters, summary.newChapters))
        }
        if (summary.failed > 0) {
            add(pluralStringResource(MR.plurals.library_updates_failed, summary.failed, summary.failed))
        }
        if (summary.skipped > 0) {
            add(pluralStringResource(MR.plurals.library_updates_skipped, summary.skipped, summary.skipped))
        }
        if (summary.neverChecked > 0) {
            add(
                pluralStringResource(
                    MR.plurals.library_update_never_left_out,
                    summary.neverChecked,
                    summary.neverChecked,
                ),
            )
        }
    }
}
