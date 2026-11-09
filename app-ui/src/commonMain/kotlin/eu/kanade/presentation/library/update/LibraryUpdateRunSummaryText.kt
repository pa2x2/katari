package eu.kanade.presentation.library.update

import androidx.compose.runtime.Composable
import app.katari.core.platform.PlatformContext
import app.katari.core.platform.currentPlatformContext
import app.katari.sync.client.libraryupdate.OtherDeviceLibraryUpdate
import eu.kanade.presentation.util.relativeTimeSpanString
import tachiyomi.domain.library.update.model.LibraryUpdateRunSummary
import tachiyomi.i18n.*
import tachiyomi.i18n.platform.pluralStringResource
import tachiyomi.presentation.core.i18n.stringResource

/** "2 hours ago · 12 new chapters · 3 failed · 58 skipped", leaving out the counts that are zero. */
@Composable
fun libraryUpdateRunSummaryText(summary: LibraryUpdateRunSummary): String {
    return summaryText(summary, runTime(summary))
}

/** "2 hours ago on Pixel 6a · 12 new chapters", for an update another device ran. */
@Composable
fun libraryUpdateRunSummaryText(update: OtherDeviceLibraryUpdate): String {
    return summaryText(update.summary, libraryUpdateTimeOn(runTime(update.summary), update.deviceName))
}

/** [time] followed by the device an update ran on; [deviceName] is null for a device the account no longer lists. */
@Composable
fun libraryUpdateTimeOn(time: String, deviceName: String?): String {
    return if (deviceName != null) {
        stringResource(MR.strings.library_update_on_device, time, deviceName)
    } else {
        stringResource(MR.strings.library_update_on_other_device, time)
    }
}

@Composable
private fun runTime(summary: LibraryUpdateRunSummary): String {
    return if (summary.isRunning) {
        stringResource(MR.strings.library_updates_running)
    } else {
        relativeTimeSpanString(summary.run.startedAt)
    }
}

@Composable
private fun summaryText(summary: LibraryUpdateRunSummary, time: String): String {
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
