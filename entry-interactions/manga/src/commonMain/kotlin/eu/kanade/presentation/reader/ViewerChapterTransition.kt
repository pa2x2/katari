package eu.kanade.presentation.reader

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.remember
import eu.kanade.tachiyomi.ui.reader.model.ReaderChapter
import mihon.entry.interactions.manga.download.DownloadManager
import mihon.entry.interactions.reader.settings.ChapterTransitionMode
import mihon.entry.interactions.viewer.EntryChildTransition
import mihon.entry.viewer.chrome.ui.transition.ReaderEntryChildTransitionLoadState
import tachiyomi.i18n.*
import tachiyomi.presentation.core.i18n.stringResource
import tachiyomi.source.local.LocalSource

/**
 * A viewer's slot between two chapters: where reading goes next, whether either chapter is downloaded, and the
 * destination's loading or failure with a retry through [onRetry].
 */
@Composable
internal fun ViewerChapterTransition(
    transition: EntryChildTransition<ReaderChapter>,
    downloadManager: DownloadManager,
    displayMode: ChapterTransitionMode,
    placement: ChapterTransitionPlacement,
    onRetry: (ReaderChapter) -> Unit,
) {
    val goingToChapterDownloaded = remember(transition) {
        val toChapter = transition.to
        val toManga = toChapter?.manga
        when {
            toManga == null -> false
            toManga.source == LocalSource.ID -> true
            else -> downloadManager.isChapterDownloaded(
                chapterName = toChapter.chapter.name,
                chapterScanlator = toChapter.chapter.scanlator,
                chapterUrl = toChapter.chapter.url,
                mangaTitle = toManga.downloadFolderTitle,
                sourceId = toManga.source,
                skipCache = true,
            )
        }
    }
    val destinationState = transition.to?.stateFlow?.collectAsState()?.value
    val loadState = when (destinationState) {
        ReaderChapter.State.Loading -> ReaderEntryChildTransitionLoadState.Loading(
            stringResource(MR.strings.transition_pages_loading),
        )
        is ReaderChapter.State.Error -> ReaderEntryChildTransitionLoadState.Failed(
            stringResource(MR.strings.transition_pages_error, destinationState.error.message.orEmpty()),
        )
        ReaderChapter.State.Wait,
        is ReaderChapter.State.Loaded,
        null,
        -> ReaderEntryChildTransitionLoadState.Idle
    }
    val destination = transition.to
    val onRetryAction = if (destinationState is ReaderChapter.State.Error && destination != null) {
        { onRetry(destination) }
    } else {
        null
    }
    ChapterTransition(
        transition = transition,
        currChapterDownloaded = transition.from.pageLoader?.isLocal == true,
        goingToChapterDownloaded = goingToChapterDownloaded,
        loadState = loadState,
        onRetry = onRetryAction,
        displayMode = displayMode,
        placement = placement,
    )
}
