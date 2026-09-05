package mihon.entry.interactions.book.document.reader.navigation

import mihon.book.api.BookLocator
import mihon.book.api.BookNavigationItem
import mihon.book.api.document.resolvePosition
import mihon.entry.interactions.book.document.reader.BookDocumentReaderState
import mihon.entry.interactions.book.reader.BookReaderNavigationRow
import tachiyomi.domain.entry.model.EntryChapter

internal data class BookDocumentNavigationTarget(val chapter: EntryChapter, val locator: BookLocator? = null)

internal data class BookDocumentNavigationPresentation(
    val rows: List<BookReaderNavigationRow<BookDocumentNavigationTarget>>,
    val selectedIndex: Int,
)

/** Source chapters remain reachable regardless of the shape or size of their internal contents. */
internal fun BookDocumentReaderState.documentNavigationPresentation(): BookDocumentNavigationPresentation {
    val rows = navigationPresentation.chapters.flatMap { chapter ->
        listOf(
            BookReaderNavigationRow(
                item = BookDocumentNavigationTarget(chapter),
                title = chapter.name,
                read = chapter.read,
                bookmark = chapter.bookmark,
                progressLabel = navigationPresentation.progressLabels[chapter.id],
            ),
        ) + publicationNavigation[chapter.id].orEmpty().navigationRows(chapter, 1)
    }
    val sections = loadedSections[currentChapterId]?.sections.orEmpty()
    fun progression(locator: BookLocator, saved: Boolean = false): Float? {
        val section = sections.firstOrNull { it.document.document.resourceId == locator.resourceId } ?: return null
        val document = section.document.document
        val position =
            (if (saved) document.resolvePosition(locator) else document.navigationPosition(locator)) ?: return null
        return section.totalProgression(document.progressionAt(position))
    }
    val currentProgress = navigationLocator?.let { progression(it, saved = true) }
    val selected = if (currentProgress == null) {
        null
    } else {
        rows.withIndex()
            .filter { it.value.item.chapter.id == currentChapterId }
            .mapNotNull { row ->
                row.value.item.locator?.let { progression(it) }?.takeIf { it <= currentProgress }
                    ?.let { row.index to it }
            }
            .maxWithOrNull(compareBy<Pair<Int, Float>> { it.second }.thenBy { it.first })?.first
    }
    return BookDocumentNavigationPresentation(
        rows,
        selected ?: rows.indexOfFirst { it.item.chapter.id == currentChapterId },
    )
}

private fun List<BookNavigationItem>.navigationRows(
    chapter: EntryChapter,
    depth: Int,
): List<BookReaderNavigationRow<BookDocumentNavigationTarget>> = flatMap { item ->
    listOf(
        BookReaderNavigationRow(
            item = BookDocumentNavigationTarget(chapter, item.target),
            title = item.title.orEmpty(),
            depth = depth,
        ),
    ) + item.children.navigationRows(chapter, depth + 1)
}
