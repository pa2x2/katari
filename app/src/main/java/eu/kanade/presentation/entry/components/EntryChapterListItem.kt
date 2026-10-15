package eu.kanade.presentation.entry.components

import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import dev.icerock.moko.resources.StringResource
import eu.kanade.presentation.entry.translation.ChapterTranslateAction
import me.saket.swipe.SwipeableActionsBox
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
import mihon.presentation.kit.content.EntryMetrics
import mihon.presentation.kit.content.Icon
import mihon.presentation.kit.list.ChapterRow
import mihon.presentation.kit.surface.pulsingHighlight
import mihon.presentation.kit.theme.KatariTheme
import mihon.presentation.kit.theme.LocalContentColor
import tachiyomi.domain.library.service.LibraryPreferences
import tachiyomi.i18n.*
import tachiyomi.presentation.core.i18n.stringResource

@Composable
fun EntryChapterListItem(
    title: String,
    date: String?,
    readProgress: String?,
    scanlator: String?,
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
        background = KatariTheme.colors.primaryContainer,
        onSwipe = { onChapterSwipe(chapterSwipeStartAction) },
    )
    val end = getSwipeAction(
        action = chapterSwipeEndAction,
        read = read,
        bookmark = bookmark,
        downloadState = downloadStateProvider(),
        background = KatariTheme.colors.primaryContainer,
        onSwipe = { onChapterSwipe(chapterSwipeEndAction) },
    )

    SwipeableActionsBox(
        modifier = Modifier.clipToBounds(),
        startActions = listOfNotNull(start),
        endActions = listOfNotNull(end),
        swipeThreshold = EntryMetrics.chapterSwipeThreshold,
        backgroundUntilSwipeThreshold = KatariTheme.colors.surfaceContainerLowest,
    ) {
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
    background: Color,
    onSwipe: () -> Unit,
): me.saket.swipe.SwipeAction? {
    return when (action) {
        LibraryPreferences.ChapterSwipeAction.ToggleRead -> swipeAction(
            icon = if (!read) MaterialSymbols.Rounded.Check else MaterialSymbols.Rounded.RemoveDone,
            background = background,
            isUndo = read,
            onSwipe = onSwipe,
        )
        LibraryPreferences.ChapterSwipeAction.ToggleBookmark -> swipeAction(
            icon = if (!bookmark) MaterialSymbols.Rounded.BookmarkAdd else MaterialSymbols.Rounded.BookmarkRemove,
            background = background,
            isUndo = bookmark,
            onSwipe = onSwipe,
        )
        LibraryPreferences.ChapterSwipeAction.Download -> swipeAction(
            icon = when (downloadState) {
                EntryDownloadState.NOT_DOWNLOADED, EntryDownloadState.ERROR -> MaterialSymbols.Rounded.Download
                EntryDownloadState.QUEUE, EntryDownloadState.DOWNLOADING -> MaterialSymbols.Rounded.FileDownloadOff
                EntryDownloadState.DOWNLOADED -> MaterialSymbols.Rounded.Delete
            },
            background = background,
            onSwipe = onSwipe,
        )
        LibraryPreferences.ChapterSwipeAction.Disabled -> null
    }
}

private fun swipeAction(
    onSwipe: () -> Unit,
    icon: ImageVector,
    background: Color,
    isUndo: Boolean = false,
): me.saket.swipe.SwipeAction {
    return me.saket.swipe.SwipeAction(
        icon = {
            CompositionLocalProvider(LocalContentColor provides KatariTheme.colors.onPrimaryContainer) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    modifier = Modifier.padding(KatariTheme.spacing.l),
                )
            }
        },
        background = background,
        onSwipe = onSwipe,
        isUndo = isUndo,
    )
}
