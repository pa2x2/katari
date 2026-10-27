package eu.kanade.presentation.entry.translation

import androidx.compose.runtime.Composable
import mihon.entry.interactions.translate.EntryTranslateFailure
import mihon.entry.interactions.translate.EntryTranslateStatus
import tachiyomi.i18n.*
import tachiyomi.presentation.core.i18n.stringResource

/** How a chapter's translation status reads in a list, naming why it failed. */
@Composable
fun entryTranslateStatusText(status: EntryTranslateStatus): String = when (status) {
    EntryTranslateStatus.Translated -> stringResource(MR.strings.chapter_translation_translated)
    EntryTranslateStatus.WaitingForDownload -> stringResource(MR.strings.chapter_translation_waiting)
    EntryTranslateStatus.Queued -> stringResource(MR.strings.chapter_translation_queued)
    is EntryTranslateStatus.Translating -> stringResource(
        MR.strings.chapter_translation_translating,
        status.progress.done,
        status.progress.total,
    )
    is EntryTranslateStatus.Failed -> stringResource(
        MR.strings.chapter_translation_failed_reason,
        entryTranslateFailureText(status.failure),
    )
}

@Composable
private fun entryTranslateFailureText(failure: EntryTranslateFailure): String = when (failure) {
    EntryTranslateFailure.DownloadFailed -> stringResource(MR.strings.chapter_translation_failure_download_failed)
    EntryTranslateFailure.DownloadMissing -> stringResource(MR.strings.chapter_translation_failure_download_missing)
    EntryTranslateFailure.SetupRequired -> stringResource(MR.strings.chapter_translation_failure_setup_required)
    is EntryTranslateFailure.Error -> failure.message ?: stringResource(MR.strings.chapter_translation_failed)
}
