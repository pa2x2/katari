package mihon.entry.interactions.book.document.reader.navigation

import app.katari.book.api.BookLocator
import mihon.entry.interactions.book.document.reader.BookDocumentReaderState
import mihon.entry.interactions.book.document.reader.BookDocumentSection
import tachiyomi.domain.entry.model.EntryChapter

/**
 * Where the next ([forward]) or previous section from [section] starts: the one beside it in its chapter, or past the
 * chapter's first or last section, the chapter on that side. Null where there is none to go to.
 */
internal fun BookDocumentReaderState.sectionStep(
    section: BookDocumentSection<EntryChapter>,
    forward: Boolean,
): BookDocumentNavigationTarget? {
    val sections = loadedSections[section.owner.id]?.sections.orEmpty()
    val index = sections.indexOfFirst { it.key == section.key }
    sections.getOrNull(if (forward) index + 1 else index - 1)?.let {
        return BookDocumentNavigationTarget(it.owner, BookLocator(it.document.document.resourceId))
    }
    val chapterWindow = readingOrder.window(section.owner.id) ?: return null
    return (if (forward) chapterWindow.next else chapterWindow.previous)?.let { BookDocumentNavigationTarget(it) }
}
