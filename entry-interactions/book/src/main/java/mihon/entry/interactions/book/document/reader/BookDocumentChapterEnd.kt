package mihon.entry.interactions.book.document.reader

import mihon.book.api.document.BookDocumentBlockId
import mihon.entry.interactions.viewer.EntryChildWindow
import tachiyomi.domain.entry.model.EntryChapter

/**
 * Content-derived evidence that the current chapter is the end of the reading stream.
 *
 * Chapter completion must never depend on the chapter-transition row: the row may render at any
 * size, fail to compose, or be hidden by presentation changes, while persisted progress keeps
 * working. The evidence is therefore the tail of the chapter's final loaded section, and it
 * exists only while the reading window has no next chapter.
 */
internal data class BookDocumentChapterEnd(
    val chapter: EntryChapter,
    val finalSectionKey: String,
    val finalBlockId: BookDocumentBlockId,
)

internal fun bookDocumentChapterEnd(
    window: EntryChildWindow<EntryChapter>,
    loadedSections: Map<Long, BookDocumentPublicationSections<EntryChapter>>,
): BookDocumentChapterEnd? {
    if (window.next != null) return null
    val lastSection = loadedSections[window.current.id]?.sections?.lastOrNull() ?: return null
    return BookDocumentChapterEnd(
        chapter = window.current,
        finalSectionKey = lastSection.key,
        finalBlockId = lastSection.document.blocks.last().id,
    )
}
