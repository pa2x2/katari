package eu.kanade.presentation.entry.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import eu.kanade.presentation.components.DownloadIndicator
import eu.kanade.presentation.components.DownloadIndicatorAction
import eu.kanade.presentation.components.DownloadIndicatorMenuItem
import eu.kanade.presentation.components.DownloadIndicatorState
import eu.kanade.presentation.entry.translation.ChapterTranslateAction
import eu.kanade.presentation.entry.translation.ChapterTranslationBadge
import eu.kanade.presentation.entry.translation.chapterTranslateActions
import mihon.entry.interactions.download.EntryDownloadState
import mihon.entry.interactions.translate.EntryTranslateStatus
import tachiyomi.i18n.*
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
    translateStatusProvider: () -> EntryTranslateStatus? = { null },
    onTranslateClick: ((ChapterTranslateAction) -> Unit)? = null,
) {
    if (onClick == null) return

    val translateStatus = translateStatusProvider().takeIf { onTranslateClick != null }
    val translateMenuItems = if (onTranslateClick != null) {
        chapterTranslateActions(downloadStateProvider(), translateStatus).map { action ->
            DownloadIndicatorMenuItem(stringResource(action.label)) { onTranslateClick(action) }
        }
    } else {
        emptyList()
    }
    DownloadIndicator(
        enabled = enabled,
        modifier = modifier,
        badge = translateStatus?.let { status -> { ChapterTranslationBadge(status) } },
        menuItems = translateMenuItems,
        notDownloadedMenuItems = translateMenuItems,
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
        queuedContentDescription = stringResource(MR.strings.download_state_queued),
        downloadingContentDescription = stringResource(MR.strings.download_state_downloading),
        downloadedContentDescription = stringResource(MR.strings.label_downloaded),
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
