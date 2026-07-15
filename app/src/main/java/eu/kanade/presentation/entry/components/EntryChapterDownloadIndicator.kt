package eu.kanade.presentation.entry.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import eu.kanade.presentation.components.DownloadIndicator
import eu.kanade.presentation.components.DownloadIndicatorAction
import eu.kanade.presentation.components.DownloadIndicatorState
import mihon.entry.interactions.EntryDownloadState
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.i18n.stringResource

enum class ChapterDownloadAction {
    START,
    START_NOW,
    CANCEL,
    DELETE,
}

@Composable
fun EntryChapterDownloadIndicator(
    enabled: Boolean,
    downloadStateProvider: () -> EntryDownloadState,
    downloadProgressProvider: () -> Int,
    onClick: ((ChapterDownloadAction) -> Unit)?,
    modifier: Modifier = Modifier,
) {
    if (onClick == null) return

    DownloadIndicator(
        enabled = enabled,
        modifier = modifier,
        downloadStateProvider = {
            when (downloadStateProvider()) {
                EntryDownloadState.NOT_DOWNLOADED -> DownloadIndicatorState.NOT_DOWNLOADED
                EntryDownloadState.QUEUE -> DownloadIndicatorState.QUEUE
                EntryDownloadState.DOWNLOADING -> DownloadIndicatorState.DOWNLOADING
                EntryDownloadState.DOWNLOADED -> DownloadIndicatorState.DOWNLOADED
                EntryDownloadState.ERROR -> DownloadIndicatorState.ERROR
            }
        },
        downloadProgressProvider = downloadProgressProvider,
        startContentDescription = stringResource(MR.strings.manga_download),
        errorContentDescription = stringResource(MR.strings.chapter_error),
        startNowText = stringResource(MR.strings.action_start_downloading_now),
        cancelText = stringResource(MR.strings.action_cancel),
        deleteText = stringResource(MR.strings.action_delete),
        onClick = {
            onClick(
                when (it) {
                    DownloadIndicatorAction.START -> ChapterDownloadAction.START
                    DownloadIndicatorAction.START_NOW -> ChapterDownloadAction.START_NOW
                    DownloadIndicatorAction.CANCEL -> ChapterDownloadAction.CANCEL
                    DownloadIndicatorAction.DELETE -> ChapterDownloadAction.DELETE
                },
            )
        },
    )
}
