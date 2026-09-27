package eu.kanade.presentation.reader

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import mihon.entry.interactions.reader.navigation.EntryReaderNavigationPresentation
import mihon.entry.interactions.reader.navigation.EntryReaderNavigationSheet

/** Reader chapters presented through the shared table of contents, keyed by chapter id. */
@Composable
internal fun ReaderChapterNavigationSheet(
    presentation: EntryReaderNavigationPresentation?,
    currentChapterId: Long?,
    onChapterClick: (Long) -> Unit,
    onDismissRequest: () -> Unit,
) {
    if (presentation == null) return
    val rows = remember(presentation) {
        presentation.chapters.map { chapter -> presentation.chapterRow(chapter, chapter.id) }
    }
    EntryReaderNavigationSheet(
        visible = true,
        rows = rows,
        selectedIndex = presentation.chapters.indexOfFirst { it.id == currentChapterId },
        onItemClick = onChapterClick,
        onDismissRequest = onDismissRequest,
    )
}
