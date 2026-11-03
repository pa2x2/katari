package eu.kanade.presentation.library.update

import androidx.compose.runtime.Composable
import eu.kanade.presentation.util.relativeTimeSpanString
import mihon.core.platform.PlatformContext
import mihon.core.platform.currentPlatformContext
import mihon.feature.library.update.report.LibraryUpdateRunSummary
import tachiyomi.i18n.*
import tachiyomi.i18n.platform.pluralStringResource
import tachiyomi.presentation.core.i18n.stringResource

/** "2 hours ago · 12 new chapters · 3 failed · 58 skipped", leaving out the counts that are zero. */
@Composable
fun libraryUpdateRunSummaryText(summary: LibraryUpdateRunSummary): String {
    val time = if (summary.isRunning) {
        stringResource(MR.strings.library_updates_running)
    } else {
        relativeTimeSpanString(summary.run.startedAt)
    }
    return (listOf(time) + currentPlatformContext().libraryUpdateOutcomeCounts(summary)).joinToString(" · ")
}

fun PlatformContext.libraryUpdateOutcomeCounts(summary: LibraryUpdateRunSummary): List<String> {
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
