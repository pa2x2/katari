package eu.kanade.presentation.entry.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import dev.icerock.moko.resources.StringResource
import eu.kanade.presentation.entry.translation.ChapterTranslateAction
import mihon.entry.interactions.download.EntryDownloadState
import mihon.entry.interactions.translate.EntryTranslateStatus
import mihon.icons.materialsymbols.MaterialSymbols
import mihon.icons.materialsymbols.rounded.BookmarkAdd
import mihon.icons.materialsymbols.rounded.BookmarkRemove
import mihon.icons.materialsymbols.rounded.Check
import mihon.icons.materialsymbols.rounded.Delete
import mihon.icons.materialsymbols.rounded.Download
import mihon.icons.materialsymbols.rounded.FileDownloadOff
import mihon.icons.materialsymbols.rounded.RemoveDone
import mihon.presentation.kit.list.ChapterRow
import mihon.presentation.kit.list.SwipeAction
import mihon.presentation.kit.list.SwipeActionsRow
import mihon.presentation.kit.surface.pulsingHighlight
import tachiyomi.domain.library.service.LibraryPreferences
import tachiyomi.i18n.*
import tachiyomi.presentation.core.i18n.stringResource

@Composable
fun EntryChapterListItem(
    title: String,
    date: String?,
    readProgress: String?,
    scanlator: String?,
    kind: String?,
    summary: String?,
    preview: (@Composable (Modifier) -> Unit)?,
    onPreviewClick: (() -> Unit)?,
    read: Boolean,
    duration: String?,
    bookmark: Boolean,
    selected: Boolean,
    downloadIndicatorEnabled: Boolean,
    downloadStateProvider: () -> EntryDownloadState,
    downloadProgressProvider: () -> Int,
    modifier: Modifier = Modifier,
    unconsumedIndicatorLabel: StringResource = MR.strings.action_filter_unseen,
    chapterSwipeStartAction: LibraryPreferences.ChapterSwipeAction,
    chapterSwipeEndAction: LibraryPreferences.ChapterSwipeAction,
    onLongClick: () -> Unit,
    onClick: () -> Unit,
    onDownloadClick: ((ChapterDownloadAction) -> Unit)?,
    onChapterSwipe: (LibraryPreferences.ChapterSwipeAction) -> Unit,
    highlightTrigger: Int? = null,
    translateStatusProvider: () -> EntryTranslateStatus? = { null },
    onTranslateClick: ((ChapterTranslateAction) -> Unit)? = null,
) {
    val start = getSwipeAction(
        action = chapterSwipeStartAction,
        read = read,
        bookmark = bookmark,
        downloadState = downloadStateProvider(),
        onSwipe = { onChapterSwipe(chapterSwipeStartAction) },
    )
    val end = getSwipeAction(
        action = chapterSwipeEndAction,
        read = read,
        bookmark = bookmark,
        downloadState = downloadStateProvider(),
        onSwipe = { onChapterSwipe(chapterSwipeEndAction) },
    )

    SwipeActionsRow(start = start, end = end) {
        ChapterRow(
            title = title,
            read = read,
            bookmark = bookmark,
            onClick = onClick,
            modifier = modifier.pulsingHighlight(
                trigger = highlightTrigger,
                visible = !selected,
                iterations = 2,
                highlightDurationMillis = 1_400L,
            ),
            date = date,
            duration = duration,
            progress = readProgress,
            scanlator = scanlator,
            kind = kind,
            summary = summary,
            preview = preview,
            onPreviewClick = onPreviewClick,
            unreadLabel = stringResource(unconsumedIndicatorLabel),
            selected = selected,
            onLongClick = onLongClick,
            trailing = {
                EntryChapterDownloadIndicator(
                    enabled = downloadIndicatorEnabled,
                    downloadStateProvider = downloadStateProvider,
                    downloadProgressProvider = downloadProgressProvider,
                    onClick = onDownloadClick,
                    translateStatusProvider = translateStatusProvider,
                    onTranslateClick = onTranslateClick,
                )
            },
        )
    }
}

private fun getSwipeAction(
    action: LibraryPreferences.ChapterSwipeAction,
    read: Boolean,
    bookmark: Boolean,
    downloadState: EntryDownloadState,
    onSwipe: () -> Unit,
): SwipeAction? {
    return when (action) {
        LibraryPreferences.ChapterSwipeAction.ToggleRead -> SwipeAction(
            icon = if (!read) MaterialSymbols.Rounded.Check else MaterialSymbols.Rounded.RemoveDone,
            isUndo = read,
            onSwipe = onSwipe,
        )
        LibraryPreferences.ChapterSwipeAction.ToggleBookmark -> SwipeAction(
            icon = if (!bookmark) MaterialSymbols.Rounded.BookmarkAdd else MaterialSymbols.Rounded.BookmarkRemove,
            isUndo = bookmark,
            onSwipe = onSwipe,
        )
        LibraryPreferences.ChapterSwipeAction.Download -> SwipeAction(
            icon = when (downloadState) {
                EntryDownloadState.NOT_DOWNLOADED, EntryDownloadState.ERROR -> MaterialSymbols.Rounded.Download
                EntryDownloadState.QUEUE, EntryDownloadState.DOWNLOADING -> MaterialSymbols.Rounded.FileDownloadOff
                EntryDownloadState.DOWNLOADED -> MaterialSymbols.Rounded.Delete
            },
            onSwipe = onSwipe,
        )
        LibraryPreferences.ChapterSwipeAction.Disabled -> null
    }
}
